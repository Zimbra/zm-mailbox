/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.Locale;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.AddInviteData;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender.Verb;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.cs.mailbox.alerts.CalItemReminderService;
import com.zimbra.cs.mailbox.alerts.CalItemSmsReminderTask;
import com.zimbra.common.util.StringUtil;

/**
 * @since Mar 2, 2005
 * @author tim
 */
public class SendInviteReply extends CalendarRequest {

    private static final String[] TARGET_PATH = new String[] { MailConstants.A_ID };

    @Override
    protected String[] getProxiedIdPath(Element request)     { return TARGET_PATH; }

    @Override
    protected boolean checkMountpointProxy(Element request)  { return false; }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        Account acct = getRequestedAccount(zsc);
        Account authAcct = getAuthenticatedAccount(zsc);
        boolean isAdmin = zsc.isUsingAdminPrivileges();
        OperationContext octxt = getOperationContext(zsc, context);

        boolean onBehalfOf = isOnBehalfOfRequest(zsc);

        ItemId iid = new ItemId(request.getAttribute(MailConstants.A_ID), zsc);
        int compNum = (int) request.getAttributeLong(MailConstants.A_CAL_COMPONENT_NUM);

        String verbStr = request.getAttribute(MailConstants.A_VERB);
        Verb verb = CalendarMailSender.parseVerb(verbStr);
        boolean isDecline = CalendarMailSender.VERB_DECLINE.equals(verb);

        boolean updateOrg = request.getAttributeBool(MailConstants.A_CAL_UPDATE_ORGANIZER, true);

        // Get the identity/persona being used in the reply.  It is set at the request level, but
        // let's also look for it in the <m> child element too, because that is the precedent in
        // the SendMsg request.  For SendInviteReply we have to insist it at request level because
        // <m> is an optional element.
        String identityId = request.getAttribute(MailConstants.A_IDENTITY_ID, null);
        if (identityId == null) {
            Element msgElem = request.getOptionalElement(MailConstants.E_MSG);
            if (msgElem != null)
                identityId = msgElem.getAttribute(MailConstants.A_IDENTITY_ID, null);
        }

        Element response = getResponseElement(zsc);

        boolean intendedForMe = true;
        Invite oldInv = null;
        int calItemId;
        int inviteMsgId;
        CalendarItem calItem = null;
        boolean wasInTrash = false;
        String stat = null;

