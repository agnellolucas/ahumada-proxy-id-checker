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
import com.ahumada.fuse.enums.DocEstadoEnum;
import com.ahumada.fuse.enums.MessageListEnum;
import com.ahumada.fuse.exceptions.BusinessHandledException;
import com.ahumada.fuse.external.services.IdCheckerServiceProvider;
import com.ahumada.fuse.resources.model.RestRequest;
import com.ahumada.fuse.resources.model.RestResponse;
import com.ahumada.fuse.utils.FunctionUtils;

public class ProcessRequest implements Processor {

	private final int RUT_LENGTH = 10;
	static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

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

			// Retrieve database data or call external service
			boolean callExternalService = true;

			Fv29ClienteDatos clienteDatos = getClienteDatos(request.getRut(), request.getSerie());
			if(clienteDatos != null && !FunctionUtils.stringIsNullOrEmpty(clienteDatos.getDocEstado()) && clienteDatos.getFechaConsultaDatosValidez() != null) {
				// If a document has a state BLOQUEADO it will never be updated again, it will always be invalid
				if(clienteDatos.getDocEstado().trim().equalsIgnoreCase(DocEstadoEnum.BLOQUEADO.getEstado())) {
					response = setRestResponse(clienteDatos, null);
					callExternalService = false;

				// Evaluate existing data to see if record is still valid
				} else if(clienteDatos.getCurrentTimestampDatabase() != null && clienteDatos.getCurrentTimestampDatabase().before(clienteDatos.getFechaConsultaDatosValidez()) 
						&& clienteDatos.getDocEstado().trim().equalsIgnoreCase(DocEstadoEnum.VIGENTE.getEstado())) {
					response = setRestResponse(clienteDatos, null);
					callExternalService = false;
				} 
			}

