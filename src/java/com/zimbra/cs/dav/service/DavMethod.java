/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.resource.DavResource;

public abstract class DavMethod {
	public abstract String getName();
	public abstract void handle(DavContext ctxt) throws DavException, IOException;
	
	// needs to throw either 403 Forbidden, or 409 Conflict in case of an error.
	public void checkPrecondition(DavContext ctxt) throws DavException {
	}
	
	public void checkPostcondition(DavContext ctxt) throws DavException {
	}
	
	protected static Map<Integer, String> sStatusTextMap;
	
	static {
		sStatusTextMap = new HashMap<Integer, String>();
		
		sStatusTextMap.put(HttpServletResponse.SC_CONTINUE,            "HTTP/1.1 100 Continue");
		sStatusTextMap.put(HttpServletResponse.SC_SWITCHING_PROTOCOLS, "HTTP/1.1 101 Switching Protocols");
		
		sStatusTextMap.put(HttpServletResponse.SC_OK,                            "HTTP/1.1 200 OK");
		sStatusTextMap.put(HttpServletResponse.SC_CREATED,                       "HTTP/1.1 201 Created");
		sStatusTextMap.put(HttpServletResponse.SC_ACCEPTED,                      "HTTP/1.1 202 Accepted");
		sStatusTextMap.put(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION, "HTTP/1.1 203 Non-Authoritative Information");
		sStatusTextMap.put(HttpServletResponse.SC_NO_CONTENT,                    "HTTP/1.1 204 No Content");
		sStatusTextMap.put(HttpServletResponse.SC_RESET_CONTENT,                 "HTTP/1.1 205 Reset Content");
		sStatusTextMap.put(HttpServletResponse.SC_PARTIAL_CONTENT,               "HTTP/1.1 206 Partial Content");
		
		sStatusTextMap.put(HttpServletResponse.SC_MULTIPLE_CHOICES,   "HTTP/1.1 300 Multiple Choices");
		sStatusTextMap.put(HttpServletResponse.SC_MOVED_PERMANENTLY,  "HTTP/1.1 301 Moved Permanently");
		sStatusTextMap.put(HttpServletResponse.SC_FOUND,              "HTTP/1.1 302 Found");
		sStatusTextMap.put(HttpServletResponse.SC_SEE_OTHER,          "HTTP/1.1 303 See Other");
		sStatusTextMap.put(HttpServletResponse.SC_NOT_MODIFIED,       "HTTP/1.1 304 Not Modified");
		sStatusTextMap.put(HttpServletResponse.SC_USE_PROXY,          "HTTP/1.1 305 Use Proxy");
		sStatusTextMap.put(HttpServletResponse.SC_TEMPORARY_REDIRECT, "HTTP/1.1 307 Temporary Redirect");
		
		sStatusTextMap.put(HttpServletResponse.SC_BAD_REQUEST,                     "HTTP/1.1 400 Bad Request");
		sStatusTextMap.put(HttpServletResponse.SC_UNAUTHORIZED,                    "HTTP/1.1 401 Unauthorized");
		sStatusTextMap.put(HttpServletResponse.SC_PAYMENT_REQUIRED,                "HTTP/1.1 402 Payment Required");
		sStatusTextMap.put(HttpServletResponse.SC_FORBIDDEN,                       "HTTP/1.1 403 Forbidden");
		sStatusTextMap.put(HttpServletResponse.SC_NOT_FOUND,                       "HTTP/1.1 404 Not Found");
		sStatusTextMap.put(HttpServletResponse.SC_METHOD_NOT_ALLOWED,              "HTTP/1.1 405 Method Not Allowed");
		sStatusTextMap.put(HttpServletResponse.SC_NOT_ACCEPTABLE,                  "HTTP/1.1 406 Not Acceptable");
		sStatusTextMap.put(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED,   "HTTP/1.1 407 Proxy Authentication Required");
		sStatusTextMap.put(HttpServletResponse.SC_REQUEST_TIMEOUT,                 "HTTP/1.1 408 Request Time-out");
		sStatusTextMap.put(HttpServletResponse.SC_CONFLICT,                        "HTTP/1.1 409 Conflict");
		sStatusTextMap.put(HttpServletResponse.SC_GONE,                            "HTTP/1.1 410 Gone");
		sStatusTextMap.put(HttpServletResponse.SC_LENGTH_REQUIRED,                 "HTTP/1.1 411 Length Required");
		sStatusTextMap.put(HttpServletResponse.SC_PRECONDITION_FAILED,             "HTTP/1.1 412 Precondition Failed");
		sStatusTextMap.put(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,        "HTTP/1.1 413 Reques Entity Too Large");
		sStatusTextMap.put(HttpServletResponse.SC_REQUEST_URI_TOO_LONG,            "HTTP/1.1 414 Request-URI Too Large");
		sStatusTextMap.put(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,          "HTTP/1.1 415 Unsupported Media Type");
		sStatusTextMap.put(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, "HTTP/1.1 416 Requested range not satisfiable");
		sStatusTextMap.put(HttpServletResponse.SC_EXPECTATION_FAILED,              "HTTP/1.1 417 Expectation Failed");
		
		sStatusTextMap.put(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,      "HTTP/1.1 500 Internal Server Error");
		sStatusTextMap.put(HttpServletResponse.SC_NOT_IMPLEMENTED,            "HTTP/1.1 501 Not Implemented");
		sStatusTextMap.put(HttpServletResponse.SC_BAD_GATEWAY,                "HTTP/1.1 502 Bad Gateway");
		sStatusTextMap.put(HttpServletResponse.SC_SERVICE_UNAVAILABLE,        "HTTP/1.1 503 Service Unavailable");
		sStatusTextMap.put(HttpServletResponse.SC_GATEWAY_TIMEOUT,            "HTTP/1.1 504 Gateway Time-out");
		sStatusTextMap.put(HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED, "HTTP/1.1 505 HTTP Version not supported");
		
		// dav extensions
		sStatusTextMap.put(DavProtocol.STATUS_PROCESSING,           "HTTP/1.1 102 Processing");
		sStatusTextMap.put(DavProtocol.STATUS_MULTI_STATUS,         "HTTP/1.1 207 Multi-Status");
		sStatusTextMap.put(DavProtocol.STATUS_UNPROCESSABLE_ENTITY, "HTTP/1.1 422 Unprocessable Entity");
		sStatusTextMap.put(DavProtocol.STATUS_LOCKED,               "HTTP/1.1 423 Locked");
		sStatusTextMap.put(DavProtocol.STATUS_FAILED_DEPENDENCY,    "HTTP/1.1 424 Failed Dependency");
		sStatusTextMap.put(DavProtocol.STATUS_INSUFFICIENT_STORAGE, "HTTP/1.1 507 Insufficient Storage");
	}
	
	protected static final int STATUS_OK = HttpServletResponse.SC_OK;
	
	protected void addStatusElement(Element resp, int code) {
		resp.addElement(DavElements.E_STATUS).setText(sStatusTextMap.get(code));
	}
	
	protected void addComplianceHeader(DavContext ctxt, DavResource rs) throws IOException {
		HttpServletResponse resp = ctxt.getResponse();
		String comp = DavProtocol.getComplianceString(rs.getComplianceList());
		if (comp != null)
			resp.setHeader(DavProtocol.HEADER_DAV, comp);
	}
	
	protected void sendResponse(DavContext ctxt, Document outMsg) throws IOException {
		HttpServletResponse resp = ctxt.getResponse();
		resp.setStatus(ctxt.getStatus());
		if (outMsg != null) {
			resp.setContentType(DavProtocol.DAV_CONTENT_TYPE);
			OutputFormat format = OutputFormat.createPrettyPrint();
			XMLWriter writer = new XMLWriter(resp.getOutputStream(), format);
			writer.write(outMsg);
		}
	}
}
