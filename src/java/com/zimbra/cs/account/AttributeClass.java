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
/**
 * 
 */
package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.accesscontrol.Right.RightType;

public enum AttributeClass {
    mailRecipient("zimbraMailRecipient",        false), 
    account("zimbraAccount",                    true), 
    alias("zimbraAlias",                        true), 
    distributionList("zimbraDistributionList",  true), 
    cos("zimbraCOS",                            true), 
    globalConfig("zimbraGlobalConfig",          true), 
    domain("zimbraDomain",                      true),
    securityGroup("zimbraSecurityGroup",        false), 
    server("zimbraServer",                      true), 
    mimeEntry("zimbraMimeEntry",                true), 
    objectEntry("zimbraObjectEntry",            false), 
    timeZone("zimbraTimeZone",                  false), 
    zimletEntry("zimbraZimletEntry",            true),
    calendarResource("zimbraCalendarResource",  true), 
    identity("zimbraIdentity",                  true), 
    dataSource("zimbraDataSource",              true), 
    pop3DataSource("zimbraPop3DataSource",      true), 
    imapDataSource("zimbraImapDataSource",      true),
    rssDataSource("zimbraRssDataSource",        true),
    liveDataSource("zimbraLiveDataSource",      true),
    galDataSource("zimbraGalDataSource",        true),
    signature("zimbraSignature",                true),
    xmppComponent("zimbraXMPPComponent",        true),
    aclTarget("zimbraAclTarget",                true),
    group("zimbraGroup",                        false);
    
    private static class TM {
        static Map<String, AttributeClass> sOCMap = new HashMap<String, AttributeClass>();
    }
    
    String mOCName;
    boolean mProvisionable;
    
    AttributeClass(String ocName, boolean provisionable) {
        mOCName = ocName;
        mProvisionable = provisionable;
        
        TM.sOCMap.put(ocName, this);
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
    
}
