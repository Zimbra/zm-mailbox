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
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.appliance.httpclient.HttpProxyUtil;
import com.zimbra.cs.servlet.ZimbraServlet;

/**
 * @author pjoseph
 */
@SuppressWarnings("serial")
public class ApplianceProxyServlet extends ZimbraServlet {
    private static final String TARGET_PARAM = "target";
    private static final String DEFAULT_CTYPE = "text/xml";
   
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doProxy(req, resp);
    }

    private boolean canProxyHeader(String header) {
        if (header == null) return false;
        header = header.toLowerCase();
        if (header.startsWith("accept") ||
            header.equalsIgnoreCase("content-length") ||
            header.equalsIgnoreCase("connection") ||
            header.equalsIgnoreCase("keep-alive") ||
            header.equalsIgnoreCase("pragma") ||
            header.equalsIgnoreCase("host") ||
            header.equalsIgnoreCase("Cache-Control") ||
            header.equalsIgnoreCase("Expires") ||
            header.equalsIgnoreCase("cookie") ||
            header.equalsIgnoreCase("transfer-encoding")) {
            return false;
        }
        return true;
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doProxy(req, resp);
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
           ByteUtil.closeStream(baos);
        }
    }
    
    private void doProxy(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ZimbraLog.clearContext();
        boolean isAdmin = super.isRequestOnAllowedPort(req);
        if (!isAdmin) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        AuthToken authToken =  getAdminAuthTokenFromCookie(req, resp);      
        if (authToken == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        try {
			Account adminAcct = Provisioning.getInstance().get(AccountBy.id, authToken.getAccountId(),authToken);
			if(adminAcct == null) {
				resp.sendError(HttpServletResponse.SC_FORBIDDEN);
			}
		} catch (ServiceException e) {
			ZimbraLog.extensions.info("exception while proxying ", e);
			resp.sendError(HttpServletResponse.SC_FORBIDDEN);
		}
		
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
            HttpProxyUtil.configureProxy(client,url.toString());
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
            
            /** Authenticate to VAMI **/
            String cimuser = LC.zimbra_vami_user.value();
            String cimpass = LC.zimbra_vami_password.value();
    		method.addRequestHeader("cimuser", cimuser);
    		method.addRequestHeader("cimpass", cimpass);
    		Cookie cookie = new Cookie("127.0.0.1","ZCA_VAMI_AUTH",cimuser+":"+cimpass,"/",null,false);
    		client.getState().addCookie(cookie);
            client.getParams().setAuthenticationPreemptive(true);
            AuthScope authScope = new AuthScope(url.getHost(),url.getPort());
            client.getState().setCredentials(authScope, new UsernamePasswordCredentials(cimuser, cimpass));
            
            try {
                client.executeMethod(method);
            } catch (HttpException ex) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            int status = method.getStatusLine() == null ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : method.getStatusCode();

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
            	ByteUtil.copy(targetResponseBody, true, resp.getOutputStream(), true);
            }
        } finally {
            if (method != null)
                method.releaseConnection();
        }
    }
}
