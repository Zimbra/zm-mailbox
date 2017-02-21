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
