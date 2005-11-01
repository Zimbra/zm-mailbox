/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zimlet;

/**
 * 
 * @author jylee
 *
 */
public class ZimletException extends Exception {

	private ZimletException(String msg) {
		super(msg);
	}

	public static ZimletException INVALID_ZIMLET_DESCRIPTION(String msg) {
		return new ZimletException(msg);
	}
	
	public static ZimletException INVALID_ZIMLET_CONFIG(String msg) {
		return new ZimletException(msg);
	}
	
	public static ZimletException CANNOT_CREATE(String zimlet, String reason) {
		return new ZimletException("Cannot create Zimlet " + zimlet + ": " + reason);
	}
	
	public static ZimletException CANNOT_DELETE(String zimlet, String reason) {
		return new ZimletException("Cannot delete Zimlet " + zimlet + ": " + reason);
	}

	public static ZimletException CANNOT_ACTIVATE(String zimlet, String reason) {
		return new ZimletException("Cannot activate Zimlet " + zimlet + ": " + reason);
	}

	public static ZimletException CANNOT_DEACTIVATE(String zimlet, String reason) {
		return new ZimletException("Cannot deactivate Zimlet " + zimlet + ": " + reason);
	}
}
