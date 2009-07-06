/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

/**
 * Zimbra Servers enable/disable settings overridable by LC.
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

	public String getId() {
		return "zimbra";
	}
	
	public String getInstallId() {
	    return "zimbra";
	}

	public boolean supports(String className) {
		return true;
	}

    public void startup()  { }

	private boolean isShutdown;

	public void shutdown() {
		isShutdown = true;
	}

	public boolean isShutdown() {
		return isShutdown;
	}
}
