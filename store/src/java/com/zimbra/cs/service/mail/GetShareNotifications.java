/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.share.ShareNotification;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.soap.ZimbraSoapContext;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GetShareNotifications extends MailDocumentHandler {

    private static final Log sLog = LogFactory.getLog(GetShareNotifications.class);
    private final String query = "type:" + MimeConstants.CT_XML_ZIMBRA_SHARE + " AND in:inbox";
    private final Set<MailItem.Type> SEARCH_TYPES = EnumSet.of(MailItem.Type.MESSAGE);

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        Element response = zsc.createElement(MailConstants.GET_SHARE_NOTIFICATIONS_RESPONSE);
        HashSet<String> shares = new HashSet<String>();
        try(ZimbraQueryResults zqr = mbox.index.search(octxt, query, SEARCH_TYPES, SortBy.DATE_DESC, 10)) {
            while (zqr.hasNext()) {
                ZimbraHit hit = zqr.getNext();
                if (hit instanceof MessageHit) {
                    Message message = ((MessageHit)hit).getMessage();
                    try {
                        for (MPartInfo part : Mime.getParts(message.getMimeMessage())) {
                            String ctype = StringUtil.stripControlCharacters(part.getContentType());
                            if (MimeConstants.CT_XML_ZIMBRA_SHARE.equals(ctype)) {
                                ShareNotification sn = ShareNotification.fromMimePart(part.getMimePart());
                                String shareItemId = sn.getGrantorId() + ":" + sn.getItemId();
                                if (shares.contains(shareItemId)) {
                                    // this notification is stale as there is
                                    // a new one for the same share.  delete
                                    // this notification and skip to the next one.
                                    sLog.info("deleting stale notification %s", message.getId());
                                    mbox.delete(octxt, message.getId(), Type.MESSAGE);
                                    continue;
                                }
                                shares.add(shareItemId);

                                Element share = response.addElement(sn.isRevoke() || sn.isExpire() ? MailConstants.E_REVOKE : MailConstants.E_SHARE);
                                if (sn.isExpire()) {
                                    share.addAttribute(MailConstants.A_EXPIRE, true);
                                }
                                Element g = share.addUniqueElement(MailConstants.E_GRANTOR);
                                g.addAttribute(MailConstants.A_ID, sn.getGrantorId());
                                g.addAttribute(MailConstants.A_EMAIL, sn.getGrantorEmail());
                                g.addAttribute(MailConstants.A_NAME, sn.getGrantorName());
                                Element l = share.addUniqueElement(MailConstants.E_MOUNT);
                                l.addAttribute(MailConstants.A_ID, sn.getItemId());
                                l.addAttribute(MailConstants.A_NAME, sn.getItemName());
                                l.addAttribute(MailConstants.A_DEFAULT_VIEW, sn.getView());
                                l.addAttribute(MailConstants.A_RIGHTS, sn.getPermissions());
                                String status = (message.isUnread() ? "new" : "seen");
                                share.addAttribute(MailConstants.A_STATUS, status);
                                share.addAttribute(MailConstants.A_ID, "" + message.getId());
                                share.addAttribute(MailConstants.A_DATE, message.getDate());
                                if (sn.isRevoke() || sn.isExpire()) {
                                    // purge revoke/expire notification upon receipt
                                    mbox.delete(octxt, message.getId(), Type.MESSAGE);
                                }
                            }
                        }
                    } catch (IOException e) {
                        ZimbraLog.misc.warn("can't parse share notification", e);
                    } catch (MessagingException e) {
                        ZimbraLog.misc.warn("can't parse share notification", e);
                    }
                }
            }
        } catch (IOException e) {
        }

        return response;
    }
}
