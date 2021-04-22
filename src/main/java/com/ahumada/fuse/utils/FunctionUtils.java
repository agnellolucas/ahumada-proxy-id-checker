package com.ahumada.fuse.utils;

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
}
