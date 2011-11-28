/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import org.dom4j.Namespace;
import org.dom4j.QName;

public final class ArchiveConstants {

    public static final String NAMESPACE_STR = AdminConstants.NAMESPACE_STR;
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final String E_NO_OP_REQUEST = "NoOpRequest";
    
    public static final String E_CREATE_ARCHIVE_REQUEST = "CreateArchiveRequest";
    public static final String E_CREATE_ARCHIVE_RESPONSE = "CreateArchiveResponse";
    
    public static final String E_ENABLE_ARCHIVE_REQUEST = "EnableArchiveRequest";
    public static final String E_ENABLE_ARCHIVE_RESPONSE = "EnableArchiveResponse";

    public static final String E_DISABLE_ARCHIVE_REQUEST = "DisableArchiveRequest";
    public static final String E_DISABLE_ARCHIVE_RESPONSE = "DisableArchiveResponse";

    public static final QName CREATE_ARCHIVE_REQUEST = QName.get(E_CREATE_ARCHIVE_REQUEST, NAMESPACE);
    public static final QName CREATE_ARCHIVE_RESPONSE = QName.get(E_CREATE_ARCHIVE_RESPONSE, NAMESPACE);
    
    public static final QName ENABLE_ARCHIVE_REQUEST = QName.get(E_ENABLE_ARCHIVE_REQUEST, NAMESPACE);
    public static final QName ENABLE_ARCHIVE_RESPONSE = QName.get(E_ENABLE_ARCHIVE_RESPONSE, NAMESPACE);
    
    public static final QName DISABLE_ARCHIVE_REQUEST = QName.get(E_DISABLE_ARCHIVE_REQUEST, NAMESPACE);
    public static final QName DISABLE_ARCHIVE_RESPONSE = QName.get(E_DISABLE_ARCHIVE_RESPONSE, NAMESPACE);

    public static final String E_ARCHIVE = "archive";
    public static final String A_CREATE = "create";
}
