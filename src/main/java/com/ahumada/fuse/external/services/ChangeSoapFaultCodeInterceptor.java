package com.ahumada.fuse.external.services;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

public class ChangeSoapFaultCodeInterceptor extends AbstractPhaseInterceptor<Message> {

	private static Logger LOG = Logger.getLogger(ChangeSoapFaultCodeInterceptor.class);

	public ChangeSoapFaultCodeInterceptor()
	{
		super(Phase.PRE_INVOKE);
	}

	
	@Override
	public void handleMessage(Message message) throws Fault {
        XMLStreamReader reader = message.getContent(XMLStreamReader.class);
//        message.setContent(Exception.class, createErrorMessage(message, reader));
	}
	
	
//	public static String createErrorMessage(Message message, XMLStreamReader reader) {
//	}

}
