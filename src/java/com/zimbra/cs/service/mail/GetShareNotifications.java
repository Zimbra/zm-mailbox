/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;

import org.dom4j.DocumentException;

import com.google.common.io.Closeables;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.soap.ZimbraSoapContext;

public class GetShareNotifications extends MailDocumentHandler {

    private final String query = "type:" + MimeConstants.CT_XML_ZIMBRA_SHARE + " AND in:inbox";
    private final Set<MailItem.Type> SEARCH_TYPES = EnumSet.of(MailItem.Type.MESSAGE);

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        Element response = zsc.createElement(MailConstants.GET_SHARE_NOTIFICATIONS_RESPONSE);
        
        ZimbraQueryResults zqr = null;
        try {
            zqr = mbox.index.search(octxt, query, SEARCH_TYPES, SortBy.DATE_DESC, 10);
            while (zqr.hasNext()) {
                ZimbraHit hit = zqr.getNext();
                if (hit instanceof MessageHit) {
                    Message message = ((MessageHit)hit).getMessage();
                    try {
                        for (MPartInfo part : Mime.getParts(message.getMimeMessage())) {
                            String ctype = StringUtil.stripControlCharacters(part.getContentType());
                            if (MimeConstants.CT_XML_ZIMBRA_SHARE.equals(ctype)) {
                                Element content = Element.parseXML(part.getMimePart().getInputStream());
                                Element grantor = content.getElement(MailConstants.E_GRANTOR);
                                Element link = content.getElement(MailConstants.E_MOUNT);
                                
                                Element share = response.addElement(MailConstants.E_SHARE);
                                Element g = share.addElement(MailConstants.E_GRANTOR);
                                g.addAttribute(MailConstants.A_ID, grantor.getAttribute(MailConstants.A_ID));
                                g.addAttribute(MailConstants.A_EMAIL, grantor.getAttribute(MailConstants.A_EMAIL));
                                g.addAttribute(MailConstants.A_NAME, grantor.getAttribute(MailConstants.A_NAME));
                                Element l = share.addElement(MailConstants.E_MOUNT);
                                l.addAttribute(MailConstants.A_ID, link.getAttribute(MailConstants.A_ID));
                                l.addAttribute(MailConstants.A_NAME, link.getAttribute(MailConstants.A_NAME));
                                l.addAttribute(MailConstants.A_DEFAULT_VIEW, link.getAttribute(MailConstants.A_DEFAULT_VIEW));
                                l.addAttribute(MailConstants.A_RIGHTS, link.getAttribute(MailConstants.A_RIGHTS));
                                String status = (message.isUnread() ? "new" : "seen");
                                share.addAttribute(MailConstants.A_STATUS, status);
                                share.addAttribute(MailConstants.A_ID, "" + message.getId());
                                share.addAttribute(MailConstants.A_DATE, message.getDate());
                            }
                        }
                    } catch (IOException e) {
                        ZimbraLog.misc.warn("can't parse share notification", e);
                    } catch (MessagingException e) {
                        ZimbraLog.misc.warn("can't parse share notification", e);
                    } catch (DocumentException e) {
                        ZimbraLog.misc.warn("can't parse share notification", e);
                    }
                }
            }
        } finally {
            Closeables.closeQuietly(zqr);
        }
        
        return response;
    }
}
