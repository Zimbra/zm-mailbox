/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

import org.dom4j.QName;
import org.dom4j.Namespace;

public class VoiceAdminConstants {
    public static final String NAMESPACE_STR = AdminConstants.NAMESPACE_STR;
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);
    
    public static final String E_GET_ALL_UC_PROVIDERS_REQUEST = "GetAllUCProvidersRequest";
    public static final String E_GET_ALL_UC_PROVIDERS_RESPONSE = "GetAllUCProvidersResponse";
    public static final String E_UPDATE_PRESENCE_SESSION_ID_REQUEST = "UpdatePresenceSessionIdRequest";
    public static final String E_UPDATE_PRESENCE_SESSION_ID_RESPONSE = "UpdatePresenceSessionIdResponse";
    
    public static final QName GET_ALL_UC_PROVIDERS_REQUEST = QName.get(E_GET_ALL_UC_PROVIDERS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_UC_PROVIDERS_RESPONSE = QName.get(E_GET_ALL_UC_PROVIDERS_RESPONSE, NAMESPACE);
    public static final QName UPDATE_PRESENCE_SESSION_ID_REQUEST = QName.get(E_UPDATE_PRESENCE_SESSION_ID_REQUEST, NAMESPACE);
    public static final QName UPDATE_PRESENCE_SESSION_ID_RESPONSE = QName.get(E_UPDATE_PRESENCE_SESSION_ID_RESPONSE, NAMESPACE);
    
    public static final String E_PROVIDER = "provider";
}
