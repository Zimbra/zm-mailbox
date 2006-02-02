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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.WriteOpDocumentHandler;
import com.zimbra.soap.ZimbraContext;

public class SaveWiki extends WriteOpDocumentHandler {

	@Override
	public Element handle(Element request, Map context)
			throws ServiceException, SoapFaultException {
		Wiki wiki = Wiki.getInstance();
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = Mailbox.getMailboxByAccountId(wiki.getWikiAccountId());
        OperationContext octxt = lc.getOperationContext();

        Element msgElem = request.getElement(MailService.E_MSG);

        // check to see whether the entire message has been uploaded under separate cover
        String attachment = msgElem.getAttribute(MailService.A_ATTACHMENT_ID, null);

        ParseMimeMessage.MimeMessageData mimeData = new ParseMimeMessage.MimeMessageData();
        MimeMessage mm;
        if (attachment != null)
            mm = SendMsg.parseUploadedMessage(lc, attachment, mimeData);
        else
            mm = ParseMimeMessage.parseMimeMsgSoap(lc, mbox, msgElem, null, mimeData);

        long date = System.currentTimeMillis();

        try {
            mm.saveChanges();
        } catch (MessagingException me) {
            throw ServiceException.FAILURE("completing MIME message object", me);
        }

        ParsedMessage pm = new ParsedMessage(mm, date, mbox.attachmentsIndexingEnabled());
        try {
            pm.analyze();
        } catch (ServiceException e) {
            if (ConversionException.isTemporaryCauseOf(e))
                throw e;
        }

        WikiItem wikiItem;
        try {
    		wikiItem = mbox.createWiki(octxt, pm, wiki.getWikiFolderId());
        } catch (IOException ioe) {
        	throw ServiceException.FAILURE("creating wiki item", ioe);
        }
		wiki.addWiki(wikiItem);
        // we can now purge the uploaded attachments
        if (mimeData.uploads != null)
            FileUploadServlet.deleteUploads(mimeData.uploads);

        Element response = lc.createElement(MailService.SAVE_WIKI_RESPONSE);
        Element m = response.addElement(MailService.E_MSG);
        m.addAttribute(MailService.A_ID, lc.formatItemId(wikiItem));
        return response;
	}

}