			// Call external service again if record wasn't found in the database or is no longer valid
			if(callExternalService) response = callIdCheckerProvider(request, clienteDatos);

		} catch (BusinessHandledException e) {
			// Log the business error and the original exception
			logger.error(new StringBuilder()
					.append(e.getMessage())
					.append(" :: ")
					.append(e.getCause() != null ? e.getCause().getMessage() : "Internal validation exception")
					.toString());
			// Build a response with handled error message
			response = setRestResponse(null, e.getMessage());

		} catch (Exception e) {
			logger.error(e.getMessage());
			// Set unexpected error response
			response = setRestResponse(null, null);

		} finally {
			// Close DB Connection
			try {
				if(connManager != null) connManager.closeConnection();
			} catch (SQLException e) {
				logger.error(String.format(MessageListEnum.SQLERROR_CLOSE_CONNECTION.getDesc(), e.getMessage()));
			}
		}

		exchange.getOut().setFault(!response.isSuccess());
		exchange.getOut().setBody(response.toString());
	}

	/*
	 * Validate and parse message to POJO
	 */
	public RestRequest validateRequest(Exchange exchange) throws BusinessHandledException, Exception {


		RestRequest request = null;
		try {
			request = exchange.getIn().getBody(RestRequest.class);
			if(request == null) throw new BusinessHandledException("No se pudo recuperar el cuerpo del mensaje");
		} catch (Exception e) {
			throw new BusinessHandledException(String.format("Error al analizar JSON :: %s", e.getMessage()));
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
		if(errorMessage != null) throw new BusinessHandledException(errorMessage.toString());

		return request;
	}

	
	/*
	 * Call ID Checker Provider 
	 */
	private RestResponse callIdCheckerProvider(RestRequest req, Fv29ClienteDatos oldClienteDatos) throws BusinessHandledException, Exception {

		RestResponse response = null;

		// Just in case it can't inject the bean
		if(serviceProvider == null) throw new BusinessHandledException("No se pudo crear una instancia del cliente para llamar al servicio de validación de datos externos");
		// Call ID checker
		if(serviceProvider.isValidServiceProvider()) {
			Fv29ClienteDatos newClienteDatos = serviceProvider.callIdCheckerProvider(req, connManager);
			// Save (update or insert) customer data
			saveClienteDatos(newClienteDatos, oldClienteDatos);
			// Obtain the expiration date
			newClienteDatos.setFechaConsultaDatosValidez(getValidityClienteDatos(newClienteDatos));
			// Set response
			response = setRestResponse(newClienteDatos, null);
		}

		return response;

	}
	
	public RestResponse setRestResponse(Fv29ClienteDatos clienteDatos, String errorMessage) {

		RestResponse response = null;

		if(FunctionUtils.stringIsNullOrEmpty(errorMessage) && clienteDatos != null) {
			boolean existsDocEstado = !FunctionUtils.stringIsNullOrEmpty(clienteDatos.getDocEstado());

			if(existsDocEstado && clienteDatos.getDocEstado().trim().equalsIgnoreCase(DocEstadoEnum.VIGENTE.getEstado())) {
				response = new RestResponse(true);
				response.setMessage(
						String.format("RUT %s and SERIE %s encontrado en la base de datos :: Estado %s", 
								clienteDatos.getDocNumero(),
								clienteDatos.getDocSerie(),
								clienteDatos.getDocEstado()));

				response.setVencimiento(clienteDatos.getFechaConsultaDatosValidez() != null ? FORMATTER.format(clienteDatos.getFechaConsultaDatosValidez()) : "1999-12-30");

			} else {
				response = new RestResponse(false);
				response.setMessage(
						String.format("Numero de Serie %s no corresponde con el RUT %s :: Estado %s", 
								clienteDatos.getDocSerie(),
								clienteDatos.getDocNumero(),
								(existsDocEstado ? clienteDatos.getDocEstado() : "no encontrado")));
			}
		} else if(!FunctionUtils.stringIsNullOrEmpty(errorMessage)) {
			response = new RestResponse(false);
			response.setMessage(errorMessage);
		} else {
			response = new RestResponse(false);
			response.setMessage(String.format(MessageListEnum.GENERIC_EXCEPTION.getDesc(), "Error inesperado, comuníquese con el equipo de soporte de la aplicación"));
		}

		return response;
	}



	public String getDaysOfRecordValidity() {
		return daysOfRecordValidity;
	}

	public void setDaysOfRecordValidity(String daysOfRecordValidity) {
		this.daysOfRecordValidity = daysOfRecordValidity;
	}

	
	//TODO BEGIN move methods to a new class that provides interface between service and database resources
	private Fv29ClienteDatos getClienteDatos(String rut, String serie) throws BusinessHandledException, Exception {
		Fv29ClienteDatos clienteDatos = null;

		try {
			clienteDatos = DbHelper.getBasicFv29ClienteDatos(rut, serie, Integer.parseInt(daysOfRecordValidity), connManager.getConnection());
		} catch (SQLException e) {
			throw new BusinessHandledException(
					String.format("Error al consultar los datos del cliente en la base de datos :: RUT %s :: SERIE %s ", rut, serie)
					, e);
		}

		return clienteDatos;
	}
	private boolean saveClienteDatos(Fv29ClienteDatos newClienteDatos, Fv29ClienteDatos oldClienteDatos) throws BusinessHandledException, Exception {
		// Save response to database
		if(newClienteDatos != null) {
			try {

				// Evaluate if previous record exists and if it is valid 
				boolean existsPreviousRecord = oldClienteDatos != null ;
				boolean isPreviousValid = existsPreviousRecord && !FunctionUtils.stringIsNullOrEmpty(oldClienteDatos.getDocEstado()) && oldClienteDatos.getDocEstado().trim().equalsIgnoreCase(DocEstadoEnum.VIGENTE.getEstado());

				// Delete any valid record with the same RUT and ESTADO = V before inserting a new one
				boolean isCurrentValid = !FunctionUtils.stringIsNullOrEmpty(newClienteDatos.getDocEstado()) && newClienteDatos.getDocEstado().trim().equalsIgnoreCase(DocEstadoEnum.VIGENTE.getEstado());
				if(isCurrentValid) {
					DbHelper.deleteFvClienteDatos(newClienteDatos, connManager.getConnection());
				}

				/*
				 * If previous record is valid and current is valid, then the previous record was deleted 
				 * In all other cases if previous existed then it's a data update
				 */
				boolean isDataUpdate = (isPreviousValid && isCurrentValid)
							? false 
							: existsPreviousRecord;

				// call method to update or insert data
				boolean insertedUpdate = DbHelper.upsertFv29ClienteDatos(newClienteDatos, isDataUpdate, connManager.getConnection());

				if(!insertedUpdate) throw new BusinessHandledException("Actualización o inserción de datos en la base de datos no confirmada");

				return true;
			} catch (SQLException e) {
				throw new BusinessHandledException(
						String.format("Error al guardar los datos del cliente devueltos por Equifax en la base de datos :: RUT %s :: SERIE %s ", newClienteDatos.getDocNumero(), newClienteDatos.getDocSerie())
						, e);
			} 
		}
		return false;
	}
	private Date getValidityClienteDatos(Fv29ClienteDatos clienteDatos) throws BusinessHandledException, Exception {
		Date validityDate = null;
		try {
			validityDate = DbHelper.getValidityFv29ClienteDatos(clienteDatos.getDocNumero(), clienteDatos.getDocSerie(), Integer.parseInt(daysOfRecordValidity), connManager.getConnection());
			if(validityDate == null) throw new BusinessHandledException("La consulta no devolvió ningún registro");
		} catch (SQLException e) {
			throw new BusinessHandledException(
					String.format("Error al consultar la fecha de caducidad del registro en la base de datos :: RUT %s :: SERIE %s ", clienteDatos.getDocNumero(), clienteDatos.getDocSerie())
					, e);
		}
		return validityDate;
	}

}