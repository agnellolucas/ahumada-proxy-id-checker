package com.ahumada.fuse.external.services;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.camel.PropertyInject;

import com.ahumada.fuse.db.ConnectionManager;
import com.ahumada.fuse.db.model.Fv29ClienteDatos;
import com.ahumada.fuse.exceptions.BusinessHandledException;
import com.ahumada.fuse.resources.model.RestRequest;
import com.ahumada.fuse.utils.FunctionUtils;
import com.equifax.cl.schema.eidcomparevalidator.geteidcomparevalidatorbreq.GeteIDCompareValidatorRequest;
import com.equifax.cl.schema.eidcomparevalidator.geteidcomparevalidatorbresp.GeteIDCompareValidatorResponse;

import cl.equifax.dws.osb_efx.equifax.eidcomparevalidator.EIDCompareValidator;
import cl.equifax.dws.osb_efx.equifax.eidcomparevalidator.EIDCompareValidator_Service;

public class IdCheckerServiceProvider {

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
	public Fv29ClienteDatos callIdCheckerProvider(RestRequest req, ConnectionManager connManager) throws BusinessHandledException, Exception {

		Fv29ClienteDatos clienteDatos = null;
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
			if(responseSOAP != null) {
				clienteDatos = geteIDCompareValidatorResponseToModel(responseSOAP);
			} else {
				throw new BusinessHandledException("El proveedor de ID devolvió una respuesta sin datos.");
			}

		} catch (MalformedURLException e) {
			// throws exception with handled error message
			throw new BusinessHandledException(String.format("El archivo WSDL no está disponible :: URL %s", getWsdlURLServiceProvider()), e);
		} catch (Exception e) {
			StringBuilder err = new StringBuilder();
			if(e instanceof SOAPFaultException) {
				err.append(String.format("El servicio externo devolvió un error al consultar este RUT. FaultString :: %s . Detail :: %s", 
						((SOAPFaultException)e).getFault().getFaultString(),
						((SOAPFaultException)e).getFault().getDetail().getTextContent()));
				//TODO if necessary to save the fault details the method is ready, just check if we need to delete existing record
				//DbHelper.upsertFaultFv29ClienteDatos(clienteDatos, isDataUpdate, connManager.getConnection());
			} else {
				err.append("Error al llamar a un servicio externo para consultar datos a través de RUT");
			}
			// throws exception with handled error message
			throw new BusinessHandledException(err.toString(), e);
		}

		return clienteDatos;
	}

	/*
	 * Convert SOAP response to model
	 */
	private Fv29ClienteDatos geteIDCompareValidatorResponseToModel(
			GeteIDCompareValidatorResponse response) {

		Fv29ClienteDatos model = null;
		if(response.getStatusDoctoIdentidad() != null 
				&& !FunctionUtils.stringIsNullOrEmpty(response.getStatusDoctoIdentidad().getNumero())
				&& !FunctionUtils.stringIsNullOrEmpty(response.getStatusDoctoIdentidad().getNumero())) {

			// Build root element
			model = new Fv29ClienteDatos(response.getStatusDoctoIdentidad().getNumero(), response.getStatusDoctoIdentidad().getSerie());

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

	public boolean isValidServiceProvider() throws BusinessHandledException {
		if(FunctionUtils.stringIsNullOrEmpty(getUsuarioServiceProvider())){
			String err = "El usuario del proveedor de verificación de RUT no se encuentra en el archivo de configuración de la aplicación.";
			throw new BusinessHandledException(err);
		}
		if(FunctionUtils.stringIsNullOrEmpty(getClaveServiceProvider())) {
			String err = "La contraseña del proveedor de verificación de RUT no se encuentra en el archivo de configuración de la aplicación.";
			throw new BusinessHandledException(err);
		}
		if(FunctionUtils.stringIsNullOrEmpty(getWsdlURLServiceProvider())) {
			String err = "No se encontró lo parametro wsdlURLServiceProvider en el archivo de configuración de la aplicación.";
			throw new BusinessHandledException(err);
		}
		if(FunctionUtils.stringIsNullOrEmpty(getTargetNamespaceNameServiceProvider())) {
			String err = "No se encontró lo parametro targetNamespaceNameServiceProvider en el archivo de configuración de la aplicación.";
			throw new BusinessHandledException(err);
		}
		if(FunctionUtils.stringIsNullOrEmpty(getServiceNameServiceProvider())) {
			String err = "No se encontró lo parametro serviceNameServiceProvider en el archivo de configuración de la aplicación.";
			throw new BusinessHandledException(err);
		}
		if(FunctionUtils.stringIsNullOrEmpty(getAddressServiceProvider())) {
			String err = "No se encontró lo parametro addressServiceProvider en el archivo de configuración de la aplicación.";
			throw new BusinessHandledException(err);
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
