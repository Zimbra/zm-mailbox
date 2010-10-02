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

import com.google.common.collect.Iterables;
import com.zimbra.cs.account.Provisioning;

public class ZFeatures {

    private Map<String, Collection<String>> mAttrs;

    public ZFeatures(Map<String, Collection<String>> attrs) {
        mAttrs = attrs;
    }

    /**
     * @param name name of attr to get
     * @return null if unset, or first value in list
     */
    private String get(String name) {
        Collection<String> value = mAttrs.get(name);
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Iterables.get(value, 0);

    }

    public boolean getBool(String name) {
        return Provisioning.TRUE.equals(get(name));
    }

    public Map<String, Collection<String>> getAttrs() { return mAttrs; }
    
    public boolean getContacts() { return getBool(Provisioning.A_zimbraFeatureContactsEnabled); }

    public boolean getMail() { return getBool(Provisioning.A_zimbraFeatureMailEnabled); }

    public boolean getVoice() { return getBool(Provisioning.A_zimbraFeatureVoiceEnabled); }

    public boolean getCalendar() { return getBool(Provisioning.A_zimbraFeatureCalendarEnabled); }

    public boolean getCalendarUpsell() { return getBool(Provisioning.A_zimbraFeatureCalendarUpsellEnabled); }

    public String getCalendarUpsellURL() { return get(Provisioning.A_zimbraFeatureCalendarUpsellURL); }    

    public boolean getTasks() { return getBool(Provisioning.A_zimbraFeatureTasksEnabled); }

    public boolean getTagging() { return getBool(Provisioning.A_zimbraFeatureTaggingEnabled); }

    public boolean getOptions() { return getBool(Provisioning.A_zimbraFeatureOptionsEnabled); }
    
    public boolean getAdvancedSearch() { return getBool(Provisioning.A_zimbraFeatureAdvancedSearchEnabled); }

    public boolean getSavedSearches() { return getBool(Provisioning.A_zimbraFeatureSavedSearchesEnabled); }

    public boolean getConversations() { return getBool(Provisioning.A_zimbraFeatureConversationsEnabled); }

    public boolean getChangePassword() { return getBool(Provisioning.A_zimbraFeatureChangePasswordEnabled); }

    public boolean getInitialSearchPreference() { return getBool(Provisioning.A_zimbraFeatureInitialSearchPreferenceEnabled); }

    public boolean getFilters() { return getBool(Provisioning.A_zimbraFeatureFiltersEnabled); }

    public boolean getGal() { return getBool(Provisioning.A_zimbraFeatureGalEnabled); }

    public boolean getHtmlCompose() { return getBool(Provisioning.A_zimbraFeatureHtmlComposeEnabled); }

    public boolean getIM() { return getBool(Provisioning.A_zimbraFeatureIMEnabled); }

    public boolean getViewInHtml() { return getBool(Provisioning.A_zimbraFeatureViewInHtmlEnabled); }

    public boolean getSharing() { return getBool(Provisioning.A_zimbraFeatureSharingEnabled); }

    public boolean getMailForwarding() { return getBool(Provisioning.A_zimbraFeatureMailForwardingEnabled); }

    public boolean getMailForwardingInFilter() { return getBool(Provisioning.A_zimbraFeatureMailForwardingInFiltersEnabled); }

    public boolean getMobileSync() { return getBool(Provisioning.A_zimbraFeatureMobileSyncEnabled); }

    public boolean getSkinChange() { return getBool(Provisioning.A_zimbraFeatureSkinChangeEnabled); }

    public boolean getNotebook() { return getBool(Provisioning.A_zimbraFeatureNotebookEnabled); }

    public boolean getBriefcases() { return getBool(Provisioning.A_zimbraFeatureBriefcasesEnabled); }

    public boolean getGalAutoComplete() { return getBool(Provisioning.A_zimbraFeatureGalAutoCompleteEnabled); }

    public boolean getOutOfOfficeReply() { return getBool(Provisioning.A_zimbraFeatureOutOfOfficeReplyEnabled); }

    public boolean getNewMailNotification() { return getBool(Provisioning.A_zimbraFeatureNewMailNotificationEnabled); }

    public boolean getIdentities() { return getBool(Provisioning.A_zimbraFeatureIdentitiesEnabled); }

    public boolean getPop3DataSource() { return getBool(Provisioning.A_zimbraFeaturePop3DataSourceEnabled); }
    
    public boolean getGroupcalendarEnabled() { return getBool(Provisioning.A_zimbraFeatureGroupCalendarEnabled); }

    public boolean getFlagging() { return getBool(Provisioning.A_zimbraFeatureFlaggingEnabled); }

    public boolean getMailPriority() { return getBool(Provisioning.A_zimbraFeatureMailPriorityEnabled); }

    public boolean getPortalEnabled() { return getBool(Provisioning.A_zimbraFeaturePortalEnabled); }
    
    // defaults to TRUE
    public boolean getWebSearchEnabled() { return get(Provisioning.A_zimbraFeatureWebSearchEnabled) == null ||
    											  getBool(Provisioning.A_zimbraFeatureWebSearchEnabled); }

    // defaults to TRUE
    public boolean getWebClientShowOfflineLink() { return get(Provisioning.A_zimbraWebClientShowOfflineLink) == null ||
                                                          getBool(Provisioning.A_zimbraWebClientShowOfflineLink); }

	// defaults to TRUE
	public boolean getNewAddrBookEnabled() { return get(Provisioning.A_zimbraFeatureNewAddrBookEnabled) == null ||
													getBool(Provisioning.A_zimbraFeatureNewAddrBookEnabled); }
	// defaults to TRUE
	public boolean getPop3Enabled() { return	get(Provisioning.A_zimbraPop3Enabled) == null ||
												getBool(Provisioning.A_zimbraPop3Enabled); }
}

