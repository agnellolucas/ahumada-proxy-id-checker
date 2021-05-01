package com.ahumada.fuse.external.services;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.camel.PropertyInject;
import org.apache.log4j.Logger;

import com.ahumada.fuse.db.ConnectionManager;
import com.ahumada.fuse.db.DbHelper;
import com.ahumada.fuse.db.model.Fv29ClienteDatos;
import com.ahumada.fuse.resources.model.RestRequest;
import com.ahumada.fuse.resources.model.RestResponse;
import com.ahumada.fuse.utils.FunctionUtils;
import com.equifax.cl.schema.eidcomparevalidator.geteidcomparevalidatorbreq.GeteIDCompareValidatorRequest;
import com.equifax.cl.schema.eidcomparevalidator.geteidcomparevalidatorbresp.GeteIDCompareValidatorResponse;

import cl.equifax.dws.osb_efx.equifax.eidcomparevalidator.EIDCompareValidator;
import cl.equifax.dws.osb_efx.equifax.eidcomparevalidator.EIDCompareValidator_Service;

public class IdCheckerServiceProvider {

	private Logger logger = Logger.getLogger(getClass());

	@PropertyInject(value = "wsdlURLServiceProvider")
	private String wsdlURLServiceProvider;
	@PropertyInject(value = "targetNamespaceNameServiceProvider")
	private String targetNamespaceNameServiceProvider;
	@PropertyInject(value = "serviceNameServiceProvider")
	private String serviceNameServiceProvider;
	@PropertyInject(value = "addressServiceProvider")
	private String addressServiceProvider;
	@PropertyInject(value = "usuarioServiceProvider")
	private String usuarioServiceProvider;
	@PropertyInject(value = "claveServiceProvider")
	private String claveServiceProvider;


	public IdCheckerServiceProvider() {
		//		 Set Manually QA parameters
		//		setWsdlURLServiceProvider("http://localhost:8088/mockeIDCompareValidatorSOAP?wsdl"); // Local Mock
		//		setWsdlURLServiceProvider("https://qws.equifax.cl/efc-eid-compare-validator-soap/Equifax/eIDCompareValidator.wsdl");
		//		setTargetNamespaceNameServiceProvider("http://dws.equifax.cl/osb-efx/Equifax/eIDCompareValidator");
		//		setServiceNameServiceProvider("eIDCompareValidator");
		//		setAddressServiceProvider("https://qws.equifax.cl/efc-eid-compare-validator-soap/Equifax/eIDCompareValidator");
		//		setUsuarioServiceProvider("eIDComp01");
		//		setClaveServiceProvider("dicom7");
	}

