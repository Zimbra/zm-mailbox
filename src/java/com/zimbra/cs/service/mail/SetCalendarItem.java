/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.CalendarItem.ReplyInfo;
import com.zimbra.cs.mailbox.Mailbox.SetCalendarItemData;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.ParsedMessage.CalendarPartInfo;
import com.zimbra.cs.service.mail.ParseMimeMessage.InviteParserResult;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

public class SetCalendarItem extends CalendarRequest {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_FOLDER };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }

    static class SetCalendarItemInviteParser extends ParseMimeMessage.InviteParser {
        
        private boolean mExceptOk = false;
        private boolean mForCancel = false;
        private Folder mFolder;
        
        private byte mItemType;
        
        SetCalendarItemInviteParser(boolean exceptOk, boolean forCancel, Folder folder, byte itemType) {
            mExceptOk = exceptOk;
            mForCancel = forCancel;
            mFolder = folder;
            mItemType = itemType;
        }

        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraSoapContext zc, OperationContext octxt, Account account, Element inviteElem) throws ServiceException 
        {
            Element content = inviteElem.getOptionalElement(MailConstants.E_CONTENT);
            if (content != null) {
                ParseMimeMessage.InviteParserResult toRet = CalendarUtils.parseInviteRaw(account, inviteElem);
                return toRet;
            } else {
                if (mForCancel)
                    return CalendarUtils.parseInviteForCancel(
                            account, mFolder, mItemType, inviteElem, null,
                            mExceptOk, CalendarUtils.RECUR_ALLOWED);
                else
                    return CalendarUtils.parseInviteForCreate(
                            account, mItemType, inviteElem, null, null,
                            mExceptOk, CalendarUtils.RECUR_ALLOWED);
            }
        }
    };
    
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        String flagsStr = request.getAttribute(MailConstants.A_FLAGS, null);
        int flags = flagsStr != null ? Flag.flagsToBitmask(flagsStr) : 0;
        String tagsStr = request.getAttribute(MailConstants.A_TAGS, null);
        long tags = tagsStr != null ? Tag.tagsToBitmask(tagsStr) : 0;
        
        synchronized (mbox) {
            int defaultFolder = getItemType() == MailItem.TYPE_TASK ? Mailbox.ID_FOLDER_TASKS : Mailbox.ID_FOLDER_CALENDAR;
            String defaultFolderStr = Integer.toString(defaultFolder);
            String folderIdStr = request.getAttribute(MailConstants.A_FOLDER, defaultFolderStr);
            ItemId iidFolder = new ItemId(folderIdStr, zsc);
            Folder folder = mbox.getFolderById(octxt, iidFolder.getId());
            SetCalendarItemParseResult parsed = parseSetAppointmentRequest(request, zsc, octxt, folder, getItemType(), false);

            CalendarItem calItem = mbox.setCalendarItem(octxt, iidFolder.getId(),
                flags, tags, parsed.defaultInv, parsed.exceptions,
                parsed.replies, parsed.nextAlarm);

            Element response = getResponseElement(zsc);

            if (parsed.defaultInv != null)
                response.addElement(MailConstants.A_DEFAULT).
                    addAttribute(MailConstants.A_ID, ifmt.formatItemId(parsed.defaultInv.mInv.getMailItemId()));
            
            if (parsed.exceptions != null) {
	            for (SetCalendarItemData cur : parsed.exceptions) {
	                Element e = response.addElement(MailConstants.E_CAL_EXCEPT);
	                e.addAttribute(MailConstants.A_CAL_RECURRENCE_ID, cur.mInv.getRecurId().toString());
	                e.addAttribute(MailConstants.A_ID, ifmt.formatItemId(cur.mInv.getMailItemId()));
	            }
            }
            String itemId = ifmt.formatItemId(calItem == null ? 0 : calItem.getId());
            response.addAttribute(MailConstants.A_CAL_ID, itemId);
            if (!parsed.isTodo)
                response.addAttribute(MailConstants.A_APPT_ID_DEPRECATE_ME, itemId);  // for backward compat

            return response;
        } // synchronized(mbox)
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
                if ((desc != null && desc.length() > 0) || (descHtml != null && descHtml.length() > 0))
                    ipr.mInvite.setDescription(desc, descHtml);
            }
        }

        ParsedMessage pm = new ParsedMessage(mm, mbox.attachmentsIndexingEnabled());

        Invite inv = (ipr == null ? null : ipr.mInvite);
        if (inv == null || inv.getDTStamp() == -1) { //zdsync if -1 for 4.5 back compat
            CalendarPartInfo cpi = pm.getCalendarPartInfo();
            ZVCalendar cal = null;
            if (cpi != null && CalendarItem.isAcceptableInvite(mbox.getAccount(), cpi))
                cal = cpi.cal;
            if (cal == null)
                throw ServiceException.FAILURE("SetCalendarItem could not build an iCalendar object", null);
            boolean sentByMe = false; // not applicable in the SetCalendarItem case
            Invite iCalInv = Invite.createFromCalendar(acct, pm.getFragment(), cal, sentByMe).get(0);
            
            if (inv == null) {
            	inv = iCalInv;
            } else {
            	inv.setDtStamp(iCalInv.getDTStamp()); //zdsync
            	inv.setFragment(iCalInv.getFragment()); //zdsync
            }
        }
        inv.setPartStat(partStatStr);

        SetCalendarItemData sadata = new SetCalendarItemData();
        sadata.mInv = inv;
        sadata.mPm = pm;
        return sadata;
    }

    public static class SetCalendarItemParseResult {
        public SetCalendarItemData defaultInv;
        public SetCalendarItemData[] exceptions;
        public List<ReplyInfo> replies;
        public boolean isTodo;
        public long nextAlarm;
    }

    public static SetCalendarItemParseResult parseSetAppointmentRequest(
            Element request, ZimbraSoapContext zsc, OperationContext octxt,
            Folder folder, byte itemType, boolean parseIds)
	throws ServiceException {
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
                        zsc, octxt, acct, mbox, e, new SetCalendarItemInviteParser(false, false, folder, itemType));
                if (defInv == null)
                    defInv = result.defaultInv.mInv;
            }
        }
        
        // for each <except>
        for (Element e : request.listElements(MailConstants.E_CAL_EXCEPT)) {
            SetCalendarItemData exDat = getSetCalendarItemData(
                    zsc, octxt, acct, mbox, e, new SetCalendarItemInviteParser(true, false, folder, itemType));
            exceptions.add(exDat);
            if (defInv == null)
                defInv = exDat.mInv;
        }

        // for each <cancel>
        for (Element e : request.listElements(MailConstants.E_CAL_CANCEL)) {
            SetCalendarItemData exDat = getSetCalendarItemData(
                    zsc, octxt, acct, mbox, e, new SetCalendarItemInviteParser(true, true, folder, itemType));
            exceptions.add(exDat);
            if (defInv == null)
                defInv = exDat.mInv;
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