        // the user could be accepting EITHER the original-mail-item (id="nnn") OR the
        // calendar item (id="aaaa-nnnn") --- work in both cases
        if (iid.hasSubpart()) {
            // directly accepting the calendar item
            calItemId = iid.getId();
            inviteMsgId = iid.getSubpartId();
            calItem = safeGetCalendarItemById(mbox, octxt, iid.getId());
            if (calItem == null)
                throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "Could not find calendar item");
            oldInv = calItem.getInvite(inviteMsgId, compNum);
        } else {
            // accepting the message: go find the calendar item and then the invite
            inviteMsgId = iid.getId();
            Message msg = mbox.getMessageById(octxt, inviteMsgId);
            Message.CalendarItemInfo info = msg.getCalendarItemInfo(compNum);
            if (info == null)
                throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "Could not find calendar item");

            String intendedFor = msg.getCalendarIntendedFor();
            Account intendedAcct = null;
            if (intendedFor != null) {
                try {
                    InternetAddress intendedForAddr = new JavaMailInternetAddress(intendedFor);
                    intendedAcct = Provisioning.getInstance().get(AccountBy.name, intendedForAddr.getAddress());
                } catch (AddressException e) {
                    throw ServiceException.INVALID_REQUEST("The intended account " + intendedFor + " is invalid", e);
                }
                if (intendedAcct == null) {
                    throw ServiceException.INVALID_REQUEST("The intended account " + intendedFor + " was not found", null);
                }
                // Special case: intended account = me.
                if (intendedAcct.equals(mbox.getAccount()))
                    intendedAcct = null;
                else
                    intendedForMe = false;
            }

            if (intendedAcct != null) {
                // trace logging: let's just indicate we're replying to a remote appointment
                ZimbraLog.calendar.info("<SendInviteReply> (remote mbox) id=%s, verb=%s, notifyOrg=%s",
                        new ItemIdFormatter(zsc).formatItemId(iid),
                        verb.toString(), Boolean.toString(updateOrg));

                // Replying to a remote appointment
                calItem = null;
                calItemId = 0;
                ZMailbox zmbx = getRemoteZMailbox(octxt, authAcct, intendedAcct);
                // Try to add the appointment to remote mailbox.
                AddInviteResult addInviteResult = sendAddInvite(zmbx, octxt, msg);
                if (addInviteResult == null)
                    throw MailServiceException.INVITE_OUT_OF_DATE(iid.toString());

                // Forward the reply request.
                remoteSendInviteReply(zmbx, request, addInviteResult);
            } else {
                // Replying to a local appointment
                if (info.getInvite() != null) {
                    calItem = mbox.getCalendarItemByUid(octxt, info.getInvite().getUid());
                    wasInTrash = calItem != null && calItem.inTrash();

                    if (calItem != null && !wasInTrash) {
                        Invite newInv = info.getInvite();
                        // If appointment exists, check if our invite has been outdated.
                        Invite curr = calItem.getInvite(newInv.getRecurId());
                        if (curr != null && !newInv.isSameOrNewerVersion(curr))
                            throw MailServiceException.INVITE_OUT_OF_DATE(iid.toString());
                    }

                    Invite inv = info.getInvite().newCopy();
                    Invite.setDefaultAlarm(inv, acct);
                    inv.setMailItemId(inviteMsgId);

                    // If appointment doesn't exist, add it if the reply is not a decline.  No need to
                    // add to calendar if we're declining.
                    // If appointment already exists, apply the invite against it so the appointment has
                    // the latest data we replied on.  This is true even when declining.
                    // (TODO: Is it better to delete the existing appointment/instance when declining?)
                    if (calItem != null || !isDecline) {
                        // Add the invite.  This will either create or update the appointment.
                        int folder;
                        boolean untrashing = wasInTrash && !isDecline;
                        if (calItem == null || untrashing) {
                            // If appointment/task doesn't exist, create in default folder.
                            // If it exists but is in Trash and is not a decline, move it out of Trash.
                            // If it's in trash and we're declining, leave it in trash.
                            folder = inv.isTodo() ? Mailbox.ID_FOLDER_TASKS : Mailbox.ID_FOLDER_CALENDAR;
                        } else {
                            folder = calItem.getFolderId();
                        }
                        ParsedMessage pm = new ParsedMessage(msg.getMimeMessage(false), false);
                        AddInviteData aid = mbox.addInvite(octxt, inv, folder, pm, false, untrashing, true);
                        if (aid == null)
                            throw ServiceException.FAILURE("Could not create/update calendar item", null);
                        calItemId = aid.calItemId;
                        // Refetch updated item.
                        calItem = safeGetCalendarItemById(mbox, octxt, aid.calItemId);
                        if (calItem == null)
                            throw ServiceException.FAILURE("Could not refetch created/updated calendar item", null);
                    } else {
                        calItemId = 0;
                    }
                    oldInv = inv;
                } else if (info.calItemCreated()) {
                    // legacy case (before we added Invite info to Message metadata)
                    calItem = safeGetCalendarItemById(mbox, octxt, info.getCalendarItemId());
                    if (calItem == null)
                        throw ServiceException.FAILURE("Missing invite data", null);
                    calItemId = info.getCalendarItemId().getId();  // Must be for this mailbox
                    wasInTrash = calItem.inTrash();
                    oldInv = calItem.getInvite(inviteMsgId, compNum);
                } else {
                    throw ServiceException.FAILURE("Missing invite data", null);
                }
            }
        }

        if (intendedForMe) {
            if (oldInv == null)
                throw MailServiceException.INVITE_OUT_OF_DATE(iid.toString());

            if (calItem != null && (mbox.getEffectivePermissions(octxt, calItemId, MailItem.Type.UNKNOWN) & ACL.RIGHT_ACTION) == 0) {
                throw ServiceException.PERM_DENIED("You do not have ACTION rights for CalendarItem "+calItemId);
            }

            // check if invite organizer requested rsvp or not
            if (oldInv.hasRsvp()) {
                updateOrg = updateOrg && oldInv.getRsvp();
            }

            // Don't allow creating/editing a private appointment on behalf of another user,
            // unless that other user is a calendar resource.
            boolean allowPrivateAccess = calItem != null ? calItem.allowPrivateAccess(authAcct, isAdmin) : true;
            boolean isCalendarResource = acct instanceof CalendarResource;
            if (!allowPrivateAccess && !oldInv.isPublic() && !isCalendarResource)
                throw ServiceException.PERM_DENIED("Cannot reply to a private appointment/task on behalf of another user");

            // see if there is a specific Exception being referenced by this reply...
            Element exc = request.getOptionalElement(MailConstants.E_CAL_EXCEPTION_ID);
            ParsedDateTime exceptDt = null;
            if (exc != null) {
                TimeZoneMap tzmap = oldInv.getTimeZoneMap();
                Element tzElem = request.getOptionalElement(MailConstants.E_CAL_TZ);
                ICalTimeZone tz = null;
                if (tzElem != null) {
                    tz = CalendarUtils.parseTzElement(tzElem);
                    tzmap.add(tz);
                }
                exceptDt = CalendarUtils.parseDateTime(exc, tzmap);
            } else if (oldInv.hasRecurId()) {
                exceptDt = oldInv.getRecurId().getDt();
            }

            // trace logging
            String calItemIdStr = calItem != null ? Integer.toString(calItem.getId()) : "none";
            String folderIdStr = calItem != null ? Integer.toString(calItem.getFolderId()) : "none";
            if (exceptDt == null)
                ZimbraLog.calendar.info("<SendInviteReply> id=%s, folderId=%s, verb=%s, notifyOrg=%s, subject=\"%s\", UID=%s",
                        calItemIdStr, folderIdStr, verb.toString(), Boolean.toString(updateOrg),
                        oldInv.isPublic() ? oldInv.getName() : "(private)", oldInv.getUid());
            else
                ZimbraLog.calendar.info("<SendInviteReply> id=%s, folderId=%s, verb=%s, notifyOrg=%s, subject=\"%s\", UID=%s, recurId=%s",
                        calItemIdStr, folderIdStr, verb.toString(), Boolean.toString(updateOrg),
                        oldInv.isPublic() ? oldInv.getName() : "(private)", oldInv.getUid(), exceptDt.getUtcString());

            // If we're replying to a non-exception instance of a recurring appointment, create a local
            // exception instance first.  Then reply to it.
            if (calItem != null && oldInv.isRecurrence() && exceptDt != null) {
                Invite localException = oldInv.makeInstanceInvite(exceptDt);
                long now = octxt != null ? octxt.getTimestamp() : System.currentTimeMillis();
                localException.setDtStamp(now);

                String partStat = verb.getXmlPartStat();
                localException.setPartStat(partStat);
                ZAttendee at = localException.getMatchingAttendee(acct, identityId);
                if (at != null)
                    at.setPartStat(partStat);

                // Carry over the MimeMessage/ParsedMessage to preserve any attachments.
                MimeMessage mmInv = calItem.getSubpartMessage(oldInv.getMailItemId());
                ParsedMessage pm = mmInv != null ? new ParsedMessage(mmInv, false) : null;

                int folder;
                boolean untrashing = wasInTrash && !isDecline;
                if (untrashing) {
                    // If it exists but is in Trash and is not a decline, move it out of Trash.
                    // If it's in trash and we're declining, leave it in trash.
                    folder = localException.isTodo() ? Mailbox.ID_FOLDER_TASKS : Mailbox.ID_FOLDER_CALENDAR;
                } else {
                    folder = calItem.getFolderId();
                }
                mbox.addInvite(octxt, localException, folder, pm, true, untrashing, true);

                // Refetch the updated calendar item and set oldInv to refetched local exception instance.
                calItem = safeGetCalendarItemById(mbox, octxt, calItemId);
                if (calItem == null)
                    throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "Could not find calendar item");
                oldInv = calItem.getInvite(new RecurId(exceptDt, RecurId.RANGE_NONE));
            }

            ZAttendee att = oldInv.getMatchingAttendee(mbox.getAccount());
            if (att != null) {
                stat = att.getPartStat();
            }

            if (updateOrg && oldInv.hasOrganizer()) {
                Locale locale;
                Account organizer = oldInv.getOrganizerAccount();
                if (organizer != null)
                    locale = organizer.getLocale();
                else
                    locale = !onBehalfOf ? acct.getLocale() : authAcct.getLocale();
                String subject;
                if (!allowPrivateAccess && !oldInv.isPublic())
                    subject = L10nUtil.getMessage(MsgKey.calendarSubjectWithheld, locale);
                else
                    subject = oldInv.getName();
                String replySubject =
                    CalendarMailSender.getReplySubject(verb, subject, locale);

                CalSendData csd = new CalSendData();
                csd.mOrigId = new ItemId(mbox, oldInv.getMailItemId());
                csd.mReplyType = MailSender.MSGTYPE_REPLY;
                csd.mInvite = CalendarMailSender.replyToInvite(acct, identityId, authAcct, onBehalfOf, allowPrivateAccess, oldInv, verb, replySubject, exceptDt);

                ZVCalendar iCal = csd.mInvite.newToICalendar(true);

                ParseMimeMessage.MimeMessageData parsedMessageData = new ParseMimeMessage.MimeMessageData();

                // did they specify a custom <m> message?  If so, then we don't have to build one...
                Element msgElem = request.getOptionalElement(MailConstants.E_MSG);
                if (msgElem != null) {
                    String text = ParseMimeMessage.getTextPlainContent(msgElem);
                    String html = ParseMimeMessage.getTextHtmlContent(msgElem);
                    iCal.addDescription(text, html);

                    MimeBodyPart[] mbps = new MimeBodyPart[1];
                    mbps[0] = CalendarMailSender.makeICalIntoMimePart(iCal);

                    // the <inv> element is *NOT* allowed -- we always build it manually
                    // based on the params to the <SendInviteReply> and stick it in the
                    // mbps (additionalParts) parameter...
                    csd.mMm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, mbox, msgElem,
                        mbps, ParseMimeMessage.NO_INV_ALLOWED_PARSER, parsedMessageData);
                } else {
                    // build a default "Accepted" response
                    if (!(acct instanceof CalendarResource)) {
                        csd.mMm = CalendarMailSender.createDefaultReply(
                                acct, identityId, authAcct, identityId, isAdmin, onBehalfOf, calItem, oldInv, null, replySubject,
                                verb, null, iCal);
                    } else {
                        // different template for calendar resources
                        RecurId rid = oldInv.getRecurId();
                        ParsedDateTime ridDt = rid != null ? rid.getDt() : null;
                        Invite replyInv = CalendarMailSender.replyToInvite(
                                acct, authAcct, onBehalfOf, allowPrivateAccess, oldInv,
                                verb, replySubject, ridDt);
                        MimeMessage mmInv = calItem.getSubpartMessage(oldInv.getMailItemId());
                        csd.mMm = CalendarMailSender.createResourceAutoReply(
                                octxt, identityId, identityId, mbox, verb, false, null,
                                calItem, oldInv, new Invite[] { replyInv }, mmInv, true);
                    }
                }

                int apptFolderId;
                if (calItem != null)
                    apptFolderId = calItem.getFolderId();
                else
                    apptFolderId = oldInv.isTodo() ? Mailbox.ID_FOLDER_TASKS : Mailbox.ID_FOLDER_CALENDAR;
                MailSendQueue sendQueue = new MailSendQueue();
                try {
                    if (stat != null && IcalXmlStrMap.PARTSTAT_DECLINED.equals(stat) && !CalendarMailSender.VERB_DECLINE.equals(verb)) {
                        sendCalendarMessage(zsc, octxt, apptFolderId, acct, mbox, csd, response, sendQueue, true/*set previous folder*/);
                    } else {
                        sendCalendarMessage(zsc, octxt, apptFolderId, acct, mbox, csd, response, sendQueue);
                    }
                } finally {
                    sendQueue.send();
                }
            }

            RecurId recurId = null;
            if (exceptDt != null) {
                recurId = new RecurId(exceptDt, RecurId.RANGE_NONE);
            }
            ZAttendee me = oldInv.getMatchingAttendee(acct);
            String cnStr = null;
            String addressStr = acct.getName();
            String role = IcalXmlStrMap.ROLE_OPT_PARTICIPANT;
            int seqNo = oldInv.getSeqNo();
            long dtStamp = oldInv.getDTStamp();
            if (me != null) {
                if (me.hasCn()) {
                    cnStr = me.getCn();
                }
                addressStr = me.getAddress();
                if (me.hasRole()) {
                    role = me.getRole();
                }
            }

            if (calItem != null)
                mbox.modifyPartStat(octxt, calItemId, recurId, cnStr, addressStr, null, role, verb.getXmlPartStat(), Boolean.FALSE, seqNo, dtStamp);
        }
        
		try {
			if(!StringUtil.isNullOrEmpty(verbStr) && "ACCEPT".equals(verbStr)) {
				CalItemReminderService.scheduleReminder(new CalItemSmsReminderTask(), calItem);
			}
		} catch(Exception ex) {
			ZimbraLog.calendar.warn("Can't schedule reminder sms for recipient : "+ex.getMessage());
		}
        // move the invite to the Trash if the user wants it
        if (deleteInviteOnReply(acct)) {
            try {
                if (onBehalfOf) {
                    // HACK: Run the move in the context of the organizer
                    // mailbox because the authenticated account doesn't
                    // have rights on Inbox and Trash folders.
                    octxt = new OperationContext(mbox);
                }
                mbox.move(octxt, inviteMsgId, MailItem.Type.MESSAGE, Mailbox.ID_FOLDER_TRASH);
            } catch (MailServiceException.NoSuchItemException nsie) {
                ZimbraLog.calendar.debug("can't move nonexistent invite to Trash: " + inviteMsgId);
            }
        }

        return response;
    }

    /**
     * @throws ServiceException subclasses may throw an error
     */
    protected boolean deleteInviteOnReply(Account acct) throws ServiceException {
        return acct.isPrefDeleteInviteOnReply();
    }

    private static class AddInviteResult {
        private final int mCalItemId;
        private final int mInvId;
        private final int mCompNum;
        public AddInviteResult(int calItemId, int invId, int compNum) {
            mCalItemId = calItemId;
            mInvId = invId;
            mCompNum = compNum;
        }
        public int getCalItemId() { return mCalItemId; }
        public int getInvId() { return mInvId; }
        public int getCompNum() { return mCompNum; }
    }

    private static ZMailbox getRemoteZMailbox(OperationContext octxt, Account authAcct, Account targetAcct)
    throws ServiceException {
        AuthToken authToken = null;
        if (octxt != null)
            authToken = AuthToken.getCsrfUnsecuredAuthToken(octxt.getAuthToken());
        if (authToken == null)
            authToken = AuthProvider.getAuthToken(authAcct);
        String pxyAuthToken = authToken.getProxyAuthToken();
        ZAuthToken zat = pxyAuthToken == null ? authToken.toZAuthToken() : new ZAuthToken(pxyAuthToken);

        ZMailbox.Options zoptions = new ZMailbox.Options(zat, AccountUtil.getSoapUri(targetAcct));
        zoptions.setNoSession(true);
        zoptions.setTargetAccount(targetAcct.getId());
        zoptions.setTargetAccountBy(Key.AccountBy.id);
        ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
        if (zmbx != null) {
            zmbx.setName(targetAcct.getName()); /* need this when logging in using another user's auth */
        }
        return zmbx;
    }

    private static AddInviteResult sendAddInvite(ZMailbox zmbx, OperationContext octxt, Message msg)
    throws ServiceException {
        ItemIdFormatter ifmt = new ItemIdFormatter();
        Element addInvite = Element.create(SoapProtocol.SoapJS, MailConstants.ADD_APPOINTMENT_INVITE_REQUEST);
        ToXML.encodeMessageAsMIME(addInvite, ifmt, octxt, msg, null /* part */,
                true /* mustInline */, false /* mustNotInline */, false /* serializeType */, ToXML.NOTIFY_FIELDS);
        Element response = zmbx.invoke(addInvite);
        int calItemId = (int) response.getAttributeLong(MailConstants.A_CAL_ID, 0);
        int invId = (int) response.getAttributeLong(MailConstants.A_CAL_INV_ID, 0);
        int compNum = (int) response.getAttributeLong(MailConstants.A_CAL_COMPONENT_NUM, 0);
        if (calItemId != 0)
            return new AddInviteResult(calItemId, invId, compNum);
        else
            return null;
    }

    private static void remoteSendInviteReply(ZMailbox zmbx, Element origRequest, AddInviteResult ids)
    throws ServiceException {
        ItemIdFormatter ifmt = new ItemIdFormatter();
        Element req = origRequest.clone();
        req.detach();
        String idStr = ifmt.formatItemId(ids.getCalItemId(), ids.getInvId());
        req.addAttribute(MailConstants.A_ID, idStr);
        req.addAttribute(MailConstants.A_CAL_COMPONENT_NUM, ids.getCompNum());
        zmbx.invoke(req);
    }

    /**
     * Note returns null if {@code calItemId} represents a remote calendar item
     */
    private static CalendarItem safeGetCalendarItemById(Mailbox mbox, OperationContext octxt, ItemId calItemId)
    throws ServiceException {
        try {
            return mbox.getCalendarItemById(octxt, calItemId);
        } catch (MailServiceException e) {
            String code = e.getCode();
            if (MailServiceException.NO_SUCH_ITEM.equals(code)
                || MailServiceException.NO_SUCH_CALITEM.equals(code)
                || MailServiceException.NO_SUCH_APPT.equals(code)
                || MailServiceException.NO_SUCH_TASK.equals(code))
                return null;
            else
                throw e;
        }
    }

    private static CalendarItem safeGetCalendarItemById(Mailbox mbox, OperationContext octxt, int calItemId)
    throws ServiceException {
        try {
            return mbox.getCalendarItemById(octxt, calItemId);
        } catch (MailServiceException e) {
            String code = e.getCode();
            if (MailServiceException.NO_SUCH_ITEM.equals(code)
                || MailServiceException.NO_SUCH_CALITEM.equals(code)
                || MailServiceException.NO_SUCH_APPT.equals(code)
                || MailServiceException.NO_SUCH_TASK.equals(code))
                return null;
            else
                throw e;
        }
    }
}
