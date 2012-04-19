/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.soap;

import org.dom4j.QName;
import org.dom4j.Namespace;

public class VoiceAdminConstants {
    public static final String NAMESPACE_STR = AdminConstants.NAMESPACE_STR;
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);
    
    public static final String E_GET_ALL_UC_PROVIDERS_REQUEST = "GetAllUCProvidersRequest";
    public static final String E_GET_ALL_UC_PROVIDERS_RESPONSE = "GetAllUCProvidersResponse";
    
    public static final QName GET_ALL_UC_PROVIDERS_REQUEST = QName.get(E_GET_ALL_UC_PROVIDERS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_UC_PROVIDERS_RESPONSE = QName.get(E_GET_ALL_UC_PROVIDERS_RESPONSE, NAMESPACE);
    
    
    public static final String E_PROVIDER = "provider";
}
