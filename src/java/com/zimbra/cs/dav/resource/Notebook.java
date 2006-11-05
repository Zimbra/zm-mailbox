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
package com.zimbra.cs.dav.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.Wiki.WikiContext;

public class Notebook extends MailItemResource {

	private Document mDoc;
	private WikiContext mWctxt;

	public Notebook(DavContext ctxt, Document doc) throws ServiceException {
		super(doc);
		mDoc = doc;
		mWctxt = new WikiContext(ctxt.getOperationContext(), null);
		setCreationDate(doc.getDate());
		setLastModifiedDate(doc.getChangeDate());
		setProperty(DavElements.P_DISPLAYNAME, doc.getSubject());
		// content length is just an estimate.  the actual content will be larger
		// after chrome composition.
		setProperty(DavElements.P_GETCONTENTLENGTH, Integer.toString(doc.getSize()));
		setProperty(DavElements.P_GETCONTENTTYPE, doc.getContentType());
	}

	@Override
	public InputStream getContent() throws IOException, DavException {
		try {
			if (mDoc.getType() == MailItem.TYPE_DOCUMENT)
				return mDoc.getRawDocument();
			Wiki wiki = Wiki.getInstance(mWctxt, mDoc.getAccount().getId(), mDoc.getFolderId());
			String val = wiki.getTemplate(mWctxt, mDoc.getName()).getComposedPage(mWctxt, mDoc, "_Template");
			return new ByteArrayInputStream(val.getBytes());
		} catch (ServiceException se) {
			throw new DavException("cannot get contents", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se);
		}
	}

	@Override
	public boolean isCollection() {
		return false;
	}
}
