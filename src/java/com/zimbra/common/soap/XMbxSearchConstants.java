/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
    public static final String A_numMsgs = "numMsgs";

}
