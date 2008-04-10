/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.auth;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthContext;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;

public class HostedAuth extends ZimbraCustomAuth {
	//public static String HEADER_AUTH_METHOD = "Auth-Method";
	public static String HEADER_AUTH_USER = "Auth-User";
	public static String HEADER_AUTH_PASSWORD = "Auth-Pass";
	//public static String HEADER_AUTH_PROTOCOL = "Auth-Protocol";
	//public static String HEADER_AUTH_LOGIN_ATTEMPT = "Auth-Login-Attempt";
	public static String HEADER_CLIENT_IP = "Client-IP";
	public static String HEADER_AUTH_STATUS = "Auth-Status";
	//public static String HEADER_AUTH_SERVER = "Auth-Server";
	//public static String HEADER_AUTH_PORT = "Auth-Port";
	
	public static String AUTH_STATUS_OK = "OK";
	
	/**
	 *  This custom auth module takes arguments in the following form:
	 * {URL} [GET|POST - default is GET] [encryption method - defautl is plain] [auth protocol - default is imap] 
	 * e.g.: http://auth.customer.com:80 GET
	 **/
	public void authenticate(Account acct, String password,
			Map<String, Object> context, List<String> args) throws Exception {
		HttpClient client = new HttpClient();
		HttpMethod method = null;
		
		/*String authMethod = "plain";
		String authProtocol = "imap";*/
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
				method = new GetMethod(targetURL);
			else
				method = new PostMethod(targetURL);
		} else
			method = new GetMethod(targetURL);
		
		method.addRequestHeader(HEADER_CLIENT_IP,context.get(AuthContext.AC_ORIGINATING_CLIENT_IP).toString());
		method.addRequestHeader(HEADER_AUTH_USER,acct.getName());
		method.addRequestHeader(HEADER_AUTH_PASSWORD,password);
		//method.addRequestHeader(HEADER_AUTH_PROTOCOL,authProtocol);
        try {
            client.executeMethod(method);
        } catch (HttpException ex) {
        	throw AuthFailedServiceException.AUTH_FAILED(acct.getName(),acct.getName(), "HTTP request to remote authentication server failed",ex); 
        } catch (IOException ex) {
        	throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), acct.getName(), "HTTP request to remote authentication server failed",ex); 
        }
        int status = method.getStatusCode(); 
        if(status != HttpStatus.SC_OK) {
        	throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), "HTTP request to remote authentication server failed. Remote response code: " + Integer.toString(status));
        }
        
        String responseMessage;
        if(method.getResponseHeader(HEADER_AUTH_STATUS) != null) {
        	responseMessage = method.getResponseHeader(HEADER_AUTH_STATUS).getValue();
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
