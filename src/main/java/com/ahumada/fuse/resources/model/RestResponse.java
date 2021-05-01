package com.ahumada.fuse.resources.model;

import com.ahumada.fuse.utils.JSonUtilities;

public class RestResponse {

	private String message;
	private boolean success;
	private String vencimiento;

	public RestResponse(boolean success) {
		this.success = success;
	}

	public RestResponse(String message, boolean success) {
		this.message = message;
		this.success = success;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	public String getVencimiento() {
		return vencimiento;
	}

	public void setVencimiento(String vencimiento) {
		this.vencimiento = vencimiento;
	}

	public String toString() {
		try {
			return JSonUtilities.getInstance().java2json(this);
		} catch (Exception e) {
			return e.getMessage();
		}
	}
}