	/*
	 * Call ID Checker Provider 
	 */
	public RestResponse callIdCheckerProvider(RestRequest req, boolean isDataUpdate, ConnectionManager connManager) {

		Fv29ClienteDatos clienteDatos = null;
		RestResponse restResponse;
		URL wsdlUrl;

		try {

			wsdlUrl = new URL(getWsdlURLServiceProvider());
			QName serviceName = new QName(getTargetNamespaceNameServiceProvider(), getServiceNameServiceProvider());
			EIDCompareValidator_Service service = new EIDCompareValidator_Service(wsdlUrl,serviceName);
			EIDCompareValidator client = service.getEIDCompareValidatorSOAP();

			// Set Endpoint - SOAP address
			BindingProvider provider = (BindingProvider)client;
			provider.getRequestContext().put(
					BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
					getAddressServiceProvider());

			// Build Request
			GeteIDCompareValidatorRequest requestSOAP = new GeteIDCompareValidatorRequest();
			requestSOAP.setRut(req.getRut());
			requestSOAP.setSerie(req.getSerie());
			requestSOAP.setUsuario(getUsuarioServiceProvider());
			requestSOAP.setClave(getClaveServiceProvider());

			GeteIDCompareValidatorResponse responseSOAP = null;
			responseSOAP = client.geteIDCompareValidator(requestSOAP);
			// Evaluate response
			if(responseSOAP != null && !FunctionUtils.stringIsNullOrEmpty(responseSOAP.getExiste()) && responseSOAP.getExiste().equalsIgnoreCase("S")) {
				restResponse = new RestResponse(true);
				restResponse.setMessage(String.format("RUT %s encontrado ", responseSOAP.getStatusDoctoIdentidad().getNumero()));

				// Convert Response to Model
				clienteDatos = geteIDCompareValidatorResponseToModel(responseSOAP);

			} else {
				restResponse = new RestResponse(false);
				restResponse.setMessage(String.format("RUT %s no encontrado ", responseSOAP.getStatusDoctoIdentidad().getNumero()));
			}

		} catch (MalformedURLException e) {
			restResponse = new RestResponse(false);
			restResponse.setMessage(e.getMessage());
			logger.error(e.getMessage());			
		} catch (Exception e) {
			restResponse = new RestResponse(false);
			StringBuilder err = new StringBuilder();
			if(e instanceof SOAPFaultException) {
				err.append(String.format("El servicio externo devolvió un error al consultar este RUT. FaultString :: %s . Detail :: %s", 
						((SOAPFaultException)e).getFault().getFaultString(),
						((SOAPFaultException)e).getFault().getDetail().getTextContent()));
			} else {
				err.append("Error al llamar a un servicio externo para consultar datos a través de RUT");
				logger.error(e.getMessage());			
			}
			restResponse.setMessage(err.toString());
			logger.error(e.getMessage());			
		}

		// Save response to database
		if(clienteDatos != null) {
			try {
				DbHelper.upsertFv29ClienteDatos(clienteDatos, isDataUpdate, connManager.getConnection());
			} catch (SQLException e) {
				logger.error(String.format("Error al guardar los datos del cliente devueltos por Equifax en la base de datos :: RUT %s", req.getRut()));
			}
		}

		return restResponse;
	}

	/*
	 * Convert SOAP response to model
	 */
	private Fv29ClienteDatos geteIDCompareValidatorResponseToModel(
			GeteIDCompareValidatorResponse response) {

		Fv29ClienteDatos model = null;
		if(!FunctionUtils.stringIsNullOrEmpty(response.getExiste()) && 
				response.getStatusDoctoIdentidad() != null &&
				!FunctionUtils.stringIsNullOrEmpty(response.getStatusDoctoIdentidad().getNumero())) {

			// Build root element
			model = new Fv29ClienteDatos(response.getStatusDoctoIdentidad().getNumero(), response.getExiste());

			// Root
			if(!FunctionUtils.stringIsNullOrEmpty(response.getIdTransaction())) 
				model.setIdTransaction(response.getIdTransaction());

			// Status Docto Identidad
			if(!FunctionUtils.stringIsNullOrEmpty(response.getStatusDoctoIdentidad().getTipo())) 
				model.setDocTipo(response.getStatusDoctoIdentidad().getTipo());

			if(!FunctionUtils.stringIsNullOrEmpty(response.getStatusDoctoIdentidad().getSerie())) 
				model.setDocSerie(response.getStatusDoctoIdentidad().getSerie());

			if(!FunctionUtils.stringIsNullOrEmpty(response.getStatusDoctoIdentidad().getEstado())) 
				model.setDocEstado(response.getStatusDoctoIdentidad().getEstado());

			if(!FunctionUtils.stringIsNullOrEmpty(response.getStatusDoctoIdentidad().getMotivo())) 
				model.setDocMotivo(response.getStatusDoctoIdentidad().getMotivo());

			// Personal Information
			if(response.getPersonalInformation() != null) {

				if(!FunctionUtils.stringIsNullOrEmpty(response.getPersonalInformation().getNombres()))
					model.setNombres(response.getPersonalInformation().getNombres());

				if(!FunctionUtils.stringIsNullOrEmpty(response.getPersonalInformation().getApellidoPaterno()))
					model.setApaterno(response.getPersonalInformation().getApellidoPaterno());

				if(!FunctionUtils.stringIsNullOrEmpty(response.getPersonalInformation().getApellidoMaterno()))
					model.setAmaterno(response.getPersonalInformation().getApellidoMaterno());

				if(!FunctionUtils.stringIsNullOrEmpty(response.getPersonalInformation().getNacionalidad()))
					model.setNacionalidad(response.getPersonalInformation().getNacionalidad());

				if(!FunctionUtils.stringIsNullOrEmpty(response.getPersonalInformation().getSexo()))
					model.setSexo(response.getPersonalInformation().getSexo());

				if(!FunctionUtils.stringIsNullOrEmpty(response.getPersonalInformation().getEstadoCivil()))
					model.setEcivil(response.getPersonalInformation().getEstadoCivil());

				if(!FunctionUtils.stringIsNullOrEmpty(response.getPersonalInformation().getFechaNacimiento()))
					model.setFnacimiento(response.getPersonalInformation().getFechaNacimiento());

				if(!FunctionUtils.stringIsNullOrEmpty(response.getPersonalInformation().getFechaDefuncion()))
					model.setFdefuncion(response.getPersonalInformation().getFechaDefuncion());

				if(!FunctionUtils.stringIsNullOrEmpty(response.getPersonalInformation().getFechaMatrimonio()))
					model.setFmatrimonio(response.getPersonalInformation().getFechaMatrimonio());

			}
		}

		return model;

	}

