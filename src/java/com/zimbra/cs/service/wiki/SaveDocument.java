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
package com.zimbra.cs.service.wiki;

import java.io.IOException;
import java.util.Map;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraContext;

public class SaveDocument extends WikiDocumentHandler {

	private static class Doc {
		public byte[] contents;
		public String name;
		public String contentType;
	}
	
	protected Doc getDocumentDataFromMessage(OperationContext octxt, Mailbox mbox, String msgid, String part) throws ServiceException {
		Message item = mbox.getMessageById(octxt, Integer.parseInt(msgid));
        MimeMessage mm = item.getMimeMessage();
		Doc doc = new Doc();
		try {
	        MimePart mp = Mime.getMimePart(mm, part);
			doc.contents = ByteUtil.getContent(mp.getInputStream(), 0);
			doc.name = mp.getFileName();
			doc.contentType = mp.getContentType();
		} catch (Exception e) {
			throw ServiceException.FAILURE("cannot get part "+part+" from message "+msgid, e);
		}
		
		return doc;
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
    public boolean isReadOnly() {
        return false;
    }

	@Override
	public Element handle(Element request, Map context)
			throws ServiceException, SoapFaultException {
        ZimbraContext lc = getZimbraContext(context);
        Wiki wiki = getRequestedWiki(request, lc);

        Element docElem = request.getElement(MailService.E_DOC);
        Mailbox mbox = Mailbox.getMailboxByAccountId(wiki.getWikiAccountId());
        OperationContext octxt = lc.getOperationContext();

        Doc doc;
        Element attElem = docElem.getOptionalElement(MailService.E_UPLOAD);;
        if (attElem != null) {
            String aid = attElem.getAttribute(MailService.A_ID, null);
            doc = getDocumentDataFromAttachment(lc, aid);
        } else {
        	attElem = docElem.getElement(MailService.E_MSG);
            String msgid = attElem.getAttribute(MailService.A_ID, null);
            String part = attElem.getAttribute(MailService.A_PART, null);
        	doc = getDocumentDataFromMessage(octxt, mbox, msgid, part);
        }

        
        String name = docElem.getAttribute(MailService.A_NAME, doc.name);
        String ctype = docElem.getAttribute(MailService.A_CONTENT_TYPE, doc.contentType);
        int fid = (int)docElem.getAttributeLong(MailService.A_FOLDER, wiki.getWikiFolderId());

        Document docItem;
   		docItem = mbox.createDocument(octxt, fid, name, ctype, doc.contents, null);
        //wiki.addWiki(docItem);  // XXX need to keep track of documents.

        Element response = lc.createElement(MailService.SAVE_DOCUMENT_RESPONSE);
        Element m = response.addElement(MailService.E_MSG);
        m.addAttribute(MailService.A_ID, lc.formatItemId(docItem));
        return response;
	}
}
