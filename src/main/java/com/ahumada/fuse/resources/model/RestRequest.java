package com.ahumada.fuse.resources.model;

import java.io.Serializable;

import com.ahumada.fuse.utils.JSonUtilities;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RestRequest implements Serializable {

	private static final long serialVersionUID = -1672571109821602672L;

	private String rut;
	private String serie;
	
	public RestRequest() {}

	public String getRut() {
		return rut;
	}


	public void setRut(String rut) {
		this.rut = rut;
	}


	public String getSerie() {
		return serie;
	}


	public void setSerie(String serie) {
		this.serie = serie;
	}


	@JsonIgnore
	public String toString() {
		try {
			return JSonUtilities.getInstance().java2json(this);
		} catch (Exception e) {
			return e.getMessage();
		}
	}

}
