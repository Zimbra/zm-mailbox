/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.dav;

import java.io.IOException;
import java.net.URLConnection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavResponse;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.FileUploadServlet;

/**
 * 
 * @author jylee
 *
 */
public class DavContext {
	private HttpServletRequest  mReq;
	private HttpServletResponse mResp;
	private OperationContext mOpCtxt;
	private Account mAuthAccount;
	private String mUri;
	private String mUser;
	private String mPath;
	private int mStatus;
	private Document mRequestMsg;
	private FileUploadServlet.Upload mUpload;
	private DavResponse mResponse;
	private boolean mResponseSent;
	private DavResource mRequestedResource;
    private RequestType mRequestType;
	
    private enum RequestType { PRINCIPAL, RESOURCE };
    
	public DavContext(HttpServletRequest req, HttpServletResponse resp, Account authUser) {
		mReq = req;  mResp = resp;
		mUri = req.getPathInfo();
		if (mUri != null && mUri.length() > 1) {
		    int index = mUri.indexOf('/', 1);
            if (index > 0) {
                String reqType = mUri.substring(1, index);
                if (reqType.equals("home"))
                    mRequestType = RequestType.RESOURCE;
                else
                    mRequestType = RequestType.PRINCIPAL;
                int start = index+1;
                index = mUri.indexOf('/', index+1);
                if (index != -1) {
                    mUser = mUri.substring(start, index);
                    mPath = mUri.substring(index);
                } else {
                    mUser = mUri.substring(start);
                    mPath = "/";
                }
            }
		}
		mStatus = HttpServletResponse.SC_OK;
		mAuthAccount = authUser;
		mOpCtxt = new Mailbox.OperationContext(authUser);
	}
	
	/* Returns HttpServletRequest object containing the current DAV request. */
	public HttpServletRequest getRequest() {
		return mReq;
	}
	
	/* Returns HttpServletResponse object used to return DAV response. */
	public HttpServletResponse getResponse() {
		return mResp;
	}

	/* Returns OperationContext used to access Mailbox. */
	public OperationContext getOperationContext() {
		return mOpCtxt;
	}

	/* Returns the authenticated account used to make the current request. */
	public Account getAuthAccount() {
		return mAuthAccount;
	}

	/* Convenience methods used to parse URL to map to DAV resources.
	 * 
	 * Request:
	 * 
	 * http://server:port/service/dav/user1/Notebook/pic1.jpg
	 * 
	 * getUri()  -> /user1/Notebook/pic1.jpg
	 * getUser() -> user1
	 * getPath() -> /Notebook/pic1.jpg
	 * getItem() -> pic1.jpg
	 * 
	 */
	public String getUri() {
		return mUri;
	}

	public String getUser() {
		return mUser;
	}
	
	public String getPath() {
		return mPath;
	}

	public String getItem() {
		if (mPath != null) {
			if (mPath.equals("/"))
				return mPath;
			int index;
			if (mPath.endsWith("/")) {
				int length = mPath.length();
				index = mPath.lastIndexOf('/', length-2);
				if (index != -1)
					return mPath.substring(index+1, length-1);
			} else {
				index = mPath.lastIndexOf('/');
				if (index != -1)
					return mPath.substring(index+1);
			}
		}
		return null;
	}

	/* Status is HTTP response code that is set by DAV methods in case of
	 * exceptional conditions.
	 */
	public int getStatus() {
		return mStatus;
	}
	
	public void setStatus(int s) {
		mStatus = s;
	}

	/* HttpServletResponse body can be written directly by DAV method handlers,
	 * in which case DAV method would tell the framework that the response
	 * has been already sent.
	 */
	public void responseSent() {
		mResponseSent = true;
	}
	
	public boolean isResponseSent() {
		return mResponseSent;
	}
	
	/* Depth header - RFC 2518bis section 10.2 */
	public enum Depth {
		zero, one, infinity
	}
	
	public Depth getDepth() {
		String hdr = mReq.getHeader(DavProtocol.HEADER_DEPTH);
		if (hdr == null)
			return Depth.zero;
		if (hdr.equals("0"))
			return Depth.zero;
		if (hdr.equals("1"))
			return Depth.one;
		if (hdr.equalsIgnoreCase("infinity"))
			return Depth.infinity;
		
		ZimbraLog.dav.info("invalid depth: "+hdr);
		return Depth.zero;
	}

	/* Returns true if the DAV request contains a message. */
	public boolean hasRequestMessage() {
		String hdr = mReq.getHeader(DavProtocol.HEADER_CONTENT_LENGTH);
		return (mRequestMsg != null || 
				hdr != null && Integer.parseInt(hdr) > 0);
	}
	
	public FileUploadServlet.Upload getUpload() throws DavException, IOException {
		if (mUpload == null) {
			String name = getItem();
			String ctype = getRequest().getContentType();
			if (ctype == null)
				ctype = URLConnection.getFileNameMap().getContentTypeFor(name);
			if (ctype == null)
				ctype = DavProtocol.DEFAULT_CONTENT_TYPE;
			try {
				mUpload = FileUploadServlet.saveUpload(mReq.getInputStream(), name, ctype, mAuthAccount.getId());
			} catch (ServiceException se) {
				throw new DavException("can't save upload", se);
			}
		}
		return mUpload;
	}
	
	public void cleanup() {
		if (mUpload != null)
			FileUploadServlet.deleteUpload(mUpload);
		mUpload = null;
	}
	
	/* Returns XML Document containing the request. */
	public Document getRequestMessage() throws DavException {
		if (mRequestMsg != null)
			return mRequestMsg;
		try {
			if (hasRequestMessage()) {
				mRequestMsg = new SAXReader().read(getUpload().getInputStream());
				return mRequestMsg;
			}
		} catch (DocumentException e) {
			throw new DavException("unable to parse request message", HttpServletResponse.SC_BAD_REQUEST, e);
		} catch (IOException e) {
			throw new DavException("can't read uploaded file", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
		throw new DavException("no request msg", HttpServletResponse.SC_BAD_REQUEST, null);
	}

	/* Returns true if there is a response message generated by DAV method handler. */
	public boolean hasResponseMessage() {
		return mResponse != null;
	}
	
	/* Returns DavResponse */
	public DavResponse getDavResponse() {
		if (mResponse == null)
			mResponse = new DavResponse();
		return mResponse;
	}
	
	public DavResource getRequestedResource() throws DavException, ServiceException {
		if (mRequestedResource == null) {
            if (mRequestType == RequestType.RESOURCE)
                mRequestedResource = UrlNamespace.getResourceAt(this, mUser, mPath);
            else
                mRequestedResource = UrlNamespace.getPrincipalAtUrl(this, mUri);
			if (mRequestedResource != null)
				ZimbraLog.addToContext(ZimbraLog.C_NAME, mRequestedResource.getOwner());
		}
		return mRequestedResource;
	}
	
	private static final String EVOLUTION = "Evolution";
	private static final String ICAL = "DAVKit";
	
	private boolean userAgentHeaderContains(String str) {
		String userAgent = mReq.getHeader(DavProtocol.HEADER_USER_AGENT);
		if (userAgent == null)
			return false;
		return userAgent.indexOf(str) >= 0;
	}
	
	public boolean isEvolutionClient() {
		return userAgentHeaderContains(EVOLUTION);
	}
	
	public boolean isIcalClient() {
		return userAgentHeaderContains(ICAL);
	}
}
