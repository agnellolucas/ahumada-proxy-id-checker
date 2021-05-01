package com.ahumada.fuse.processor;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.PropertyInject;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.ahumada.fuse.db.ConnectionManager;
import com.ahumada.fuse.db.DbHelper;
import com.ahumada.fuse.db.model.Fv29ClienteDatos;
import com.ahumada.fuse.enums.MessageListEnum;
import com.ahumada.fuse.enums.RestResponseStatus;
import com.ahumada.fuse.external.services.IdCheckerServiceProvider;
import com.ahumada.fuse.resources.model.RestRequest;
import com.ahumada.fuse.resources.model.RestResponse;
import com.ahumada.fuse.utils.FunctionUtils;

public class ProcessRequest implements Processor {

	private final int RUT_LENGTH = 10;
	//	private final int RUT_MIN_LENGTH_WITHOUT_DV = 7;
	//	private final int SERIE_LENGTH = 10;

	@PropertyInject(value = "daysOfRecordValidity")
	private String daysOfRecordValidity;

	private Logger logger = Logger.getLogger(getClass());

	@BeanInject
	ConnectionManager connManager;

	@BeanInject
	IdCheckerServiceProvider serviceProvider;

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

				boolean callExternalService = true;
				boolean isDataUpdate = false;

				// Check if RUT information exists in the database
				Fv29ClienteDatos clienteDatos = getClienteDatos(request.getRut());
				if(clienteDatos != null) {
					// Make sure it is a data update because it already exists on the database
					isDataUpdate = true;

					// Check difference between the fechaConsultaDatos and FechaConsultaDatosValidez in days 
					if(clienteDatos.getFechaConsultaDatos() != null 
							&& clienteDatos.getFechaConsultaDatosValidez() != null
							&& clienteDatos.getCurrentTimestampDatabase() != null) {

						/*
						 * It will not call the external service again IF:
						 * 	- a record already exists in the database AND
						 *  - the current date is before the database record validity 
						 *  - the "existe" parameter is equal "S" which means the RUT exists
						 */
						if(clienteDatos.getCurrentTimestampDatabase().before(clienteDatos.getFechaConsultaDatosValidez())
								&& !FunctionUtils.stringIsNullOrEmpty(clienteDatos.getExiste()) 
								&& (clienteDatos.getExiste() != null && clienteDatos.getExiste().trim().equalsIgnoreCase("S"))) {

							callExternalService = false;
							response = new RestResponse(true);
							response.setMessage(String.format("RUT %s encontrado en la base de datos ", clienteDatos.getDocNumero()));
						}
					}
				}

				if(callExternalService) response = callIdCheckerProvider(request, isDataUpdate);

				// Just in case a response is returned null or false and without a message, we consider as an unexpected error
				if(response == null || (!response.isSuccess() && FunctionUtils.stringIsNullOrEmpty(response.getMessage()))) 
					throw new Exception(String.format(MessageListEnum.GENERIC_EXCEPTION.getDesc(), "Error inesperado, comuníquese con el equipo de soporte de la aplicación"));

				// Populate validity date
				if(response != null && response.isSuccess()) {
					Date validity = DbHelper.getValidityFv29ClienteDatos(request.getRut(), Integer.parseInt(daysOfRecordValidity), connManager.getConnection());
					if(validity != null) {
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						response.setVencimiento(formatter.format(validity));
					} else {
						response.setVencimiento("1999-12-30");
					}
				}

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
			//String rutDv = request.getRut().substring(request.getRut().length()-1);

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
	 * Check if client data exists on database
	 */
	private Fv29ClienteDatos getClienteDatos(String rut) {
		Fv29ClienteDatos clienteDatos = null;

		try {
			clienteDatos = DbHelper.getBasicFv29ClienteDatos(rut, Integer.parseInt(daysOfRecordValidity), connManager.getConnection());
		} catch (SQLException e) {
			logger.error(String.format("Error al consultar los datos del cliente en la base de datos :: RUT %s", rut));
		}

		return clienteDatos;
	}

	/*
	 * Call ID Checker Provider 
	 */
	private RestResponse callIdCheckerProvider(RestRequest req, boolean isDataUpdate) {

		RestResponse response;

		try {
			// Just in case it can't inject the bean
			if(serviceProvider == null) throw new Exception(" No no pudo iniciar IdCheckerServiceProvider ");
			// Call ID checker
			response = serviceProvider.isValidServiceProvider() ? serviceProvider.callIdCheckerProvider(req, isDataUpdate, connManager) : null;

		} catch (Exception e) {
			response = new RestResponse(false);
			response.setMessage(String.format(
					"No se pudo llamar al servicio de validación de identificación externa :: Exception %s", 
					e.getMessage()));
		}
		return response;

	}

	public String getDaysOfRecordValidity() {
		return daysOfRecordValidity;
	}

	public void setDaysOfRecordValidity(String daysOfRecordValidity) {
		this.daysOfRecordValidity = daysOfRecordValidity;
	}

}