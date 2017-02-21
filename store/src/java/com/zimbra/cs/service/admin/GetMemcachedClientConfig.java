/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
