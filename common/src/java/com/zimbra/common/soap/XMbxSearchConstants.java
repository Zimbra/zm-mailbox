/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

public final class XMbxSearchConstants {

    public static final String NAMESPACE_STR = AdminConstants.NAMESPACE_STR;
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final String E_GET_XMBX_SEARCH_REQUEST = "GetXMbxSearchRequest";
    public static final String E_GET_XMBX_SEARCH_RESPONSE = "GetXMbxSearchResponse";

    public static final String E_GET_XMBX_SEARCHES_REQUEST = "GetXMbxSearchesListRequest";
    public static final String E_GET_XMBX_SEARCHES_RESPONSE = "GetXMbxSearchesListResponse";

    public static final String E_CREATE_XMBX_SEARCH_REQUEST = "CreateXMbxSearchRequest";
    public static final String E_CREATE_XMBX_SEARCH_RESPONSE = "CreateXMbxSearchResponse";

    public static final String E_ABORT_XMBX_SEARCH_REQUEST = "AbortXMbxSearchRequest";
    public static final String E_ABORT_XMBX_SEARCH_RESPONSE = "AbortXMbxSearchResponse";

    public static final String E_DELETE_XMBX_SEARCH_REQUEST = "DeleteXMbxSearchRequest";
    public static final String E_DELETE_XMBX_SEARCH_RESPONSE = "DeleteXMbxSearchResponse";

    public static final QName GET_XMBX_SEARCH_REQUEST = QName.get(E_GET_XMBX_SEARCH_REQUEST, NAMESPACE);
    public static final QName GET_XMBX_SEARCH_RESPONSE = QName.get(E_GET_XMBX_SEARCH_RESPONSE, NAMESPACE);

    public static final QName GET_XMBX_SEARCHES_REQUEST = QName.get(E_GET_XMBX_SEARCHES_REQUEST, NAMESPACE);
    public static final QName GET_XMBX_SEARCHES_RESPONSE = QName.get(E_GET_XMBX_SEARCHES_RESPONSE, NAMESPACE);

    public static final QName CREATE_XMBX_SEARCH_REQUEST = QName.get(E_CREATE_XMBX_SEARCH_REQUEST, NAMESPACE);
    public static final QName CREATE_XMBX_SEARCH_RESPONSE = QName.get(E_CREATE_XMBX_SEARCH_RESPONSE, NAMESPACE);

    public static final QName ABORT_XMBX_SEARCH_REQUEST = QName.get(E_ABORT_XMBX_SEARCH_REQUEST, NAMESPACE);
    public static final QName ABORT_XMBX_SEARCH_RESPONSE = QName.get(E_ABORT_XMBX_SEARCH_RESPONSE, NAMESPACE);

    public static final QName DELETE_XMBX_SEARCH_REQUEST = QName.get(E_DELETE_XMBX_SEARCH_REQUEST, NAMESPACE);
    public static final QName DELETE_XMBX_SEARCH_RESPONSE = QName.get(E_DELETE_XMBX_SEARCH_RESPONSE, NAMESPACE);

    public static final String E_SrchTask = "searchtask";
    public static final String A_targetMbx = "targetMbx";
    public static final String A_status = "status";
    public static final String A_searchID = "searchID";
    public static final String A_accountId = "accountId";
    public static final String A_serverId = "serverId";
    public static final String A_serverName = "serverName";
    public static final String A_query = "query";
    public static final String A_sendNotification = "sendNotification";
    public static final String A_notificationReceivers = "notificationReceivers";
    public static final String A_notificationMessage = "notificationMessage";
    public static final String A_notificationSubj = "notificationSubj";
    public static final String A_searchArchives = "searchArchives";
    public static final String A_searchLive = "searchLive";
    public static final String A_searchAll = "searchAll";
    public static final String A_accounts = "accounts";
    public static final String A_targetFolder = "targetFolder";
    public static final String A_limit = "limit";
    public static final String A_pmLimit = "permailbox";
    public static final String A_numMsgs = "numMsgs";
    public static final String A_inDumpster = "inDumpster";

}
