/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav;

import org.dom4j.Namespace;
import org.dom4j.QName;

public class DavElements {
	public static final String WEBDAV_NS_STRING = "DAV:";
	public static final Namespace WEBDAV_NS = Namespace.get(WEBDAV_NS_STRING);
	
	// properties
	public static final String P_CREATIONDATE = "creationdate";
	public static final String P_DISPLAYNAME = "displayname";
	public static final String P_GETCONTENTLANGUAGE = "getcontentlanguage";
	public static final String P_GETCONTENTLENGTH = "getcontentlength";
	public static final String P_GETCONTENTTYPE = "getcontenttype";
	public static final String P_GETETAG = "getetag";
	public static final String P_GETLASTMODIFIED = "getlastmodified";
	public static final String P_LOCKDISCOVERY = "lockdiscovery";
	public static final String P_RESOURCETYPE = "resourcetype";
	public static final String P_SOURCE = "source";
	public static final String P_SUPPORTEDLOCK = "supportedlock";
	public static final String P_COLLECTION = "collection";
	
	// response
	public static final String P_MULTISTATUS = "multistatus";
	public static final String P_RESPONSE = "response";
	public static final String P_HREF = "href";
	public static final String P_STATUS = "status";

	// propfind
	public static final String P_PROPFIND = "propfind";
	public static final String P_ALLPROP = "allprop";
	public static final String P_PROPNAME = "propname";
	public static final String P_PROP = "prop";
	public static final String P_PROPSTAT = "propstat";
	
	public static final QName E_CREATIONDATE = QName.get(P_CREATIONDATE, WEBDAV_NS);
	public static final QName E_DISPLAYNAME = QName.get(P_DISPLAYNAME, WEBDAV_NS);
	public static final QName E_GETCONTENTLANGUAGE = QName.get(P_GETCONTENTLANGUAGE, WEBDAV_NS);
	public static final QName E_GETCONTENTLLENGTH = QName.get(P_GETCONTENTLENGTH, WEBDAV_NS);
	public static final QName E_GETCONTENTTYPE = QName.get(P_GETCONTENTTYPE, WEBDAV_NS);
	public static final QName E_GETETAG = QName.get(P_GETETAG, WEBDAV_NS);
	public static final QName E_GETLASTMODIFIED = QName.get(P_GETLASTMODIFIED, WEBDAV_NS);
	public static final QName E_LOCKDISCOVERY = QName.get(P_LOCKDISCOVERY, WEBDAV_NS);
	public static final QName E_RESOURCETYPE = QName.get(P_RESOURCETYPE, WEBDAV_NS);
	public static final QName E_SOURCE = QName.get(P_SOURCE, WEBDAV_NS);
	public static final QName E_SUPPORTEDLOCK = QName.get(P_SUPPORTEDLOCK, WEBDAV_NS);
	public static final QName E_COLLECTION = QName.get(P_COLLECTION, WEBDAV_NS);
	
	// response
	public static final QName E_MULTISTATUS = QName.get(P_MULTISTATUS, WEBDAV_NS);
	public static final QName E_RESPONSE = QName.get(P_RESPONSE, WEBDAV_NS);
	public static final QName E_HREF = QName.get(P_HREF, WEBDAV_NS);
	public static final QName E_STATUS = QName.get(P_STATUS, WEBDAV_NS);
	
	// propfind
	public static final QName E_PROPSTAT = QName.get(P_PROPSTAT, WEBDAV_NS);
	public static final QName E_PROP = QName.get(P_PROP, WEBDAV_NS);
}
