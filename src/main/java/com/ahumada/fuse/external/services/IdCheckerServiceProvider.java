package com.ahumada.fuse.external.services;

import java.net.URL;

import org.apache.camel.PropertyInject;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.log4j.Logger;

import com.ahumada.fuse.db.ConnectionManager;
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
	@PropertyInject(value = "usuarioServiceProvider")
	private String usuarioServiceProvider;
	@PropertyInject(value = "claveServiceProvider")
	private String claveServiceProvider;
	

	public IdCheckerServiceProvider() {
		//TEMP CONSTRUCTOR
		setWsdlURLServiceProvider("http://localhost:8088/mockeIDCompareValidatorSOAP?wsdl");
		setUsuarioServiceProvider("eIDComp01");
		setClaveServiceProvider("dicom7");
	}
	
	/*
	 * Call ID Checker Provider 
	 */
	public RestResponse callIdCheckerProvider(RestRequest req, ConnectionManager connManager) {

		RestResponse restResponse;
		URL wsdlUrl;
		
		try {
			
			
			wsdlUrl = new URL(getWsdlURLServiceProvider());
			//QName serviceEndpoint = new QName("https://qws.equifax.cl/efc-eid-compare-validator-soap/Equifax/eIDCompareValidator", "eIDCompareValidator");
			EIDCompareValidator_Service service = new EIDCompareValidator_Service(wsdlUrl);
			EIDCompareValidator port = service.getEIDCompareValidatorSOAP();
			
			Client cxfClient = ClientProxy.getClient(port);
			cxfClient.getInFaultInterceptors().add(new ChangeSoapFaultCodeInterceptor());
		    
			GeteIDCompareValidatorRequest requestSOAP = new GeteIDCompareValidatorRequest();
			requestSOAP.setRut(req.getRut());
			requestSOAP.setSerie(req.getSerie());
			requestSOAP.setUsuario(getUsuarioServiceProvider());
			requestSOAP.setClave(getClaveServiceProvider());
			
			GeteIDCompareValidatorResponse responseSOAP = null;
			responseSOAP = port.geteIDCompareValidator(requestSOAP);
			// Evaluate response
			if(responseSOAP != null && !FunctionUtils.stringIsNullOrEmpty(responseSOAP.getExiste()) && responseSOAP.getExiste().equalsIgnoreCase("S")) {
				restResponse = new RestResponse(true);
				restResponse.setMessage(String.format("RUT %s encontrado ", responseSOAP.getStatusDoctoIdentidad().getNumero()));
			} else {
				restResponse = new RestResponse(false);
				restResponse.setMessage(String.format("RUT %s no encontrado ", responseSOAP.getStatusDoctoIdentidad().getNumero()));
			}
			
//		} catch (MalformedURLException e) {
//			restResponse = new RestResponse(false);
//			restResponse.setMessage(e.getMessage());
//			logger.error(e.getMessage());			
		} catch (Exception e) {
			restResponse = new RestResponse(false);
			restResponse.setMessage(e.getMessage());
			logger.error(e.getMessage());			
		}
		
		return restResponse;
	}
	
	/*
	 * Getters and setters for injected properties
	 */
	public String getUsuarioServiceProvider() {
		return usuarioServiceProvider;
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
