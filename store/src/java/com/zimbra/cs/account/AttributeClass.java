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
/**
 *
 */
package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry.EntryType;

public enum AttributeClass {
    mailRecipient("zimbraMailRecipient",        false),
    account("zimbraAccount",                    true, EntryType.ACCOUNT),
    alias("zimbraAlias",                        true, EntryType.ALIAS),
    distributionList("zimbraDistributionList",  true, EntryType.DISTRIBUTIONLIST),
    cos("zimbraCOS",                            true, EntryType.COS),
    globalConfig("zimbraGlobalConfig",          true, EntryType.GLOBALCONFIG),
    domain("zimbraDomain",                      true, EntryType.DOMAIN),
    securityGroup("zimbraSecurityGroup",        false),
    server("zimbraServer",                      true, EntryType.SERVER),
    alwaysOnCluster("zimbraAlwaysOnCluster",    true, EntryType.ALWAYSONCLUSTER),
    ucService("zimbraUCService",                true, EntryType.UCSERVICE),
    mimeEntry("zimbraMimeEntry",                true),
    objectEntry("zimbraObjectEntry",            false),
    timeZone("zimbraTimeZone",                  false),
    zimletEntry("zimbraZimletEntry",            true, EntryType.ZIMLET),
    calendarResource("zimbraCalendarResource",  true, EntryType.CALRESOURCE),
    identity("zimbraIdentity",                  true, EntryType.IDENTITY),
    dataSource("zimbraDataSource",              true, EntryType.DATASOURCE),
    pop3DataSource("zimbraPop3DataSource",      true),
    imapDataSource("zimbraImapDataSource",      true),
    rssDataSource("zimbraRssDataSource",        true),
    liveDataSource("zimbraLiveDataSource",      true),
    galDataSource("zimbraGalDataSource",        true),
    signature("zimbraSignature",                true, EntryType.SIGNATURE),
    xmppComponent("zimbraXMPPComponent",        true, EntryType.XMPPCOMPONENT),
    habGroup("zimbraHabGroup",                  true, EntryType.HABGROUP),
    aclTarget("zimbraAclTarget",                true),
    group("zimbraGroup",                        true),
    groupDynamicUnit("zimbraGroupDynamicUnit",  false, EntryType.DYNAMICGROUP_DYNAMIC_UNIT),
    groupStaticUnit("zimbraGroupStaticUnit",    false, EntryType.DYNAMICGROUP_STATIC_UNIT),
    shareLocator("zimbraShareLocator",          true),
    oauth2DataSource("zimbraOauth2DataSource",  true),
    addressList("zimbraAddressList",                  true, EntryType.ADDRESS_LIST);

    public static final String OC_zimbraAccount = account.getOCName();
    public static final String OC_zimbraAclTarget = aclTarget.getOCName();
    public static final String OC_zimbraAlias = alias.getOCName();
    public static final String OC_zimbraCalendarResource = calendarResource.getOCName();
    public static final String OC_zimbraCOS = cos.getOCName();
    public static final String OC_zimbraDataSource = dataSource.getOCName();
    public static final String OC_zimbraDistributionList = distributionList.getOCName();
    public static final String OC_zimbraDomain = domain.getOCName();
    public static final String OC_zimbraGalDataSource = galDataSource.getOCName();
    public static final String OC_zimbraGlobalConfig = globalConfig.getOCName();
    public static final String OC_zimbraGroup = group.getOCName();
    public static final String OC_zimbraGroupDynamicUnit = groupDynamicUnit.getOCName();
    public static final String OC_zimbraGroupStaticUnit = groupStaticUnit.getOCName();
    public static final String OC_zimbraIdentity = identity.getOCName();
    public static final String OC_zimbraImapDataSource = imapDataSource.getOCName();
    public static final String OC_zimbraMailRecipient = mailRecipient.getOCName();
    public static final String OC_zimbraMimeEntry = mimeEntry.getOCName();
    public static final String OC_zimbraPop3DataSource = pop3DataSource.getOCName();
    public static final String OC_zimbraRssDataSource = rssDataSource.getOCName();
    public static final String OC_zimbraServer = server.getOCName();
    public static final String OC_zimbraAlwaysOnCluster = alwaysOnCluster.getOCName();
    public static final String OC_zimbraUCService = ucService.getOCName();
    public static final String OC_zimbraSignature = signature.getOCName();
    public static final String OC_zimbraXMPPComponent = xmppComponent.getOCName();
    public static final String OC_zimbraZimletEntry = zimletEntry.getOCName();
    public static final String OC_zimbraShareLocator = shareLocator.getOCName();
    public static final String OC_zimbraOauth2DataSource = oauth2DataSource.getOCName();
    public static final String OC_zimbraHabGroup = habGroup.getOCName();
    public static final String OC_zimbraAddressList = addressList.getOCName();

    private static class TM {
        static Map<String, AttributeClass> sOCMap = new HashMap<String, AttributeClass>();
    }

    String mOCName;
    boolean mProvisionable;
    EntryType entryType;

    AttributeClass(String ocName, boolean provisionable, EntryType type) {
        mOCName = ocName;
        mProvisionable = provisionable;
        entryType = type;
        TM.sOCMap.put(ocName, this);
    }

    AttributeClass(String ocName, boolean provisionable) {
        this(ocName, provisionable, null);
    }

    public static AttributeClass getAttributeClass(String ocName) {
        return TM.sOCMap.get(ocName);
    }

    public static AttributeClass fromString(String s) throws ServiceException {
        try {
            return AttributeClass.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.PARSE_ERROR("unknown attribute class: " + s, e);
        }
    }

    public String getOCName() {
        return mOCName;
    }

    public boolean isProvisionable() {
        return mProvisionable;
    }

    public EntryType getEntryType() {
        return entryType;
    }

}
