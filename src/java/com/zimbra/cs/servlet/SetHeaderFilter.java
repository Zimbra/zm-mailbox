/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.servlet;

import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.account.Entry;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.account.Key;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Log;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/** Sets headers for request. */
public class SetHeaderFilter implements Filter {
    private static final Log LOG = ZimbraLog.misc;
    private static final KeyValue[] NO_HEADERS = {};
    private static final ConcurrentMap<String, KeyValue[]> RESPONSE_HEADERS = new ConcurrentHashMap<String, KeyValue[]>();

    public static final String P_RESPONSE_HEADERS_ENABLED = "zimbraResponseHeader.enabled";
    public static final Pattern RE_HEADER = Pattern.compile("^([^:]+):\\s+(.*)$");
    public static final String UNKNOWN_HEADER_NAME = "X-Zimbra-Unknown-Header";

    private boolean isResponseHeadersEnabled = true;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (doFilter(request, response)) {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String s = filterConfig.getInitParameter(P_RESPONSE_HEADERS_ENABLED);
        if (s != null) {
            isResponseHeadersEnabled = Boolean.parseBoolean(s.trim().toLowerCase());
        }
    }

    @Override
    public void destroy() {
    }

    /**
     * Subclass may override.
     *
     * @throws IOException subclass may throw
     * @throws ServletException subclass may throw
     */
    public boolean doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        addZimbraResponseHeaders(request, response);
        return true;
    }

    private void addZimbraResponseHeaders(ServletRequest request, ServletResponse response) {
        if (!isResponseHeadersEnabled) {
            return;
        }
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        if (httpRequest.getRequestURI().startsWith("/service/admin/soap")) {
            return;
        }
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        String serverName = HttpUtil.getVirtualHost(httpRequest);
        KeyValue[] headers = getResponseHeaders(serverName);
        this.addHeaders(httpResponse, headers);
    }

    private KeyValue[] getResponseHeaders(String serverName) {
        KeyValue[] headers = RESPONSE_HEADERS.get(serverName);
        if (headers == null) {
            headers = NO_HEADERS;
            try {
                SoapProvisioning provisioning = new SoapProvisioning();
                String soapUri = LC.zimbra_admin_service_scheme.value() + LC.zimbra_zmprov_default_soap_server.value() +
                    ':' + LC.zimbra_admin_service_port.intValue() + AdminConstants.ADMIN_SERVICE_URI;
                provisioning.soapSetURI(soapUri);
                provisioning.soapZimbraAdminAuthenticate();
                Entry info = provisioning.getDomainInfo(Key.DomainBy.virtualHostname, serverName);
                if (info == null) {
                    info = provisioning.getConfig();
                }
                if (info != null) {
                    String[] values = info.getMultiAttr(ZAttrProvisioning.A_zimbraResponseHeader, true);
                    headers = new KeyValue[values.length];
                    for (int i = 0; i < values.length; i++) {
                        String value = values[i];
                        Matcher matcher = RE_HEADER.matcher(value);
                        if (matcher.matches()) {
                            headers[i] = new KeyValue(matcher.group(1), matcher.group(2));
                        } else {
                            headers[i] = new KeyValue(value);
                        }
                    }
                }
            } catch (Exception e) {
                getLogger().error("Unable to get domain config", e);
            }
            RESPONSE_HEADERS.putIfAbsent(serverName, headers);
        }
        return headers;
    }

    private void addHeaders(HttpServletResponse response, KeyValue[] headers) {
        if (headers == null) {
            return;
        }
        for (KeyValue header : headers) {
            addHeader(response, header);
        }
    }

    private void addHeader(HttpServletResponse response, KeyValue header) {
        response.addHeader(header.key, header.value);
    }

    protected Log getLogger() {
        return LOG;
    }

    private static final class KeyValue {
        public final String key;
        public final String value;

        public KeyValue(String value) {
            this(SetHeaderFilter.UNKNOWN_HEADER_NAME, value);
        }

        public KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

}
