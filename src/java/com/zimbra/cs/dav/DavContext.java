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
package com.zimbra.cs.dav;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import com.zimbra.cs.account.Account;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;

public class DavContext {
	private HttpServletRequest  mReq;
	private HttpServletResponse mResp;
	private OperationContext mOpCtxt;
	private Account mAuthAccount;
	private String mUri;
	private String mUser;
	private String mPath;
	private int mStatus;
	private Document mResponse;
	private boolean mResponseSent;
	
	public DavContext(HttpServletRequest req, HttpServletResponse resp) {
		mReq = req;  mResp = resp;
		mUri = req.getPathInfo().toLowerCase();
		int index = mUri.indexOf('/', 1);
		if (index != -1) {
			mUser = mUri.substring(1, index);
			mPath = mUri.substring(index);
		}
		mStatus = HttpServletResponse.SC_OK;
	}
	
	public HttpServletRequest getRequest() {
		return mReq;
	}
	
	public HttpServletResponse getResponse() {
		return mResp;
	}

	public void setOperationContext(Account authUser) {
		mAuthAccount = authUser;
		mOpCtxt = new Mailbox.OperationContext(authUser);
	}
	
	public OperationContext getOperationContext() {
		return mOpCtxt;
	}

	public Account getAuthAccount() {
		return mAuthAccount;
	}
	
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
			int index;
			if (mPath.endsWith("/")) {
				int length = mPath.length();
				index = mPath.lastIndexOf('/', length-1);
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
	
	public int getStatus() {
		return mStatus;
	}
	
	public void setStatus(int s) {
		mStatus = s;
	}
	
	public void responseSent() {
		mResponseSent = true;
	}
	
	public boolean isResponseSent() {
		return mResponseSent;
	}
	
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
	
	public boolean hasRequestMessage() {
		String hdr = mReq.getHeader(DavProtocol.HEADER_CONTENT_LENGTH);
		return (hdr != null && Integer.parseInt(hdr) > 0);
	}
	
	public Document getRequestMessage() throws DavException {
		try {
			if (hasRequestMessage()) {
				Document doc = new SAXReader().read(mReq.getInputStream());
				return doc;
			}
		} catch (DocumentException e) {
			throw new DavException("unable to parse request message", HttpServletResponse.SC_BAD_REQUEST, e);
		} catch (IOException e) {
			throw new DavException("unable to read input", HttpServletResponse.SC_BAD_REQUEST, e);
		}
		throw new DavException("no request msg", HttpServletResponse.SC_BAD_REQUEST, null);
	}
	
	public Document getResponseMessage() {
		if (mResponse == null)
			mResponse = org.dom4j.DocumentHelper.createDocument();
		return mResponse;
	}
}
