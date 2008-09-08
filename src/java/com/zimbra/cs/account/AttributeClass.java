/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
/**
 * 
 */
package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.Map;

public enum AttributeClass {
    mailRecipient("zimbraMailRecipient"), 
    account("zimbraAccount"), 
    alias("zimbraAlias"), 
    distributionList("zimbraDistributionList"), 
    cos("zimbraCOS"), 
    globalConfig("zimbraGlobalConfig"), 
    domain("zimbraDomain"),
    securityGroup("zimbraSecurityGroup"), 
    server("zimbraServer"), 
    mimeEntry("zimbraMimeEntry"), 
    objectEntry("zimbraObjectEntry"), 
    timeZone("zimbraTimeZone"), 
    zimletEntry("zimbraZimletEntry"),
    calendarResource("zimbraCalendarResource"), 
    identity("zimbraIdentity"), 
    dataSource("zimbraDataSource"), 
    pop3DataSource("zimbraPop3DataSource"), 
    imapDataSource("zimbraImapDataSource"),
    signature("zimbraSignature"),
    xmppComponent("zimbraXMPPComponent")
    ;
    
    private static class TM {
        static Map<String, AttributeClass> sOCMap = new HashMap<String, AttributeClass>();
    }
    
    String mOCName;
    
    AttributeClass(String ocName) {
        mOCName = ocName;
        TM.sOCMap.put(ocName, this);
    }
    
    public static AttributeClass getAttributeClass(String ocName) {
        return TM.sOCMap.get(ocName);
    }
    
    public String getOCName() {
        return mOCName;
    }
}
