/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016,2018 Synacor, Inc.
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
package com.zimbra.cs.dav.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.dom4j.io.XMLWriter;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.servlet.util.JettyUtil;

/**
 * Base class for DAV methods.
 *
 * @author jylee
 *
 */
public abstract class DavMethod {
    public abstract String getName();
    public abstract void handle(DavContext ctxt) throws DavException, IOException, ServiceException;

    public void checkPrecondition(DavContext ctxt) throws DavException, ServiceException {
    }

    public void checkPostcondition(DavContext ctxt) throws DavException, ServiceException {
    }

    @Override
    public String toString() {
        return "DAV method " + getName();
    }

    public String getMethodName() {
        return getName();
    }

    protected static final int STATUS_OK = HttpServletResponse.SC_OK;

    protected void sendResponse(DavContext ctxt) throws IOException {
        if (ctxt.isResponseSent())
            return;
        HttpServletResponse resp = ctxt.getResponse();
        resp.setStatus(ctxt.getStatus());
        String compliance = ctxt.getDavCompliance();
        if (compliance != null)
            setResponseHeader(resp, DavProtocol.HEADER_DAV, compliance);
        if (ctxt.hasResponseMessage()) {
            resp.setContentType(DavProtocol.DAV_CONTENT_TYPE);
            DavResponse respMsg = ctxt.getDavResponse();
            respMsg.writeTo(resp.getOutputStream());
        }
        ctxt.responseSent();
    }

    public static void setResponseHeader(HttpServletResponse resp, String name, String value) {
        while (value != null) {
            String val = value;
            if (value.length() > 70) {
                int index = value.lastIndexOf(',', 70);
                if (index == -1) {
                    ZimbraLog.dav.warn("header value is too long for %s : %s", name, value);
                    return;
                }
                val = value.substring(0, index);
                value = value.substring(index+1).trim();
            } else {
                value = null;
            }
            resp.addHeader(name, val);
        }
    }

    public HttpRequestBase toHttpMethod(DavContext ctxt, String targetUrl) throws IOException, DavException {
        if (ctxt.getUpload() != null && ctxt.getUpload().getSize() > 0) {
            HttpPost method = new HttpPost(targetUrl) {
                @Override
                public String getMethod() { return getMethodName(); }
            };
            HttpEntity reqEntry;
            if (ctxt.hasRequestMessage()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XMLWriter writer = new XMLWriter(baos);
                writer.write(ctxt.getRequestMessage());
                reqEntry = new ByteArrayEntity(baos.toByteArray());
            } else { // this could be a huge upload
                reqEntry = new InputStreamEntity(ctxt.getUpload().getInputStream(), ctxt.getUpload().getSize());
            }
            method.setEntity(reqEntry);
            return method;
        }
        return new HttpGet(targetUrl) {

            @Override
            public String getMethod() { return getMethodName(); }
        };
    }

    /**
     * Implemented for bug 79865
     *
     * Disable the Jetty timeout for for this request.
     *
     * By default (and our normal configuration) Jetty has a 60 second idle timeout (10 if the server is busy) for
     * connection endpoints. There's another task that keeps track of what connections have timeouts and periodically
     * works over a queue and closes endpoints that have been timed out. This plays havoc with DAV over slow connections
     * and whenever we have a long pause.
     *
     * @throws IOException
     */
    protected void disableJettyTimeout(DavContext context) throws IOException {
        // millisecond value.  0 or negative means infinite.
        long maxIdleTime = LC.zimbra_dav_max_idle_time_ms.intValue();
        JettyUtil.setIdleTimeout(maxIdleTime, context.getRequest());
    }

}
