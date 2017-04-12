/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetMsgRequest;
import com.zimbra.soap.mail.type.MsgSpec;
import com.zimbra.soap.type.AttributeName;
import com.zimbra.soap.type.MsgContent;

/**
 * @author schemers
 * @since May 26, 2004
 */
public class GetMsg extends MailDocumentHandler {

    private static final String[] TARGET_MSG_PATH = new String[] { MailConstants.E_MSG, MailConstants.A_ID };

    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_MSG_PATH;
    }

    @Override
    protected boolean checkMountpointProxy(Element request) {
        return false;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        GetMsgRequest req = zsc.elementToJaxb(request);
        MsgSpec msgSpec = req.getMsg();
        ItemId iid = new ItemId(msgSpec.getId(), zsc);
        String part = msgSpec.getPart();
        MsgContent wantContent = msgSpec.getWantContent();
        wantContent = wantContent == null ? MsgContent.full : wantContent;

        boolean raw = msgSpec.getRaw() != null ? msgSpec.getRaw() : false;
        boolean alwaysUseContentUrl = msgSpec.getUseContentUrl() != null ? msgSpec.getUseContentUrl() : false;
        boolean read = msgSpec.getMarkRead() != null ? msgSpec.getMarkRead() : false;
        int maxSize = msgSpec.getMaxInlinedLength() != null ? msgSpec.getMaxInlinedLength() : 0;
        boolean wantHTML = msgSpec.getWantHtml() != null ? msgSpec.getWantHtml() : false;
        boolean neuter = msgSpec.getNeuter() != null ? msgSpec.getNeuter() : true;

        Set<String> headers = null;
        for (AttributeName hdr : msgSpec.getHeaders()) {
            if (headers == null)  headers = new HashSet<String>();
            headers.add(hdr.getName());
        }

        boolean needGroupInfo = msgSpec.getNeedCanExpand() != null ? msgSpec.getNeedCanExpand() : false;

        Element response = zsc.createElement(MailConstants.GET_MSG_RESPONSE);
        if (iid.hasSubpart()) {
            // calendar item
            CalendarItem calItem = getCalendarItem(octxt, mbox, iid);
            if (raw) {
                throw ServiceException.INVALID_REQUEST("Cannot request RAW formatted subpart message", null);
            } else {
                String recurIdZ = msgSpec.getRecurIdZ();
                if (recurIdZ == null) {
                    // If not specified, try to get it from the Invite.
                    int invId = iid.getSubpartId();
                    Invite[] invs = calItem.getInvites(invId);
                    if (invs.length > 0) {
                        RecurId rid = invs[0].getRecurId();
                        if (rid != null)
                            recurIdZ = rid.getDtZ();
                    }
                }
                ToXML.encodeInviteAsMP(response, ifmt, octxt, calItem, recurIdZ, iid,
                        part, maxSize, wantHTML, neuter, headers, false, needGroupInfo);
            }
        } else {
            Message msg = getMsg(octxt, mbox, iid, read);
            if (raw) {
                ToXML.encodeMessageAsMIME(response, ifmt, octxt, msg, part,
                        false /* mustInline */, alwaysUseContentUrl /* mustNotInline */, false /* serializeType */,
                        msgSpec.getWantImapUid());
            } else {
                ToXML.encodeMessageAsMP(response, ifmt, octxt, msg, part, maxSize, wantHTML, neuter, headers,
                        false /* serializeType */, needGroupInfo,
                        LC.mime_encode_missing_blob.booleanValue(), msgSpec.getWantImapUid(), wantContent);
            }
        }
        return response;
    }

    @Override
    public boolean isReadOnly() {
        return RedoLogProvider.getInstance().isSlave();
    }

    public static CalendarItem getCalendarItem(OperationContext octxt, Mailbox mbox, ItemId iid) throws ServiceException {
        assert(iid.hasSubpart());
        return mbox.getCalendarItemById(octxt, iid.getId());
    }

    public static Message getMsg(OperationContext octxt, Mailbox mbox, ItemId iid, boolean read) throws ServiceException {
        assert(!iid.hasSubpart());
        Message msg = mbox.getMessageById(octxt, iid.getId());
        if (read && msg.isUnread() && !RedoLogProvider.getInstance().isSlave()) {
            try {
                mbox.alterTag(octxt, msg.getId(), MailItem.Type.MESSAGE, Flag.FlagInfo.UNREAD, false, null);
            } catch (ServiceException e) {
                if (e.getCode().equals(ServiceException.PERM_DENIED)) {
                    ZimbraLog.mailbox.info("no permissions to mark message as read (ignored): %d", msg.getId());
                } else {
                    ZimbraLog.mailbox.warn("problem marking message as read (ignored): %d", msg.getId(), e);
                }
            }
        }
        return msg;
    }

    public static List<Message> getMsgs(OperationContext octxt, Mailbox mbox, List<Integer> iids, boolean read) throws ServiceException {
        int i = 0;
        int[] msgIdArray = new int[iids.size()];
        for (int id : iids) {
            msgIdArray[i++] = id;
        }

        List<Message> toRet = new ArrayList<Message>(msgIdArray.length);
        for (MailItem item : mbox.getItemById(octxt, msgIdArray, MailItem.Type.MESSAGE)) {
            toRet.add((Message) item);
            if (read && item.isUnread() && !RedoLogProvider.getInstance().isSlave()) {
                mbox.alterTag(octxt, item.getId(), MailItem.Type.MESSAGE, Flag.FlagInfo.UNREAD, false, null);
            }
        }
        return toRet;
    }
}
