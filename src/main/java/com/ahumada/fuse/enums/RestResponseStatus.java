package com.ahumada.fuse.enums;

public enum RestResponseStatus {
	
	SUCCESS			(true, 	200, "Request sucessfully processed"),
	BAD_REQUEST		(false, 400, "Input data wrong or missing"),
	NOT_FOUND		(false, 404, "Data not found"),
	UNEXPECTED_ERROR(false, 500, "Unexpected error");
	
	private final boolean success;
	private final int statusCode;
	private final String description;
	
	private RestResponseStatus(boolean success, int statusCode, String description) {
		this.success = success;
		this.statusCode = statusCode;
		this.description = description;
	}

	public boolean isSuccess() {
		return success;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getDescription() {
		return description;
	}
	
	
}
