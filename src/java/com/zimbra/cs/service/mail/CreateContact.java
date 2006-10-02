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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimePart;

import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.operation.CreateContactOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.formatter.VCard;
import com.zimbra.cs.session.Session;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class CreateContact extends MailDocumentHandler  {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailService.E_CONTACT, MailService.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    private static final String DEFAULT_FOLDER = "" + Mailbox.ID_FOLDER_CONTACTS;

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        Mailbox.OperationContext octxt = lc.getOperationContext();
        Session session = getSession(context);

        Element cn = request.getElement(MailService.E_CONTACT);
        ItemId iidFolder = new ItemId(cn.getAttribute(MailService.A_FOLDER, DEFAULT_FOLDER), lc);
        String tagsStr = cn.getAttribute(MailService.A_TAGS, null);

        Map<String, String> attrs;
        Element vcard = cn.getOptionalElement(MailService.E_VCARD);
        if (vcard != null) {
            attrs = parseAttachedVCard(lc, mbox, vcard);
        } else {
            attrs = new HashMap<String, String>();
            for (Element e : cn.listElements(MailService.E_ATTRIBUTE)) {
                String name = e.getAttribute(MailService.A_ATTRIBUTE_NAME);
                if (name.trim().equals(""))
                    throw ServiceException.INVALID_REQUEST("at least one contact field name is blank", null);
                String value = e.getText();
                if (value != null && !value.equals(""))
                    attrs.put(name, value);
            }
        }

        CreateContactOperation op = new CreateContactOperation(session, octxt, mbox, Requester.SOAP,
        			iidFolder, attrs, tagsStr);
        op.schedule();
        Contact con = op.getContact();
        
        Element response = lc.createElement(MailService.CREATE_CONTACT_RESPONSE);
        if (con != null)
            ToXML.encodeContact(response, lc, con, null, true, null);
        return response;
    }

    private static Map<String, String> parseAttachedVCard(ZimbraSoapContext lc, Mailbox mbox, Element vcard)
    throws ServiceException {
        String text = null;
        String messageId = vcard.getAttribute(MailService.A_MESSAGE_ID, null);
        String attachId = vcard.getAttribute(MailService.A_ATTACHMENT_ID, null);

        if (attachId != null) {
            // separately-uploaded vcard attachment
            FileUploadServlet.Upload up = FileUploadServlet.fetchUpload(lc.getAuthtokenAccountId(), attachId, lc.getRawAuthToken());
            try {
                text = new String(ByteUtil.getContent(up.getInputStream(), 0));
            } catch (IOException e) {
                throw ServiceException.FAILURE("error reading vCard", e);
            }
        } else if (messageId == null) {
            // inlined content in the <vcard> element
            text = vcard.getText();
        } else {
            // part of existing message
            ItemId iid = new ItemId(messageId, lc);
            String part = vcard.getAttribute(MailService.A_PART);
            if (iid.isLocal())
                try {
                    // fetch from local store
                    if (!mbox.getAccountId().equals(iid.getAccountId()))
                        mbox = MailboxManager.getInstance().getMailboxByAccountId(iid.getAccountId());
                    Message msg = mbox.getMessageById(lc.getOperationContext(), iid.getId());
                    MimePart mp = Mime.getMimePart(msg.getMimeMessage(), part);
                    ContentType ctype = new ContentType(mp.getContentType());
                    if (!ctype.match(Mime.CT_TEXT_PLAIN) && !ctype.match(Mime.CT_TEXT_VCARD))
                        throw MailServiceException.INVALID_CONTENT_TYPE(ctype.getBaseType());
                    text = Mime.getStringContent(mp);
                } catch (IOException e) {
                    throw ServiceException.FAILURE("error reading vCard", e);
                } catch (MessagingException e) {
                    throw ServiceException.FAILURE("error fetching message part", e);
                }
            else
                try {
                    // fetch from remote store
                    Map<String, String> params = new HashMap<String, String>();
                    params.put(UserServlet.QP_PART, part);
                    byte[] content = UserServlet.getRemoteContent(lc.getRawAuthToken(), iid, params);
                    text = new String(content, "utf-8");
                } catch (IOException e) {
                    throw ServiceException.FAILURE("error reading vCard", e);
                }
        }

        List<VCard> cards = VCard.parseVCard(text);
        if (cards == null || cards.size() != 1)
            throw MailServiceException.UNABLE_TO_IMPORT_CONTACTS("cannot import more than 1 vCard at once", null);
        return cards.get(0).fields;
    }
}
