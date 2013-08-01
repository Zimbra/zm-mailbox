/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

/**
 * Constants used by the soap API
 * @author jpowers
 *
 */
public class AppBlastConstants {

    public static final String USER_SERVICE_URI  = "/service/soap/";

    public static final String NAMESPACE_STR = "urn:zimbraAppblast";
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    // Edit Documents
    public static final String E_EDIT_REQUEST = "EditDocumentRequest";
    public static final String E_EDIT_RESPONSE = "EditDocumentResponse";

    // Finish editing documents
    public static final String E_FINISH_EDIT_REQUEST = "FinishEditDocumentRequest";
    public static final String E_FINISH_EDIT_RESPONSE = "FinishEditDocumentResponse";

    public static final QName EDIT_REQUEST = QName.get(E_EDIT_REQUEST, NAMESPACE);
    public static final QName EDIT_RESPONSE = QName.get(E_EDIT_RESPONSE, NAMESPACE);

    public static final QName FINISH_EDIT_REQUEST = QName.get(E_FINISH_EDIT_REQUEST, NAMESPACE);
    public static final QName FINISH_EDIT_RESPONSE = QName.get(E_FINISH_EDIT_RESPONSE, NAMESPACE);

}
