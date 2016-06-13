/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client;

import java.util.Collection;
import java.util.Map;
import java.util.TimeZone;

import com.google.common.collect.Iterables;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.calendar.TZIDMapper;

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
        if (values == null || values.isEmpty()) {
            return null;
        }
        return Iterables.get(values, 0);
    }

    public boolean getBool(String name) {
        return ProvisioningConstants.TRUE.equals(get(name));
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

    public String getAppleiCalDelegationEnabled() { return get(ZAttrProvisioning.A_zimbraPrefAppleIcalDelegationEnabled); }
    
    public String getComposeFormat() { return get(ZAttrProvisioning.A_zimbraPrefComposeFormat); }

    public String getHtmlEditorDefaultFontFamily() { return get(ZAttrProvisioning.A_zimbraPrefHtmlEditorDefaultFontFamily); }

    public String getHtmlEditorDefaultFontSize() { return get(ZAttrProvisioning.A_zimbraPrefHtmlEditorDefaultFontSize); }

    public String getHtmlEditorDefaultFontColor() { return get(ZAttrProvisioning.A_zimbraPrefHtmlEditorDefaultFontColor); }

    public String getLocale() { return get(ZAttrProvisioning.A_zimbraPrefLocale); }

    public String getYintl() { return get(ZAttrProvisioning.A_zimbraPrefLocale); }

    public boolean getUseTimeZoneListInCalendar() { return getBool(ZAttrProvisioning.A_zimbraPrefUseTimeZoneListInCalendar); }

    public boolean getReadingPaneEnabled() { return getBool(ZAttrProvisioning.A_zimbraPrefReadingPaneEnabled); }

    public String getReadingPaneLocation() { return get(ZAttrProvisioning.A_zimbraPrefReadingPaneLocation); }

    public boolean getMailSignatureEnabled() { return getBool(ZAttrProvisioning.A_zimbraPrefMailSignatureEnabled); }

    public boolean getIncludeSpamInSearch() { return getBool(ZAttrProvisioning.A_zimbraPrefIncludeSpamInSearch); }

    public boolean getIncludeTrashInSearch() { return getBool(ZAttrProvisioning.A_zimbraPrefIncludeTrashInSearch); }

    public boolean getShowSearchString() { return getBool(ZAttrProvisioning.A_zimbraPrefShowSearchString); }

    public boolean getShowFragments() { return getBool(ZAttrProvisioning.A_zimbraPrefShowFragments); }

    public boolean getSaveToSent() { return getBool(ZAttrProvisioning.A_zimbraPrefSaveToSent); }

    public boolean getOutOfOfficeReplyEnabled() { return getBool(ZAttrProvisioning.A_zimbraPrefOutOfOfficeReplyEnabled); }

    public boolean getNewMailNotificationsEnabled() { return getBool(ZAttrProvisioning.A_zimbraPrefNewMailNotificationEnabled); }

    public boolean getMailLocalDeliveryDisabled() { return getBool(ZAttrProvisioning.A_zimbraPrefMailLocalDeliveryDisabled); }

    public boolean getMessageViewHtmlPreferred() { return getBool(ZAttrProvisioning.A_zimbraPrefMessageViewHtmlPreferred); }

    public boolean getAutoAddAddressEnabled() { return getBool(ZAttrProvisioning.A_zimbraPrefAutoAddAddressEnabled); }

    public String getShortcuts() { return get(ZAttrProvisioning.A_zimbraPrefShortcuts); }

    public boolean getUseKeyboardShortcuts() { return getBool(ZAttrProvisioning.A_zimbraPrefUseKeyboardShortcuts); }

    public boolean getSignatureEnabled() { return getBool(ZAttrProvisioning.A_zimbraPrefMailSignatureEnabled); }

    public String getClientType() { return get(ZAttrProvisioning.A_zimbraPrefClientType); }
    public boolean getIsAdvancedClient() { return "advanced".equals(getClientType()); }
    public boolean getIsStandardClient() { return "standard".equals(getClientType()); }
    
    public String getSignatureStyle() { return get(ZAttrProvisioning.A_zimbraPrefMailSignatureStyle); }
    public boolean getSignatureStyleTop() { return "outlook".equals(getSignatureStyle()); }
    public boolean getSignatureStyleBottom() { return "internet".equals(getSignatureStyle()); }

    public String getGroupMailBy() { return get(ZAttrProvisioning.A_zimbraPrefGroupMailBy); }

    public boolean getGroupByConversation() {
        String gb = getGroupMailBy();
        return "conversation".equals(gb);
    }

    public boolean getGroupByMessage() {
        String gb = getGroupMailBy();
        return gb == null || "message".equals(gb);
    }


    public String getSkin() { return get(ZAttrProvisioning.A_zimbraPrefSkin); }
    
    public String getDedupeMessagesSentToSelf() { return get(ZAttrProvisioning.A_zimbraPrefDedupeMessagesSentToSelf); }

    public String getMailInitialSearch() { return get(ZAttrProvisioning.A_zimbraPrefMailInitialSearch); }

    public String getNewMailNotificationAddress() { return get(ZAttrProvisioning.A_zimbraPrefNewMailNotificationAddress); }

    public String getMailForwardingAddress() { return get(ZAttrProvisioning.A_zimbraPrefMailForwardingAddress); }

    public String getOutOfOfficeReply() { return get(ZAttrProvisioning.A_zimbraPrefOutOfOfficeReply); }

	public String getOutOfOfficeFromDate() { return get(ZAttrProvisioning.A_zimbraPrefOutOfOfficeFromDate); }

	public String getOutOfOfficeUntilDate() { return get(ZAttrProvisioning.A_zimbraPrefOutOfOfficeUntilDate); }

    public String getMailSignature() { return get(ZAttrProvisioning.A_zimbraPrefMailSignature); }

    public long getMailItemsPerPage() { return getLong(ZAttrProvisioning.A_zimbraPrefMailItemsPerPage); }

    public long getContactsPerPage() { return getLong(ZAttrProvisioning.A_zimbraPrefContactsPerPage); }

	public long getVoiceItemsPerPage() { return getLong(ZAttrProvisioning.A_zimbraPrefVoiceItemsPerPage); }

    public long getCalendarFirstDayOfWeek() { return getLong(ZAttrProvisioning.A_zimbraPrefCalendarFirstDayOfWeek); }

    public String getCalendarWorkingHours() { return get(ZAttrProvisioning.A_zimbraPrefCalendarWorkingHours);}

    public String getInboxUnreadLifetime() { return get(ZAttrProvisioning.A_zimbraPrefInboxUnreadLifetime); }

    public String getInboxReadLifetime() { return get(ZAttrProvisioning.A_zimbraPrefInboxReadLifetime); }

    public String getSentLifetime() { return get(ZAttrProvisioning.A_zimbraPrefSentLifetime); }

    public String getJunkLifetime() { return get(ZAttrProvisioning.A_zimbraPrefJunkLifetime); }

    public String getTrashLifetime() { return get(ZAttrProvisioning.A_zimbraPrefTrashLifetime); }

    public boolean getDisplayExternalImages() { return getBool(ZAttrProvisioning.A_zimbraPrefDisplayExternalImages); }

    public long getCalendarDayHourStart() {
        long hour = getLong(ZAttrProvisioning.A_zimbraPrefCalendarDayHourStart);
        return hour == -1 ? 8 : hour;
    }

    public long getCalendarDayHourEnd() {
         long hour = getLong(ZAttrProvisioning.A_zimbraPrefCalendarDayHourEnd);
        return hour == -1 ? 18 : hour;
    }

    public String getCalendarInitialView() { return get(ZAttrProvisioning.A_zimbraPrefCalendarInitialView); }

    public String getTimeZoneId() { return get(ZAttrProvisioning.A_zimbraPrefTimeZoneId); }

    public String getTimeZoneCanonicalId() { return TZIDMapper.canonicalize(get(ZAttrProvisioning.A_zimbraPrefTimeZoneId)); }

    public String getDefaultPrintFontSize() {return get(ZAttrProvisioning.A_zimbraPrefDefaultPrintFontSize);}

    public String getFolderTreeOpen() {return get(ZAttrProvisioning.A_zimbraPrefFolderTreeOpen);}

    public String getSearchTreeOpen() {return get(ZAttrProvisioning.A_zimbraPrefSearchTreeOpen);}

    public String getTagTreeOpen() {return get(ZAttrProvisioning.A_zimbraPrefTagTreeOpen);}

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

    public String getReplyIncludeOriginalText() { return get(ZAttrProvisioning.A_zimbraPrefReplyIncludeOriginalText); }

    public boolean getReplyIncludeAsAttachment() { return "includeAsAttachment".equals(getReplyIncludeOriginalText()); }
    public boolean getReplyIncludeBody() { return "includeBody".equals(getReplyIncludeOriginalText()) || "includeBodyAndHeaders".equals(getReplyIncludeOriginalText()); }
    public boolean getReplyIncludeBodyWithPrefx() { return "includeBodyWithPrefix".equals(getReplyIncludeOriginalText()) || "includeBodyAndHeadersWithPrefix".equals(getReplyIncludeOriginalText()); }
    public boolean getReplyIncludeNone() { return "includeNone".equals(getReplyIncludeOriginalText()); }
    public boolean getReplyIncludeSmart() { return "includeSmart".equals(getReplyIncludeOriginalText()); }
    
    public String getForwardIncludeOriginalText() { return get(ZAttrProvisioning.A_zimbraPrefForwardIncludeOriginalText); }
    public boolean getForwardIncludeAsAttachment() { return "includeAsAttachment".equals(getForwardIncludeOriginalText()); }
    public boolean getForwardIncludeBody() { return "includeBody".equals(getForwardIncludeOriginalText()) || "includeBodyAndHeaders".equals(getForwardIncludeOriginalText()); }
    public boolean getForwardIncludeBodyWithPrefx() { return "includeBodyWithPrefix".equals(getForwardIncludeOriginalText()) || "includeBodyAndHeadersWithPrefix".equals(getForwardIncludeOriginalText()); }
    
    public String getForwardReplyFormat() { return get(ZAttrProvisioning.A_zimbraPrefForwardReplyFormat); }
    public boolean getForwardReplyTextFormat() { return "text".equals(getForwardReplyFormat()); }
    public boolean getForwardReplyHtmlFormat() { return "html".equals(getForwardReplyFormat()); }
    public boolean getForwardReplySameFormat() { return "same".equals(getForwardReplyFormat()); }
    public boolean getForwardReplyInOriginalFormat() { return getBool(ZAttrProvisioning.A_zimbraPrefForwardReplyInOriginalFormat); }


    public String getForwardReplyPrefixChar() { return get(ZAttrProvisioning.A_zimbraPrefForwardReplyPrefixChar); }

    public String getCalendarReminderDuration1() { return get(ZAttrProvisioning.A_zimbraPrefCalendarReminderDuration1); }
    public String getCalendarReminderDuration2() { return get(ZAttrProvisioning.A_zimbraPrefCalendarReminderDuration2); }
    public String getCalendarReminderEmail() { return get(ZAttrProvisioning.A_zimbraPrefCalendarReminderEmail); }
    public boolean getCalendarReminderSendEmail() { return getBool(ZAttrProvisioning.A_zimbraPrefCalendarReminderSendEmail); }
    public boolean getCalendarReminderMobile() { return getBool(ZAttrProvisioning.A_zimbraPrefCalendarReminderMobile); }
    public boolean getCalendarReminderYMessenger() { return getBool(ZAttrProvisioning.A_zimbraPrefCalendarReminderYMessenger); }
    public boolean getCalendarShowDeclinedMeetings() { return getBool(ZAttrProvisioning.A_zimbraPrefCalendarShowDeclinedMeetings); }


	public String getPop3DownloadSince() { return get(ZAttrProvisioning.A_zimbraPrefPop3DownloadSince); }
}
