package com.ahumada.fuse.exceptions;

public class BusinessHandledException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public BusinessHandledException(String errorMessage, Throwable err) {
		super(errorMessage, err);
	}
	
	public BusinessHandledException(String errorMessage) {
		super(errorMessage);
	}
}