	public boolean isValidServiceProvider() throws Exception{
		if(FunctionUtils.stringIsNullOrEmpty(getUsuarioServiceProvider())){
			String err = "El usuario del proveedor de verificación de RUT no se encuentra en el archivo de configuración de la aplicación.";
			logger.error(err);
			throw new Exception(err);
		}
		if(FunctionUtils.stringIsNullOrEmpty(getClaveServiceProvider())) {
			String err = "La contraseña del proveedor de verificación de RUT no se encuentra en el archivo de configuración de la aplicación.";
			logger.error(err);
			throw new Exception(err);
		}
		if(FunctionUtils.stringIsNullOrEmpty(getWsdlURLServiceProvider())) {
			String err = "No se encontró lo parametro wsdlURLServiceProvider en el archivo de configuración de la aplicación.";
			logger.error(err);
			throw new Exception(err);
		}
		if(FunctionUtils.stringIsNullOrEmpty(getTargetNamespaceNameServiceProvider())) {
			String err = "No se encontró lo parametro targetNamespaceNameServiceProvider en el archivo de configuración de la aplicación.";
			logger.error(err);
			throw new Exception(err);
		}
		if(FunctionUtils.stringIsNullOrEmpty(getServiceNameServiceProvider())) {
			String err = "No se encontró lo parametro serviceNameServiceProvider en el archivo de configuración de la aplicación.";
			logger.error(err);
			throw new Exception(err);
		}
		if(FunctionUtils.stringIsNullOrEmpty(getAddressServiceProvider())) {
			String err = "No se encontró lo parametro addressServiceProvider en el archivo de configuración de la aplicación.";
			logger.error(err);
			throw new Exception(err);
		}
		return true;
	}
	/*
	 * Getters and setters for injected properties
	 */
	public String getUsuarioServiceProvider() {
		return usuarioServiceProvider;
	}
	public String getTargetNamespaceNameServiceProvider() {
		return targetNamespaceNameServiceProvider;
	}

	public void setTargetNamespaceNameServiceProvider(String targetNamespaceNameServiceProvider) {
		this.targetNamespaceNameServiceProvider = targetNamespaceNameServiceProvider;
	}

	public String getServiceNameServiceProvider() {
		return serviceNameServiceProvider;
	}

	public void setServiceNameServiceProvider(String serviceNameServiceProvider) {
		this.serviceNameServiceProvider = serviceNameServiceProvider;
	}

	public String getAddressServiceProvider() {
		return addressServiceProvider;
	}

	public void setAddressServiceProvider(String addressServiceProvider) {
		this.addressServiceProvider = addressServiceProvider;
	}

	public void setUsuarioServiceProvider(String usuarioServiceProvider) {
		this.usuarioServiceProvider = usuarioServiceProvider;
	}
	public String getClaveServiceProvider() {
		return claveServiceProvider;
	}
	public void setClaveServiceProvider(String claveServiceProvider) {
		this.claveServiceProvider = claveServiceProvider;
	}
	public String getWsdlURLServiceProvider() {
		return wsdlURLServiceProvider;
	}
	public void setWsdlURLServiceProvider(String wsdlURLServiceProvider) {
		this.wsdlURLServiceProvider = wsdlURLServiceProvider;
	}

}
