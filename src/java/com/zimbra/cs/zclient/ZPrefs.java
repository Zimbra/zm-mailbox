/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.zclient;

import java.util.Collection;
import java.util.Map;
import java.util.TimeZone;

import com.google.common.collect.Iterables;
import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.cs.account.Provisioning;

public class ZPrefs {

    private Map<String, Collection<String>> mPrefs;

    public ZPrefs(Map<String, Collection<String>> prefs) {
        mPrefs = prefs;
    }
    
    /**
     * @param name name of pref to get
     * @return null if unset, or first value in list
     */
    public String get(String name) {
        Collection<String> values = mPrefs.get(name);
        if (values.isEmpty()) {
            return null;
        }
        return Iterables.get(values, 0);
    }

    public boolean getBool(String name) {
        return Provisioning.TRUE.equals(get(name));
    }

    public long getLong(String name) {
        String v = get(name);
        try {
            return v == null ? -1 : Long.parseLong(v);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public Map<String, Collection<String>> getPrefs() { return mPrefs; }

    public String getAppleiCalDelegationEnabled() { return get(Provisioning.A_zimbraPrefAppleIcalDelegationEnabled); }
    
    public String getComposeFormat() { return get(Provisioning.A_zimbraPrefComposeFormat); }

    public String getHtmlEditorDefaultFontFamily() { return get(Provisioning.A_zimbraPrefHtmlEditorDefaultFontFamily); }

    public String getHtmlEditorDefaultFontSize() { return get(Provisioning.A_zimbraPrefHtmlEditorDefaultFontSize); }

    public String getHtmlEditorDefaultFontColor() { return get(Provisioning.A_zimbraPrefHtmlEditorDefaultFontColor); }

    public String getLocale() { return get(Provisioning.A_zimbraPrefLocale); }

    public String getYintl() { return get(Provisioning.A_zimbraPrefLocale); }

    public boolean getUseTimeZoneListInCalendar() { return getBool(Provisioning.A_zimbraPrefUseTimeZoneListInCalendar); }

    public boolean getReadingPaneEnabled() { return getBool(Provisioning.A_zimbraPrefReadingPaneEnabled); }

    public String getReadingPaneLocation() { return get(Provisioning.A_zimbraPrefReadingPaneLocation); }

    public boolean getMailSignatureEnabled() { return getBool(Provisioning.A_zimbraPrefMailSignatureEnabled); }

    public boolean getIncludeSpamInSearch() { return getBool(Provisioning.A_zimbraPrefIncludeSpamInSearch); }

    public boolean getIncludeTrashInSearch() { return getBool(Provisioning.A_zimbraPrefIncludeTrashInSearch); }

    public boolean getShowSearchString() { return getBool(Provisioning.A_zimbraPrefShowSearchString); }

    public boolean getShowFragments() { return getBool(Provisioning.A_zimbraPrefShowFragments); }

    public boolean getSaveToSent() { return getBool(Provisioning.A_zimbraPrefSaveToSent); }

    public boolean getOutOfOfficeReplyEnabled() { return getBool(Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled); }

    public boolean getNewMailNotificationsEnabled() { return getBool(Provisioning.A_zimbraPrefNewMailNotificationEnabled); }

    public boolean getMailLocalDeliveryDisabled() { return getBool(Provisioning.A_zimbraPrefMailLocalDeliveryDisabled); }

    public boolean getMessageViewHtmlPreferred() { return getBool(Provisioning.A_zimbraPrefMessageViewHtmlPreferred); }

    public boolean getAutoAddAddressEnabled() { return getBool(Provisioning.A_zimbraPrefAutoAddAddressEnabled); }

    public String getShortcuts() { return get(Provisioning.A_zimbraPrefShortcuts); }

    public boolean getUseKeyboardShortcuts() { return getBool(Provisioning.A_zimbraPrefUseKeyboardShortcuts); }

    public boolean getSignatureEnabled() { return getBool(Provisioning.A_zimbraPrefMailSignatureEnabled); }

    public String getClientType() { return get(Provisioning.A_zimbraPrefClientType); }
    public boolean getIsAdvancedClient() { return "advanced".equals(getClientType()); }
    public boolean getIsStandardClient() { return "standard".equals(getClientType()); }
    
    public String getSignatureStyle() { return get(Provisioning.A_zimbraPrefMailSignatureStyle); }
    public boolean getSignatureStyleTop() { return "outlook".equals(getSignatureStyle()); }
    public boolean getSignatureStyleBottom() { return "internet".equals(getSignatureStyle()); }

    public String getGroupMailBy() { return get(Provisioning.A_zimbraPrefGroupMailBy); }

    public boolean getGroupByConversation() {
        String gb = getGroupMailBy();
        return "conversation".equals(gb);
    }

    public boolean getGroupByMessage() {
        String gb = getGroupMailBy();
        return gb == null || "message".equals(gb);
    }


    public String getSkin() { return get(Provisioning.A_zimbraPrefSkin); }
    
    public String getDedupeMessagesSentToSelf() { return get(Provisioning.A_zimbraPrefDedupeMessagesSentToSelf); }

    public String getMailInitialSearch() { return get(Provisioning.A_zimbraPrefMailInitialSearch); }

    public String getNewMailNotificationAddress() { return get(Provisioning.A_zimbraPrefNewMailNotificationAddress); }

    public String getMailForwardingAddress() { return get(Provisioning.A_zimbraPrefMailForwardingAddress); }

    public String getOutOfOfficeReply() { return get(Provisioning.A_zimbraPrefOutOfOfficeReply); }

	public String getOutOfOfficeFromDate() { return get(Provisioning.A_zimbraPrefOutOfOfficeFromDate); }

	public String getOutOfOfficeUntilDate() { return get(Provisioning.A_zimbraPrefOutOfOfficeUntilDate); }

    public String getMailSignature() { return get(Provisioning.A_zimbraPrefMailSignature); }

    public long getMailItemsPerPage() { return getLong(Provisioning.A_zimbraPrefMailItemsPerPage); }

    public long getContactsPerPage() { return getLong(Provisioning.A_zimbraPrefContactsPerPage); }

	public long getVoiceItemsPerPage() { return getLong(Provisioning.A_zimbraPrefVoiceItemsPerPage); }

    public long getCalendarFirstDayOfWeek() { return getLong(Provisioning.A_zimbraPrefCalendarFirstDayOfWeek); }

    public String getInboxUnreadLifetime() { return get(Provisioning.A_zimbraPrefInboxUnreadLifetime); }

    public String getInboxReadLifetime() { return get(Provisioning.A_zimbraPrefInboxReadLifetime); }

    public String getSentLifetime() { return get(Provisioning.A_zimbraPrefSentLifetime); }

    public String getJunkLifetime() { return get(Provisioning.A_zimbraPrefJunkLifetime); }

    public String getTrashLifetime() { return get(Provisioning.A_zimbraPrefTrashLifetime); }

    public boolean getDisplayExternalImages() { return getBool(Provisioning.A_zimbraPrefDisplayExternalImages); }

    public long getCalendarDayHourStart() {
        long hour = getLong(Provisioning.A_zimbraPrefCalendarDayHourStart);
        return hour == -1 ? 8 : hour;
    }

    public long getCalendarDayHourEnd() {
         long hour = getLong(Provisioning.A_zimbraPrefCalendarDayHourEnd);
        return hour == -1 ? 18 : hour;
    }

    public String getCalendarInitialView() { return get(Provisioning.A_zimbraPrefCalendarInitialView); }

    public String getTimeZoneId() { return get(Provisioning.A_zimbraPrefTimeZoneId); }

    public String getTimeZoneCanonicalId() { return TZIDMapper.canonicalize(get(Provisioning.A_zimbraPrefTimeZoneId)); }

    public String getDefaultPrintFontSize() {return get(Provisioning.A_zimbraPrefDefaultPrintFontSize);}

    private TimeZone mCachedTimeZone;
    private String mCachedTimeZoneId;

    public synchronized TimeZone getTimeZone() {
        if (mCachedTimeZone == null || (mCachedTimeZoneId != null && !mCachedTimeZoneId.equals(getTimeZoneId()))) {
            mCachedTimeZoneId = getTimeZoneId();
            mCachedTimeZone  = (mCachedTimeZoneId == null) ? null :
                    TimeZone.getTimeZone(TZIDMapper.canonicalize(mCachedTimeZoneId));
            if (mCachedTimeZone == null)
                mCachedTimeZone = TimeZone.getDefault();
        }
        return mCachedTimeZone;
    }

    public String getReplyIncludeOriginalText() { return get(Provisioning.A_zimbraPrefReplyIncludeOriginalText); }

    public boolean getReplyIncludeAsAttachment() { return "includeAsAttachment".equals(getReplyIncludeOriginalText()); }
    public boolean getReplyIncludeBody() { return "includeBody".equals(getReplyIncludeOriginalText()); }
    public boolean getReplyIncludeBodyWithPrefx() { return "includeBodyWithPrefix".equals(getReplyIncludeOriginalText()); }
    public boolean getReplyIncludeNone() { return "includeNone".equals(getReplyIncludeOriginalText()); }
    public boolean getReplyIncludeSmart() { return "includeSmart".equals(getReplyIncludeOriginalText()); }
    
    public String getForwardIncludeOriginalText() { return get(Provisioning.A_zimbraPrefForwardIncludeOriginalText); }
    public boolean getForwardIncludeAsAttachment() { return "includeAsAttachment".equals(getForwardIncludeOriginalText()); }
    public boolean getForwardIncludeBody() { return "includeBody".equals(getForwardIncludeOriginalText()); }
    public boolean getForwardIncludeBodyWithPrefx() { return "includeBodyWithPrefix".equals(getForwardIncludeOriginalText()); }
    
    public String getForwardReplyFormat() { return get(Provisioning.A_zimbraPrefForwardReplyFormat); }
    public boolean getForwardReplyTextFormat() { return "text".equals(getForwardReplyFormat()); }
    public boolean getForwardReplyHtmlFormat() { return "html".equals(getForwardReplyFormat()); }
    public boolean getForwardReplySameFormat() { return "same".equals(getForwardReplyFormat()); }
    public boolean getForwardReplyInOriginalFormat() { return getBool(Provisioning.A_zimbraPrefForwardReplyInOriginalFormat); }


    public String getForwardReplyPrefixChar() { return get(Provisioning.A_zimbraPrefForwardReplyPrefixChar); }

    public String getCalendarReminderDuration1() { return get(Provisioning.A_zimbraPrefCalendarReminderDuration1); }
    public String getCalendarReminderDuration2() { return get(Provisioning.A_zimbraPrefCalendarReminderDuration2); }
    public String getCalendarReminderEmail() { return get(Provisioning.A_zimbraPrefCalendarReminderEmail); }
    public boolean getCalendarReminderSendEmail() { return getBool(Provisioning.A_zimbraPrefCalendarReminderSendEmail); }
    public boolean getCalendarReminderMobile() { return getBool(Provisioning.A_zimbraPrefCalendarReminderMobile); }
    public boolean getCalendarReminderYMessenger() { return getBool(Provisioning.A_zimbraPrefCalendarReminderYMessenger); }

	public String getPop3DownloadSince() { return get(Provisioning.A_zimbraPrefPop3DownloadSince); }
}
