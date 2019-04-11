/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2012, 2013, 2014, 2016, 2018 Synacor, Inc.
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
package com.zimbra.cs.account.auth;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;

public class HostedAuth extends ZimbraCustomAuth {
	//public static String HEADER_AUTH_METHOD = "Auth-Method";
	public static String HEADER_AUTH_USER = "Auth-User";
	public static String HEADER_AUTH_PASSWORD = "Auth-Pass";
	public static String HEADER_AUTH_PROTOCOL = "Auth-Protocol";
	//public static String HEADER_AUTH_LOGIN_ATTEMPT = "Auth-Login-Attempt";
	public static String HEADER_CLIENT_IP = "Client-IP";
	public static String HEADER_AUTH_STATUS = "Auth-Status";
	//public static String HEADER_AUTH_SERVER = "Auth-Server";
	//public static String HEADER_AUTH_PORT = "Auth-Port";
	public static String HEADER_AUTH_USER_AGENT = "Auth-User-Agent";
	public static String HEADER_X_ZIMBRA_REMOTE_ADDR = "X-ZIMBRA-REMOTE-ADDR";
	
	public static String AUTH_STATUS_OK = "OK";
	
	/**
	 * zmprov md test.com zimbraAuthMech 'custom:hosted http://auth.customer.com:80'
	 * 
	 *  This custom auth module takes arguments in the following form:
	 * {URL} [GET|POST - default is GET] [encryption method - defautl is plain] [auth protocol - default is imap] 
	 * e.g.: http://auth.customer.com:80 GET
	 **/
	public void authenticate(Account acct, String password,
			Map<String, Object> context, List<String> args) throws Exception {
		HttpClient client = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient().build();
		HttpRequestBase method = null;
		
		String targetURL = args.get(0);
		/*
		if (args.size()>2) {
			authMethod = args.get(2);
		}
		
		if (args.size()>3) {
			authMethod = args.get(3);
		}*/
		
		if(args.size()>1) {
			if(args.get(1).equalsIgnoreCase("GET"))
				method = new HttpGet(targetURL);
			else
				method = new HttpPost(targetURL);
		} else
			method = new HttpGet(targetURL);
		
		if(context.get(AuthContext.AC_ORIGINATING_CLIENT_IP)!=null)
			method.addHeader(HEADER_CLIENT_IP,context.get(AuthContext.AC_ORIGINATING_CLIENT_IP).toString());
		
	    if(context.get(AuthContext.AC_REMOTE_IP) != null)
	        method.addHeader(HEADER_X_ZIMBRA_REMOTE_ADDR,context.get(AuthContext.AC_REMOTE_IP).toString());
		
		method.addHeader(HEADER_AUTH_USER,acct.getName());
		method.addHeader(HEADER_AUTH_PASSWORD,password);
		
		AuthContext.Protocol proto =
		    (AuthContext.Protocol)context.get(AuthContext.AC_PROTOCOL);
		if (proto != null)
		    method.addHeader(HEADER_AUTH_PROTOCOL,proto.toString());
		
		if (context.get(AuthContext.AC_USER_AGENT)!=null)
            method.addHeader(HEADER_AUTH_USER_AGENT, context.get(AuthContext.AC_USER_AGENT).toString());
		
		HttpResponse response;
        try {
            response = HttpClientUtil.executeMethod(client, method);
        } catch (HttpException ex) {
        	throw AuthFailedServiceException.AUTH_FAILED(acct.getName(),acct.getName(), "HTTP request to remote authentication server failed",ex); 
        } catch (IOException ex) {
        	throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), acct.getName(), "HTTP request to remote authentication server failed",ex); 
        } finally {
            if (method != null)
                method.releaseConnection();    
        }
        
        int status = response.getStatusLine().getStatusCode();
        if(status != HttpStatus.SC_OK) {
        	throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), "HTTP request to remote authentication server failed. Remote response code: " + Integer.toString(status));
        }
        
        String responseMessage;
        if(response.getFirstHeader(HEADER_AUTH_STATUS) != null) {
        	responseMessage = response.getFirstHeader(HEADER_AUTH_STATUS).getValue();
        } else {
        	throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), "Empty response from remote authentication server.");
        }
        if(responseMessage.equalsIgnoreCase(AUTH_STATUS_OK)) {
        	return;
        } else {
        	throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), responseMessage);
        }
        
	}

}
