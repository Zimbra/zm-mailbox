/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
