/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.MailItem;

public abstract class PhantomResource extends DavResource {

	protected static final String BY_DATE = "by-date";
	protected static final String BY_TYPE = "by-type";
	protected static final String BY_SENDER = "by-sender";
	
	protected static final String TODAY = "today";
	protected static final String WEEK  = "last-week";
	protected static final String MONTH = "last-month";
	protected static final String YEAR  = "last-year";
	protected static final String ALL   = "all";

	protected List<String> mTokens;
	protected long mNow;
	
	protected static final byte[] SEARCH_TYPES = new byte[] { MailItem.TYPE_MESSAGE };
    
	PhantomResource(String uri, String owner, List<String> tokens) {
		super(uri, owner);
		mTokens = tokens;
		mNow = System.currentTimeMillis();
		setCreationDate(mNow);
		setLastModifiedDate(mNow);
		setProperty(DavElements.P_GETCONTENTLENGTH, "0");
		setProperty(DavElements.P_DISPLAYNAME, tokens.get(tokens.size()-1));
	}
	
	protected static List<String> parseUri(String uri) {
		ArrayList<String> l = new ArrayList<String>();
		StringTokenizer tok = new StringTokenizer(uri, "/");
		while (tok.hasMoreTokens())
			l.add(tok.nextToken());
		return l;
	}
	
	public void delete(DavContext ctxt) throws DavException {
		throw new DavException("cannot delete this resource", HttpServletResponse.SC_FORBIDDEN, null);
	}
}
