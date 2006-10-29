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

import java.io.IOException;
import java.io.InputStream;

import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.service.ServiceException;

public class Notebook extends DavResource {

	private InputStream mContent;

	public Notebook(Document doc) throws ServiceException {
		super(doc.getPath(), doc.getAccount());
		setCreationDate(doc.getDate());
		setLastModifiedDate(doc.getChangeDate());
		setProperty(DavElements.P_DISPLAYNAME, doc.getSubject());
		setProperty(DavElements.P_GETCONTENTLENGTH, Integer.toString(doc.getSize()));
		setProperty(DavElements.P_GETCONTENTTYPE, doc.getContentType());
		try {
			mContent = doc.getRawDocument();
		} catch (Exception e) {
			mContent = null;
			setProperty(DavElements.P_GETCONTENTLENGTH, Integer.toString(0));
		}
	}

	@Override
	public InputStream getContent() throws IOException, DavException {
		return mContent;
	}

	@Override
	public boolean isCollection() {
		return false;
	}
}
