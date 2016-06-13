/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.dav.resource;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.Document;

/**
 * Represents Notebook / Wiki item.
 * 
 * @author jylee
 *
 */
public class Notebook extends MailItemResource {

	private Document mDoc;

	public Notebook(DavContext ctxt, Document doc) throws ServiceException {
		super(ctxt, doc);
		mDoc = doc;
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
            return mDoc.getContentStream();
		} catch (ServiceException se) {
			throw new DavException("cannot get contents", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se);
		}
	}

	@Override
	public boolean isCollection() {
		return false;
	}
}
