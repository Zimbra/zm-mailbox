/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.List;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.CalendarItem.ReplyInfo;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.SetCalendarItemData;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.util.TagUtil;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.ParsedMessage.CalendarPartInfo;
import com.zimbra.cs.service.mail.ParseMimeMessage.InviteParserResult;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

public class SetCalendarItem extends CalendarRequest {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_FOLDER };

    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_FOLDER_PATH;
    }

    @Override
    protected boolean checkMountpointProxy(Element request)  {
        return true;
    }

    static class SetCalendarItemInviteParser extends ParseMimeMessage.InviteParser {

        private boolean mExceptOk = false;
        private boolean mForCancel = false;
        private final Folder mFolder;
        private final MailItem.Type type;

        SetCalendarItemInviteParser(boolean exceptOk, boolean forCancel, Folder folder, MailItem.Type type) {
            mExceptOk = exceptOk;
            mForCancel = forCancel;
            mFolder = folder;
            this.type = type;
        }

        @Override
        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraSoapContext zc, OperationContext octxt,
                Account account, Element inviteElem) throws ServiceException {
            Element content = inviteElem.getOptionalElement(MailConstants.E_CONTENT);
            if (content != null) {
                ParseMimeMessage.InviteParserResult toRet = CalendarUtils.parseInviteRaw(account, inviteElem);
                return toRet;
            } else {
                if (mForCancel) {
                    return CalendarUtils.parseInviteForCancel(account, mFolder, type, inviteElem, null,
                            mExceptOk, CalendarUtils.RECUR_ALLOWED);
                } else {
                    return CalendarUtils.parseInviteForCreate(account, type, inviteElem, null, null,
                            mExceptOk, CalendarUtils.RECUR_ALLOWED);
                }
            }
        }
    };

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        int flags = Flag.toBitmask(request.getAttribute(MailConstants.A_FLAGS, null));
        String[] tags = TagUtil.parseTags(request, mbox, octxt);

        try (final MailboxLock l = mbox.lock(true)) {
            l.lock();
            int defaultFolder = getItemType() == MailItem.Type.TASK ? Mailbox.ID_FOLDER_TASKS : Mailbox.ID_FOLDER_CALENDAR;
            String defaultFolderStr = Integer.toString(defaultFolder);
            String folderIdStr = request.getAttribute(MailConstants.A_FOLDER, defaultFolderStr);
            ItemId iidFolder = new ItemId(folderIdStr, zsc);
            Folder folder = mbox.getFolderById(octxt, iidFolder.getId());
            SetCalendarItemParseResult parsed = parseSetAppointmentRequest(request, zsc, octxt, folder, getItemType(), false);

            CalendarItem calItem = mbox.setCalendarItem(octxt, iidFolder.getId(),
                    flags, tags, parsed.defaultInv, parsed.exceptions,
                    parsed.replies, parsed.nextAlarm);

            Element response = getResponseElement(zsc);

            if (parsed.defaultInv != null) {
                response.addElement(MailConstants.A_DEFAULT).
                    addAttribute(MailConstants.A_ID, ifmt.formatItemId(parsed.defaultInv.invite.getMailItemId()));
            }

            if (parsed.exceptions != null) {
                for (SetCalendarItemData cur : parsed.exceptions) {
                    Element e = response.addElement(MailConstants.E_CAL_EXCEPT);
                    e.addAttribute(MailConstants.A_CAL_RECURRENCE_ID, cur.invite.getRecurId().toString());
                    e.addAttribute(MailConstants.A_ID, ifmt.formatItemId(cur.invite.getMailItemId()));
                }
            }
            String itemId = ifmt.formatItemId(calItem == null ? 0 : calItem.getId());
            response.addAttribute(MailConstants.A_CAL_ID, itemId);
            try {
                Element inv = request.getElement(MailConstants.A_DEFAULT).getElement(MailConstants.E_MSG).getElement(MailConstants.E_INVITE);
                Element comp = inv.getOptionalElement(MailConstants.E_INVITE_COMPONENT);
                if (comp != null) {
                    inv = comp;
                }
                String reqCalItemId = inv.getAttribute(MailConstants.A_CAL_ID);
                String uid = inv.getAttribute(MailConstants.A_UID);
                boolean uidSame = (calItem == null || (calItem.getUid() == null && uid == null) || (calItem.getUid() != null && (calItem.getUid().equals(uid) || (Invite.isOutlookUid(calItem.getUid()) && calItem.getUid().equalsIgnoreCase(uid))))); //new or same as requested, or Outlook and case-insensitive equal
                if (ZimbraLog.calendar.isInfoEnabled()) {
                    StringBuilder logBuf = new StringBuilder();
                    if (!reqCalItemId.equals(itemId)) {
                        logBuf.append("Mapped requested id ").append(reqCalItemId).append(" -> ").append(itemId);
                    }
                    if (!uidSame) {
                        logBuf.append(" ?? requested UID ").append(uid).append(" differs from mapped ").append(calItem.getUid());
                        ZimbraLog.calendar.warn(logBuf.toString());
                    } else if (logBuf.length() > 0) {
                        ZimbraLog.calendar.info(logBuf.toString());
                    }
                }
                assert(uidSame);
            } catch (ServiceException se) {
                //one of the elements we wanted to use doesn't exist; ignore; no log/assertion possible
            }
            if (!parsed.isTodo)
                response.addAttribute(MailConstants.A_APPT_ID_DEPRECATE_ME, itemId);  // for backward compat

            return response;
        }
    }

    static SetCalendarItemData getSetCalendarItemData(ZimbraSoapContext zsc, OperationContext octxt, Account acct, Mailbox mbox, Element e, ParseMimeMessage.InviteParser parser)
    throws ServiceException {
        String partStatStr = e.getAttribute(MailConstants.A_CAL_PARTSTAT, IcalXmlStrMap.PARTSTAT_NEEDS_ACTION);

        // <M>
        Element msgElem = e.getElement(MailConstants.E_MSG);

        // check to see whether the entire message has been uploaded under separate cover
        String attachmentId = msgElem.getAttribute(MailConstants.A_ATTACHMENT_ID, null);
        Element contentElement = msgElem.getOptionalElement(MailConstants.E_CONTENT);

        InviteParserResult ipr = null;

        MimeMessage mm = null;
        if (attachmentId != null) {
            ParseMimeMessage.MimeMessageData mimeData = new ParseMimeMessage.MimeMessageData();
            mm = SendMsg.parseUploadedMessage(zsc, attachmentId, mimeData);
        } else if (contentElement != null) {
            mm = ParseMimeMessage.importMsgSoap(msgElem);
        } else {
            CalSendData dat = handleMsgElement(zsc, octxt, msgElem, acct, mbox, parser);
            mm = dat.mMm;
            ipr = parser.getResult();
        }

        if (ipr == null && msgElem.getOptionalElement(MailConstants.E_INVITE) != null) {
            ipr = parser.parse(zsc, octxt, mbox.getAccount(), msgElem.getElement(MailConstants.E_INVITE));
            // Get description texts out of the MimeMessage and set in the parsed invite.  Do it only if
            // the MimeMessage has text parts.  This prevents discarding texts when they're specified only
            // in the <inv> but not in mime parts.
            if (ipr != null && ipr.mInvite != null && mm != null) {
                String desc = Invite.getDescription(mm, MimeConstants.CT_TEXT_PLAIN);
                String descHtml = Invite.getDescription(mm, MimeConstants.CT_TEXT_HTML);
                if ((desc != null && desc.length() > 0) || (descHtml != null && descHtml.length() > 0)) {
                    ipr.mInvite.setDescription(desc, descHtml);
                }
            }
        }

        ParsedMessage pm = new ParsedMessage(mm, mbox.attachmentsIndexingEnabled());

        Invite inv = (ipr == null ? null : ipr.mInvite);
        if (inv == null || inv.getDTStamp() == -1) { //zdsync if -1 for 4.5 back compat
            CalendarPartInfo cpi = pm.getCalendarPartInfo();
            ZVCalendar cal = null;
            if (cpi != null && CalendarItem.isAcceptableInvite(mbox.getAccount(), cpi)) {
                cal = cpi.cal;
            }
            if (cal == null) {
                throw ServiceException.FAILURE("SetCalendarItem could not build an iCalendar object", null);
            }
            boolean sentByMe = false; // not applicable in the SetCalendarItem case
            Invite iCalInv = Invite.createFromCalendar(acct, pm.getFragment(acct.getLocale()), cal, sentByMe).get(0);

            if (inv == null) {
                inv = iCalInv;
            } else {
                inv.setDtStamp(iCalInv.getDTStamp()); //zdsync
                inv.setFragment(iCalInv.getFragment()); //zdsync
            }
        }
        inv.setPartStat(partStatStr);

        SetCalendarItemData sadata = new SetCalendarItemData();
        sadata.invite = inv;
        sadata.message = pm;
        return sadata;
    }

    public static class SetCalendarItemParseResult {
        public SetCalendarItemData defaultInv;
        public SetCalendarItemData[] exceptions;
        public List<ReplyInfo> replies;
        public boolean isTodo;
        public long nextAlarm;
    }

    public static SetCalendarItemParseResult parseSetAppointmentRequest(Element request, ZimbraSoapContext zsc,
            OperationContext octxt, Folder folder, MailItem.Type type, boolean parseIds) throws ServiceException {
        Account acct = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);

        SetCalendarItemParseResult result = new SetCalendarItemParseResult();
        ArrayList<SetCalendarItemData> exceptions = new ArrayList<SetCalendarItemData>();
        Invite defInv = null;

        // First, the <default>
        {
            Element e = request.getOptionalElement(MailConstants.A_DEFAULT);
            if (e != null) {
                result.defaultInv = getSetCalendarItemData(
                        zsc, octxt, acct, mbox, e, new SetCalendarItemInviteParser(false, false, folder, type));
                defInv = result.defaultInv.invite;
            }
        }

        // for each <except>
        for (Element e : request.listElements(MailConstants.E_CAL_EXCEPT)) {
            SetCalendarItemData exDat = getSetCalendarItemData(
                    zsc, octxt, acct, mbox, e, new SetCalendarItemInviteParser(true, false, folder, type));
            exceptions.add(exDat);
            if (defInv == null) {
                defInv = exDat.invite;
            }
        }

        // for each <cancel>
        for (Element e : request.listElements(MailConstants.E_CAL_CANCEL)) {
            SetCalendarItemData exDat = getSetCalendarItemData(
                    zsc, octxt, acct, mbox, e, new SetCalendarItemInviteParser(true, true, folder, type));
            exceptions.add(exDat);
            if (defInv == null) {
                defInv = exDat.invite;
            }
        }

        if (exceptions.size() > 0) {
            result.exceptions = new SetCalendarItemData[exceptions.size()];
            exceptions.toArray(result.exceptions);
        } else {
            if (result.defaultInv == null)
                throw ServiceException.INVALID_REQUEST("No default/except/cancel specified", null);
        }

        // <replies>
        Element repliesElem = request.getOptionalElement(MailConstants.E_CAL_REPLIES);
        if (repliesElem != null)
            result.replies = CalendarUtils.parseReplyList(repliesElem, defInv.getTimeZoneMap());

        result.isTodo = defInv != null && defInv.isTodo();

        boolean noNextAlarm = request.getAttributeBool(MailConstants.A_CAL_NO_NEXT_ALARM, false);
        if (noNextAlarm)
            result.nextAlarm = CalendarItem.NEXT_ALARM_ALL_DISMISSED;
        else
            result.nextAlarm = request.getAttributeLong(MailConstants.A_CAL_NEXT_ALARM, CalendarItem.NEXT_ALARM_KEEP_CURRENT);

        return result;
    }
}
