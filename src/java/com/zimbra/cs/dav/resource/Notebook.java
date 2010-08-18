/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.dav.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.wiki.WikiPage;

/**
 * Represents Notebook / Wiki item.
 * 
 * @author jylee
 *
 */
public class Notebook extends MailItemResource {

	private Document mDoc;
	private WikiPage.WikiContext mWctxt;

	public Notebook(DavContext ctxt, Document doc) throws ServiceException {
		super(ctxt, doc);
		mDoc = doc;
		mWctxt = new WikiPage.WikiContext(ctxt.getOperationContext(), null);
		setCreationDate(doc.getDate());
		setLastModifiedDate(doc.getChangeDate());
		setProperty(DavElements.P_DISPLAYNAME, doc.getName());
		// content length is just an estimate.  the actual content will be larger
		// after chrome composition.
		setProperty(DavElements.P_GETCONTENTLENGTH, Long.toString(doc.getSize()));
		setProperty(DavElements.P_GETCONTENTTYPE, doc.getContentType());
	}

	@Override
	public InputStream getContent(DavContext ctxt) throws IOException, DavException {
		try {
			if (mDoc.getType() == MailItem.TYPE_DOCUMENT)
				return mDoc.getContentStream();
			WikiPage page = WikiPage.create(mDoc);
			String val = page.getTemplate(mWctxt).getComposedPage(mWctxt, mDoc, "_Template");
			StringBuilder buf = new StringBuilder();
			buf.append("<html><head>");
			buf.append("<title>").append(mDoc.getName()).append("</title>");
			buf.append("<link rel='stylesheet' type='text/css' href='/zimbra/css/wiki.css'>");
			buf.append("</head><body style='margin:0px'>").append(val).append("</body></html>");
			return new ByteArrayInputStream(buf.toString().getBytes("UTF-8"));
		} catch (ServiceException se) {
			throw new DavException("cannot get contents", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se);
		}
	}

	@Override
	public boolean isCollection() {
		return false;
	}
}
