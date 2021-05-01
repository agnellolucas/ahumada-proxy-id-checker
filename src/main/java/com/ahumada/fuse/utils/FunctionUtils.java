package com.ahumada.fuse.utils;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

public class FunctionUtils {

	public static boolean stringIsNullOrEmpty(String str) {
		if(str !=null) {
			return str.isEmpty();
		}
		return true;
	}

	public static boolean integerIsNullOrZero(Integer intr) {
		if(intr !=null) {
			return intr == 0;
		}
		return true;
	}

	public static String objToXmlString(Object obj) throws JAXBException {
		StringWriter sw = new StringWriter();
		JAXBContext context = JAXBContext.newInstance(obj.getClass());
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		m.marshal(m, sw);
		return sw.toString();
	}
}
