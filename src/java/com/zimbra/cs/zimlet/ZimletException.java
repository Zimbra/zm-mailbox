/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zimlet;

/**
 * 
 * @author jylee
 *
 */
@SuppressWarnings("serial")
public class ZimletException extends Exception {

	private ZimletException(String msg) {
		super(msg);
	}

	private ZimletException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public static ZimletException ZIMLET_HANDLER_ERROR(String msg) {
		return new ZimletException(msg);
	}
	
	public static ZimletException INVALID_ZIMLET_DESCRIPTION(String msg) {
		return new ZimletException(msg);
	}
	
	public static ZimletException INVALID_ZIMLET_CONFIG(String msg) {
		return new ZimletException(msg);
	}
	
	public static ZimletException CANNOT_DEPLOY(String zimlet, Throwable cause) {
		return new ZimletException("Cannot deploy Zimlet " + zimlet, cause);
	}
	
	public static ZimletException CANNOT_CREATE(String zimlet, Throwable cause) {
		return new ZimletException("Cannot create Zimlet " + zimlet, cause);
	}
	
	public static ZimletException CANNOT_DELETE(String zimlet, Throwable cause) {
		return new ZimletException("Cannot delete Zimlet " + zimlet, cause);
	}
	
	public static ZimletException CANNOT_ACTIVATE(String zimlet, Throwable cause) {
		return new ZimletException("Cannot activate Zimlet " + zimlet, cause);
	}
	
	public static ZimletException CANNOT_DEACTIVATE(String zimlet, Throwable cause) {
		return new ZimletException("Cannot deactivate Zimlet " + zimlet, cause);
	}
	
	public static ZimletException CANNOT_ENABLE(String zimlet, Throwable cause) {
		return new ZimletException("Cannot enable Zimlet " + zimlet, cause);
	}
	
	public static ZimletException CANNOT_DISABLE(String zimlet, Throwable cause) {
		return new ZimletException("Cannot disable Zimlet " + zimlet, cause);
	}

	public static ZimletException CANNOT_FLUSH_CACHE(Throwable cause) {
		return new ZimletException("Cannot flush zimlet cache", cause);
	}
}
