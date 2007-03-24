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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.service.formatter.VCard;
import com.zimbra.cs.session.Session;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class CreateContact extends MailDocumentHandler  {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_CONTACT, MailConstants.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    private static final String DEFAULT_FOLDER = "" + Mailbox.ID_FOLDER_CONTACTS;

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        Mailbox.OperationContext octxt = zsc.getOperationContext();
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
        Session session = getSession(context);

        boolean verbose = request.getAttributeBool(MailConstants.A_VERBOSE, true);

        Element cn = request.getElement(MailConstants.E_CONTACT);
        ItemId iidFolder = new ItemId(cn.getAttribute(MailConstants.A_FOLDER, DEFAULT_FOLDER), zsc);
        String tagsStr = cn.getAttribute(MailConstants.A_TAGS, null);

        Map<String, String> attrs;
        Element vcard = cn.getOptionalElement(MailConstants.E_VCARD);
        if (vcard != null) {
            attrs = parseAttachedVCard(zsc, mbox, vcard);
        } else {
            attrs = new HashMap<String, String>();
            for (Element e : cn.listElements(MailConstants.E_ATTRIBUTE)) {
                String name = e.getAttribute(MailConstants.A_ATTRIBUTE_NAME);
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
        
        Element response = zsc.createElement(MailConstants.CREATE_CONTACT_RESPONSE);
        if (con != null) {
            if (verbose)
                ToXML.encodeContact(response, ifmt, con, null, true, null);
            else
                response.addElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, con.getId());
        }
        return response;
    }

    private static Map<String, String> parseAttachedVCard(ZimbraSoapContext zsc, Mailbox mbox, Element vcard)
    throws ServiceException {
        String text = null;
        String messageId = vcard.getAttribute(MailConstants.A_MESSAGE_ID, null);
        String attachId = vcard.getAttribute(MailConstants.A_ATTACHMENT_ID, null);

        if (attachId != null) {
            // separately-uploaded vcard attachment
            FileUploadServlet.Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), attachId, zsc.getRawAuthToken());
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
            ItemId iid = new ItemId(messageId, zsc);
            String part = vcard.getAttribute(MailConstants.A_PART);
            if (iid.isLocal())
                try {
                    // fetch from local store
                    if (!mbox.getAccountId().equals(iid.getAccountId()))
                        mbox = MailboxManager.getInstance().getMailboxByAccountId(iid.getAccountId());
                    Message msg = mbox.getMessageById(zsc.getOperationContext(), iid.getId());
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
                    byte[] content = UserServlet.getRemoteContent(zsc.getRawAuthToken(), iid, params);
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
