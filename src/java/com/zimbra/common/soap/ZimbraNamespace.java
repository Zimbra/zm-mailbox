/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 29, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.common.soap;

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
    public static final QName E_REASON = QName.get("Reason", ZIMBRA);
    public static final QName E_TEXT = QName.get("Text", ZIMBRA);
    public static final QName E_DETAIL = QName.get("Detail", ZIMBRA);
    public static final QName E_ARGUMENT = QName.get("a", ZIMBRA);

    public static final String A_ONERROR = "onerror";
    public static final String A_REQUEST_ID = "requestId";
    public static final String A_ARG_NAME = "n";
    public static final String A_ARG_TYPE = "t";
    public static final String DEF_ONERROR = "continue";
    
    public static final String E_NOTIFY   = "notify";
    public static final String E_REFRESH  = "refresh";
    public static final String E_TAGS     = "tags";
    public static final String E_CREATED  = "created";
    public static final String E_DELETED  = "deleted";
    public static final String E_MODIFIED = "modified";
    public static final String E_IM       = "im";

    public static final String ZIMBRA_ACCOUNT_STR = "urn:zimbraAccount";
    public static final Namespace ZIMBRA_ACCOUNT = Namespace.get(ZIMBRA_ACCOUNT_STR);

}
