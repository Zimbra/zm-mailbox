/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Appliance
 * Copyright (C) 2011 VMware, Inc.
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
package com.zimbra.cs.appliance.servapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.HttpException;
import com.zimbra.cs.appliance.httpclient.HttpProxyUtil;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import com.zimbra.common.localconfig.LC;

/**
 * @author pjoseph
 */
@SuppressWarnings("serial")
public class ApplianceProxyServlet extends HttpServlet {
    private static final String TARGET_PARAM = "target";
    private static final String USER_PARAM = "cimuser";
    private static final String PASS_PARAM = "cimpass";
    private static final String FORMAT_PARAM = "fmt";
    private static final String FILENAME_PARAM = "filename";
    private static final String AUTH_PARAM = "cimauth";
    private static final String AUTH_BASIC = "basic";
    private static final int MAX_PROXY_HOPCOUNT = 3;
    public static final String COOKIE_ZCA_VAMI_AUTH_TOKEN       = "ZCA_VAMI_AUTH_TOKEN";	  
	
    
    private boolean canProxyHeader(String header) {
        if (header == null) return false;
        header = header.toLowerCase();
        if (header.startsWith("accept") ||
            header.equals("content-length") ||
            header.equals("connection") ||
            header.equals("keep-alive") ||
            header.equals("expires") ||
            header.equals("pragma") ||
            header.equals("host") ||
            //header.equals("user-agent") ||
            header.equals("cache-control") ||
            header.equals("cookie") ||
            header.equals("transfer-encoding")) {
            return false;
        }
        return true;
    }
    
    private byte[] copyPostedData(HttpServletRequest req) throws IOException {
        int size = req.getContentLength();
        if (req.getMethod().equalsIgnoreCase("GET") || size <= 0) {
            return null;
        }
        InputStream is = req.getInputStream();
        ByteArrayOutputStream baos = null;
        try {
            if (size < 0)
                size = 0; 
            baos = new ByteArrayOutputStream(size);
            byte[] buffer = new byte[8192];
            int num;
            while ((num = is.read(buffer)) != -1) {
                baos.write(buffer, 0, num);
            }
            return baos.toByteArray();
        } finally {
            com.zimbra.cs.appliance.util.ByteUtil.closeStream(baos);
        }
    }


    private static boolean hasZCAAuthCookie(HttpState state) {
        Cookie[] cookies = state.getCookies();
        if (cookies == null)
            return false;

        for (Cookie c: cookies) {
            if (c.getName().equals(COOKIE_ZCA_VAMI_AUTH_TOKEN))
                return true;
        }
        return false;
    }

    protected boolean isAdminRequest(HttpServletRequest req) {
        return req.getServerPort() == LC.zimbra_admin_service_port.intValue();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doProxy(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doProxy(req, resp);
    }

    private static final String DEFAULT_CTYPE = "text/xml";

    private void doProxy(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	boolean isAdmin = isAdminRequest(req);
        if (!isAdmin)
            return;
	boolean hasZCAAuth = false;
        javax.servlet.http.Cookie cookies[] = req.getCookies();
        //String hostname = method.getURI().getHost();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(COOKIE_ZCA_VAMI_AUTH_TOKEN)){
                    hasZCAAuth = true;
                    continue;
		}
                //state.addCookie(new Cookie(hostname, cookies[i].getName(), cookies[i].getValue(), "/", null, false));
            }
        }
	if (!hasZCAAuth) 
		return;

        // get the posted body before the server read and parse them.
        byte[] body = copyPostedData(req);

        // sanity check
        String target = req.getParameter(TARGET_PARAM);
        if (target == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        URL url = new URL(target);
 
        HttpMethod method = null;
        try {
            HttpClient client = new HttpClient();
            com.zimbra.cs.appliance.httpclient.HttpProxyUtil.configureProxy(client,url.toString());
            String reqMethod = req.getMethod();
            if (reqMethod.equalsIgnoreCase("GET"))
                method = new GetMethod(target);
            else if (reqMethod.equalsIgnoreCase("POST")) {
                PostMethod post = new PostMethod(target);
                if (body != null)
                    post.setRequestEntity(new ByteArrayRequestEntity(body, req.getContentType()));
                
                Enumeration<String> params = req.getParameterNames();
                if(params != null) {
                	while(params.hasMoreElements()) {
                		String paramName = params.nextElement();
                		if(TARGET_PARAM.equalsIgnoreCase(paramName)) {
                			continue;
                		}
                		post.addParameter(paramName, req.getParameter(paramName));
                	}
                	
                }
                method = post;
            } else {
                resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }

            // handle basic auth
            String auth, user, pass;
            auth = req.getHeader(AUTH_PARAM);
            user = req.getHeader(USER_PARAM);
            pass = req.getHeader(PASS_PARAM);
            if (auth != null && user != null && pass != null) {
                if (!auth.equals(AUTH_BASIC)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                HttpState state = client.getState();
                client.getParams().setAuthenticationPreemptive(true);
                AuthScope authScope = new AuthScope(url.getHost(),url.getPort());
                state.setCredentials(authScope, new UsernamePasswordCredentials(user, pass));
            }
            
            Enumeration headers = req.getHeaderNames();
            while (headers.hasMoreElements()) {
                String hdr = (String) headers.nextElement();
                if (canProxyHeader(hdr)) {
                	if (hdr.equalsIgnoreCase("x-host"))
                		method.getParams().setVirtualHost(req.getHeader(hdr));
                	else
                		method.addRequestHeader(hdr, req.getHeader(hdr));
                }
            }
            
            try {
                client.executeMethod(method);
            } catch (HttpException ex) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            int status = method.getStatusLine() == null ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : method.getStatusCode();

            // workaround for Alexa Thumbnails paid web service, which doesn't bother to return a content-type line
            Header ctHeader = method.getResponseHeader("Content-Type");
            String contentType = ctHeader == null || ctHeader.getValue() == null ? DEFAULT_CTYPE : ctHeader.getValue();

            InputStream targetResponseBody = method.getResponseBodyAsStream();
            
            resp.setStatus(status);
            resp.setContentType(contentType);
            //no caching!   
            resp.setHeader("Expires", "Tue, 24 Jan 2000 17:46:50 GMT");
            // Set standard HTTP/1.1 no-cache headers.
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            // Set standard HTTP/1.0 no-cache header.
            resp.setHeader("Pragma", "no-cache");
            
            for (Header h : method.getResponseHeaders()) {
                if (canProxyHeader(h.getName())) {
                	resp.addHeader(h.getName(), h.getValue());
                }
            }
            if (targetResponseBody != null) {
            	com.zimbra.cs.appliance.util.ByteUtil.copy(targetResponseBody, true, resp.getOutputStream(), true);
            }
        } finally {
            if (method != null)
                method.releaseConnection();
        }
    }
}
