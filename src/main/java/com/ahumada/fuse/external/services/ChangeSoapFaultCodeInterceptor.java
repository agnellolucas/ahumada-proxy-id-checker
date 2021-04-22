package com.ahumada.fuse.external.services;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class ChangeSoapFaultCodeInterceptor extends AbstractPhaseInterceptor<Message>
{

	public ChangeSoapFaultCodeInterceptor()
	{
		super(Phase.POST_INVOKE);
	}

	public void handleMessage(Message message)
	{
		Exception exception = message.getContent(Exception.class);
		if(exception instanceof Fault)
		{
			Fault fault = (Fault)exception;
			System.out.println(String.format("Fault message: {%s}", fault.getMessage()));
		}
	}

	private static String modifySoapFaultCode(String message) {
		message = message.replaceAll("<faultcode>([^<]*)</faultcode>", "<faultcode>Server</faultcode>");
		System.out.println("After change message is " + message);

		return message;
	}

}
