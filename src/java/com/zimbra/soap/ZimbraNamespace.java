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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 29, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

/**
 * @author schemers
 *
 * Registry for assigned XML namespaces
 */
public class ZimbraNamespace {
	
	public static final String ZIMBRA_STR = "urn:zimbra";
	public static final Namespace ZIMBRA = Namespace.get(ZIMBRA_STR);

    public static final QName E_BATCH_REQUEST = QName.get("BatchRequest", ZIMBRA);
    public static final QName E_BATCH_RESPONSE = QName.get("BatchResponse", ZIMBRA);
    public static final QName E_CODE = QName.get("Code", ZIMBRA); 
    public static final QName E_ERROR = QName.get("Error", ZIMBRA); 
    public static final QName E_TRACE = QName.get("Trace", ZIMBRA);     

    public static final String A_ONERROR = "onerror";
    public static final String DEF_ONERROR = "continue";
    
    public static final String E_NOTIFY   = "notify";
    public static final String E_REFRESH  = "refresh";
    public static final String E_TAGS     = "tags";
    public static final String E_CREATED  = "created";
    public static final String E_DELETED  = "deleted";
    public static final String E_MODIFIED = "modified";
    
	public static final String ZIMBRA_ACCOUNT_STR = "urn:zimbraAccount";
	public static final Namespace ZIMBRA_ACCOUNT = Namespace.get(ZIMBRA_ACCOUNT_STR);

}
