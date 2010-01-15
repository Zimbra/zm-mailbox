/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.soap.ZimbraSoapContext;

public class GetMemcachedClientConfig extends AdminDocumentHandler {

    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Element response = zsc.createElement(AdminConstants.GET_MEMCACHED_CLIENT_CONFIG_RESPONSE);
        ZimbraMemcachedClient zmcd = MemcachedConnector.getClient();
        if (zmcd != null) {
            response.addAttribute(AdminConstants.A_MEMCACHED_CLIENT_CONFIG_SERVER_LIST, zmcd.getServerList());
            response.addAttribute(AdminConstants.A_MEMCACHED_CLIENT_CONFIG_HASH_ALGORITHM, zmcd.getHashAlgorithm());
            response.addAttribute(AdminConstants.A_MEMCACHED_CLIENT_CONFIG_BINARY_PROTOCOL, zmcd.getBinaryProtocolEnabled());
            response.addAttribute(AdminConstants.A_MEMCACHED_CLIENT_CONFIG_DEFAULT_EXPIRY_SECONDS, zmcd.getDefaultExpirySeconds());
            response.addAttribute(AdminConstants.A_MEMCACHED_CLIENT_CONFIG_DEFAULT_TIMEOUT_MILLIS, zmcd.getDefaultTimeoutMillis());
        }
        return response;
    }
}
