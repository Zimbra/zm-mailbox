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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import com.zimbra.cs.account.Provisioning;

import java.util.List;
import java.util.Map;

public class ZPrefs {

    private Map<String, List<String>> mPrefs;

    public ZPrefs(Map<String, List<String>> prefs) {
        mPrefs = prefs;
    }

    /**
     * @param name name of pref to get
     * @return null if unset, or first value in list
     */
    public String get(String name) {
        List<String> value = mPrefs.get(name);
        return (value == null || value.isEmpty()) ? null : value.get(0);

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

    public Map<String, List<String>> getPrefs() { return mPrefs; }

    public boolean getReadingPaneEnabled() { return getBool(Provisioning.A_zimbraPrefReadingPaneEnabled); }
    
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

    public String getMailSignature() { return get(Provisioning.A_zimbraPrefMailSignature); }

    public long getMailItemsPerPage() { return getLong(Provisioning.A_zimbraPrefMailItemsPerPage); }

    public long getContactsPerPage() { return getLong(Provisioning.A_zimbraPrefContactsPerPage); }

    public long getCalendarFirstDayOfWeek() { return getLong(Provisioning.A_zimbraPrefCalendarFirstDayOfWeek); }

}

