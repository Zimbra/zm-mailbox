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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.WriteOpDocumentHandler;
import com.zimbra.soap.ZimbraContext;

public class SaveDocument extends WriteOpDocumentHandler {

	private static class Doc {
		public byte[] contents;
		public String name;
		public String contentType;
	}
	
	protected Doc getDocumentDataFromAttachment(ZimbraContext lc, String aid) throws ServiceException {
        Upload up = FileUploadServlet.fetchUpload(lc.getAuthtokenAccountId(), aid, lc.getRawAuthToken());

        Doc doc = new Doc();
        try {
        	doc.contents = ByteUtil.getContent(up.getInputStream(), 0);
        	doc.name = up.getName();
        	doc.contentType = up.getContentType();
        } catch (IOException ioe) {
        	throw ServiceException.FAILURE("cannot get uploaded file", ioe);
        } finally {
            if (up != null)
                FileUploadServlet.deleteUpload(up);
        }
        return doc;
	}
	
	@Override
	public Element handle(Element request, Map context)
			throws ServiceException, SoapFaultException {
        ZimbraContext lc = getZimbraContext(context);
        Wiki wiki = Wiki.getInstance();
        Mailbox mbox = Mailbox.getMailboxByAccountId(wiki.getWikiAccountId());
        OperationContext octxt = lc.getOperationContext();

        Element docElem = request.getElement(MailService.E_DOC);

        Element attElem = docElem.getElement(MailService.E_UPLOAD);
        String aid = attElem.getAttribute(MailService.A_ID, null);

        Doc doc = getDocumentDataFromAttachment(lc, aid);
        
        String name = docElem.getAttribute(MailService.A_NAME, doc.name);
        String ctype = docElem.getAttribute(MailService.A_CONTENT_TYPE, doc.contentType);
        int fid = (int)docElem.getAttributeLong(MailService.A_FOLDERID, wiki.getWikiFolderId());

        Document docItem;
   		docItem = mbox.createDocument(octxt, fid, name, ctype, doc.contents, null);
        //wiki.addWiki(docItem);  // XXX need to keep track of documents.

        Element response = lc.createElement(MailService.SAVE_DOCUMENT_RESPONSE);
        Element m = response.addElement(MailService.E_MSG);
        m.addAttribute(MailService.A_ID, lc.formatItemId(docItem));
        return response;
	}
}
