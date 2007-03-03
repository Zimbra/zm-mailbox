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

package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mailbox.SetCalendarItemData;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

public class SetCalendarItem extends CalendarRequest {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_FOLDER };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }

    protected class SetCalendarItemInviteParser extends ParseMimeMessage.InviteParser {
        
        private boolean mExceptOk = false;
        private boolean mForCancel = false;
        
        SetCalendarItemInviteParser(boolean exceptOk, boolean forCancel) {
            mExceptOk = exceptOk;
            mForCancel = forCancel;
        }

        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraSoapContext zc, Account account, Element inviteElem) throws ServiceException 
        {
            Element content = inviteElem.getOptionalElement(MailConstants.E_CONTENT);
            if (content != null) {
                ParseMimeMessage.InviteParserResult toRet = CalendarUtils.parseInviteRaw(account, inviteElem);
                return toRet;
            } else {
                if (mForCancel)
                    return CalendarUtils.parseInviteForCancel(
                            account, getItemType(), inviteElem, null,
                            mExceptOk, CalendarUtils.RECUR_ALLOWED);
                else
                    return CalendarUtils.parseInviteForCreate(
                            account, getItemType(), inviteElem, null, null,
                            mExceptOk, CalendarUtils.RECUR_ALLOWED);
            }
        }
    };
    
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = zsc.getOperationContext();
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        ItemId iidFolder = new ItemId(request.getAttribute(MailConstants.A_FOLDER, CreateCalendarItem.DEFAULT_FOLDER), zsc);
        String flagsStr = request.getAttribute(MailConstants.A_FLAGS, null);
        int flags = flagsStr != null ? Flag.flagsToBitmask(flagsStr) : 0;
        String tagsStr = request.getAttribute(MailConstants.A_TAGS, null);
        long tags = tagsStr != null ? Tag.tagsToBitmask(tagsStr) : 0;
        
        SetCalendarItemData defaultData;
        ArrayList<SetCalendarItemData> exceptions = new ArrayList<SetCalendarItemData>();
        
        synchronized (mbox) {
            // First, the <default>
            {
                Element e = request.getElement(MailConstants.A_DEFAULT);
                defaultData = getSetCalendarItemData(zsc, acct, mbox, e, new SetCalendarItemInviteParser(false, false));
            }
            
            // for each <except>
            for (Iterator iter = request.elementIterator(MailConstants.E_CAL_EXCEPT);
                 iter.hasNext(); ) {
                Element e = (Element) iter.next();
                SetCalendarItemData exDat = getSetCalendarItemData(
                        zsc, acct, mbox, e,
                        new SetCalendarItemInviteParser(true, false));
                exceptions.add(exDat);
            }

            // for each <cancel>
            for (Iterator iter = request.elementIterator(MailConstants.E_CAL_CANCEL);
                 iter.hasNext(); ) {
                Element e = (Element) iter.next();
                SetCalendarItemData exDat = getSetCalendarItemData(
                        zsc, acct, mbox, e,
                        new SetCalendarItemInviteParser(true, true));
                exceptions.add(exDat);
            }

            SetCalendarItemData[] exceptArray = null;
            if (exceptions.size() > 0) {
                exceptArray = new SetCalendarItemData[exceptions.size()];
                exceptions.toArray(exceptArray);
            }
            
            int calItemId = mbox.setCalendarItem(octxt, iidFolder.getId(), flags, tags, defaultData, exceptArray);
            
            Element response = getResponseElement(zsc);
            
            response.addElement(MailConstants.A_DEFAULT).addAttribute(MailConstants.A_ID, ifmt.formatItemId(defaultData.mInv.getMailItemId()));
            
            for (Iterator iter = exceptions.iterator(); iter.hasNext();) {
                SetCalendarItemData cur = (SetCalendarItemData) iter.next();
                Element e = response.addElement(MailConstants.E_CAL_EXCEPT);
                e.addAttribute(MailConstants.A_CAL_RECURRENCE_ID, cur.mInv.getRecurId().toString());
                e.addAttribute(MailConstants.A_ID, ifmt.formatItemId(cur.mInv.getMailItemId()));
            }
            String itemId = ifmt.formatItemId(calItemId);
            response.addAttribute(MailConstants.A_CAL_ID, itemId);
            if (defaultData.mInv.isEvent())
                response.addAttribute(MailConstants.A_APPT_ID_DEPRECATE_ME, itemId);  // for backward compat
            
            return response;
        } // synchronized(mbox)
    }
    
    static private SetCalendarItemData getSetCalendarItemData(
            ZimbraSoapContext zc, Account acct, Mailbox mbox,
            Element e, ParseMimeMessage.InviteParser parser)
    throws ServiceException {
        String partStatStr = e.getAttribute(MailConstants.A_CAL_PARTSTAT,
                                            IcalXmlStrMap.PARTSTAT_NEEDS_ACTION);

        // <M>
        Element msgElem = e.getElement(MailConstants.E_MSG);


        // check to see whether the entire message has been uploaded under separate cover
        String attachmentId = msgElem.getAttribute(MailConstants.A_ATTACHMENT_ID, null);
        
        Element contentElement = msgElem.getOptionalElement(MailConstants.E_CONTENT);
        
        MimeMessage mm = null;
        
        if (attachmentId != null) {
            ParseMimeMessage.MimeMessageData mimeData = new ParseMimeMessage.MimeMessageData();
            mm = SendMsg.parseUploadedMessage(zc, attachmentId, mimeData);
        } else if (contentElement != null) {
            mm = ParseMimeMessage.importMsgSoap(msgElem);
        } else {
            CalSendData dat = handleMsgElement(zc, msgElem, acct, mbox, parser);
            mm = dat.mMm;
        }
        
        ParsedMessage pm = new ParsedMessage(mm, mbox.attachmentsIndexingEnabled());
        
        pm.analyze();
        ZVCalendar cal = pm.getiCalendar();
        if (cal == null)
            throw ServiceException.FAILURE("SetCalendarItem could not build an iCalendar object", null);

        boolean sentByMe = false; // not applicable in the SetCalendarItem case

        Invite inv = Invite.createFromCalendar(acct, pm.getFragment(), cal, sentByMe).get(0);

        inv.setPartStat(partStatStr);

        SetCalendarItemData sadata = new SetCalendarItemData();
        sadata.mInv = inv;
        sadata.mPm = pm;
        sadata.mForce = true;
        return sadata;
    }
}
