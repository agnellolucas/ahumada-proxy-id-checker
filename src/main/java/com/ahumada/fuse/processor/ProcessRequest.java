package com.ahumada.fuse.processor;

import java.sql.SQLException;

import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.ahumada.fuse.db.ConnectionManager;
import com.ahumada.fuse.enums.MessageListEnum;
import com.ahumada.fuse.enums.RestResponseStatus;
import com.ahumada.fuse.external.services.IdCheckerServiceProvider;
import com.ahumada.fuse.resources.model.RestRequest;
import com.ahumada.fuse.resources.model.RestResponse;
import com.ahumada.fuse.utils.FunctionUtils;

public class ProcessRequest implements Processor {

	final int RUT_LENGTH = 10;
	final int RUT_MIN_LENGTH_WITHOUT_DV = 7;
	final int SERIE_LENGTH = 10;
	
	private Logger logger = Logger.getLogger(getClass());

	@BeanInject
	ConnectionManager connManager;

	public void process(Exchange exchange) throws Exception {

		RestRequest request = null;
		RestResponse response = null;

		try {
			
			// Validate Data Source
			if(connManager == null || !connManager.isConnValid()) {
				logger.error("Data source property is NULL at ConnectionManager class");
				throw new Exception(MessageListEnum.SQLERROR_CONNECTION_PARAM.getDesc());
			}

			// Parse and validate request
			request = validateRequest(exchange);

			try {
				response = callIdCheckerProvider(request, connManager);

				// Just in case a response is returned null or false and without a message, we consider as an unexpected error
				if(response == null || (!response.isSuccess() && FunctionUtils.stringIsNullOrEmpty(response.getMessage()))) 
					throw new Exception(String.format(MessageListEnum.GENERIC_EXCEPTION.getDesc(), "Unexpected error, please contact the support team"));
				
			} catch (Exception e) {
				// Just log error and re-throw exception
				logger.error(e.getMessage());
				throw new Exception(e.getMessage());

			} finally {
				// Close DB Connection
				try {
					if(connManager != null) connManager.closeConnection();
				} catch (SQLException e) {
					logger.error(String.format(MessageListEnum.SQLERROR_CLOSE_CONNECTION.getDesc(), e.getMessage()));
				}
			}

		} catch (Exception e) {
			response = new RestResponse(e.getMessage(),RestResponseStatus.UNEXPECTED_ERROR.isSuccess());
		}
		
		exchange.getOut().setFault(!response.isSuccess());
		exchange.getOut().setBody(response.toString());
	}

	/*
	 * Validate and parse message to POJO
	 */
	public RestRequest validateRequest(Exchange exchange) throws Exception {


		RestRequest request = null;
		try {
			request = exchange.getIn().getBody(RestRequest.class);
			if(request == null) throw new Exception("No se pudo recuperar el cuerpo del mensaje");
		} catch (Exception e) {
			throw new Exception(String.format("Error al analizar JSON :: %s", e.getMessage()));
		}

		// Basic data validation
		StringBuilder errorMessage = null;
		
		// RUT
		if(FunctionUtils.stringIsNullOrEmpty(request.getRut())) {
			errorMessage = errorMessage == null ? new StringBuilder() : errorMessage;
			errorMessage.append("Falta el parámetro RUT en el cuerpo del mensaje");
		} else if(request.getRut().trim().length() > RUT_LENGTH) {
			errorMessage = errorMessage == null ? new StringBuilder() : errorMessage;
			errorMessage.append(String.format("RUT debe contener menos de %d dígitos ", RUT_LENGTH));
		} else {
			
			//Remove special characters 
			request.setRut(request.getRut().trim().replaceAll("[^a-zA-Z0-9]", ""));
			
			String rutCore = request.getRut().substring(0, request.getRut().length()-1); 
//			String rutDv = request.getRut().substring(request.getRut().length()-1);
			
			//Check if rut core is numeric
			if(!StringUtils.isNumeric(rutCore)) {
				errorMessage = errorMessage == null ? new StringBuilder() : errorMessage;
				errorMessage.append(String.format("Los primeros dígitos de RUT deben ser numéricos. Ej: 12345678-K :: RUT recebido %s", rutCore));
			}
			
			//TODO validate RUT and DV
			// http://lineadecodigo.com/java/validador-de-rut-en-java/
			// https://uncodigo.com/herramienta-validador-de-rut-chileno-online/
		}

		// SERIE	
		if(FunctionUtils.stringIsNullOrEmpty(request.getSerie())) {
			errorMessage = errorMessage == null ? new StringBuilder() : errorMessage;
			errorMessage.append("Falta el parámetro SERIE en el cuerpo del mensaje");
			//TODO depending on the response from Ahumada, we may need to check Series size and set default value if it is null 
		} else {
			//Remove special characters 
			request.setSerie(request.getSerie().trim().replaceAll("[^a-zA-Z0-9]", " "));
		}
		
		// throw exception in case any issue has been found during the validation
		if(errorMessage != null) throw new Exception(errorMessage.toString());

		return request;
	}


	/*
	 * Call ID Checker Provider 
	 */
	private RestResponse callIdCheckerProvider(RestRequest req, ConnectionManager connManager) {

		RestResponse response;
		
		try {
			IdCheckerServiceProvider serviceProvider = new IdCheckerServiceProvider();
			
			// Check if the needed properties to call the ID checker were properly loaded
			if(FunctionUtils.stringIsNullOrEmpty(serviceProvider.getUsuarioServiceProvider())){
				String err = "El usuario del proveedor de verificación de RUT no se encuentra en el archivo de configuración de la aplicación.";
				logger.error(err);
				throw new Exception(err);
			} else if(FunctionUtils.stringIsNullOrEmpty(serviceProvider.getClaveServiceProvider())) {
				String err = "La contraseña del proveedor de verificación de RUT no se encuentra en el archivo de configuración de la aplicación.";
				logger.error(err);
				throw new Exception(err);
			} else if(FunctionUtils.stringIsNullOrEmpty(serviceProvider.getWsdlURLServiceProvider())) {
				String err = "No se encontró la URL del proveedor de verificación de RUT en el archivo de configuración de la aplicación.";
				logger.error(err);
				throw new Exception(err);
			}
			
			// Call ID checker
			response = serviceProvider.callIdCheckerProvider(req, connManager);
			
		} catch (Exception e) {
			response = new RestResponse(false);
			response.setMessage(String.format(
					"No se pudo llamar al servicio de validación de identificación externa :: Exception %s", 
					e.getMessage()));
		}
		return response;
		
//		String RUT_ERROR1 = "123456780";
//		String RUT_ERROR2 = "000000000";
//
//		try {
//			//TODO implement call to ID checker provider
//			//restResponse = equifaxProvider.GeteIDCompareValidator(req, connManager);
//			
//			// MOCK response
//			if(req.getRut().equals(RUT_ERROR1) || req.getRut().equals(RUT_ERROR2)) {
//				restResponse = new RestResponse(false);
//				restResponse.setMessage("RUT inválida");
//			} else {
//				restResponse = new RestResponse(true);
//				restResponse.setMessage("RUT válido");
//			}
//			
//		} catch (Exception e) {
//			restResponse = new RestResponse(false);
//			restResponse.setMessage(e.getMessage());
//			logger.error(e.getMessage());
//		}

	}
	
	

}