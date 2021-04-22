package com.ahumada.fuse.enums;

public enum MessageListEnum {

	SERVICE_NAME 				("ID CHECKER API"),

	ID_CHECKER_PROVIDER_ERROR_CONNECTION		("IOException :: Failed to communicate with ID Checker Provider services :: %s"),
	ID_CHECKER_PROVIDER_ERROR_URL				("MalformedURLException :: %s :: ID Checker Provider URL isn't correct %s "),
	ID_CHECKER_PROVIDER_ERROR_PARSING			("IOException :: %s :: Error to parse response from ID Checker Provider %s"),
	ID_CHECKER_PROVIDER_ERROR_LOGREQUEST		("SQLException :: %s :: Error to save request log to ID Checker Provider %s"),
	ID_CHECKER_PROVIDER_NOENDPOINT				("Couldn't find ID Checker Provider endpoint URL on config files :: %s"),
	ID_CHECKER_PROVIDER_NORESPONSE				("ID Checker Provider didn't return a response"),
	
	SQLERROR_CLOSE_CONNECTION		("Error when closing DB connection :: %s"),
	SQLERROR_CONNECTION_PARAM		("Error to setup database connection :: %s"),

	GENERIC_IOEXCEPTION				("IOException :: %s "),
	GENERIC_EXCEPTION				("Exception :: %s");
	
	private String desc;
	
	MessageListEnum(String desc) {
		this.desc = desc;
	}
	
	public String getDesc() {
		return this.desc;
	}
}
