package com.zimbra.cs.util;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;


/**
 * Zimbra Servers enable/disable settings overridable by LC.
 * 
 * @author jjzhuang
 *
 */

public class ZimbraApplication {
	
	private static ZimbraApplication sServices;
	
	public static ZimbraApplication getInstance() {
		
		if (sServices == null) {
	        String className = LC.zimbra_class_application.value();
	        if (className != null && !className.equals("")) {
	            try {
	            	sServices = (ZimbraApplication)Class.forName(className).newInstance();
	            } catch (Exception e) {
	                ZimbraLog.misc.error("could not instantiate ZimbraServices interface of class '" + className + "'; defaulting to ZimbraServices", e);
	            }
	        }
	        if (sServices == null)
	        	sServices = new ZimbraApplication();
		}
		return sServices;
	}
	
	public boolean supports(String className) {
		return true;
	}
}
