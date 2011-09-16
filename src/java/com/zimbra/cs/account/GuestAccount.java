/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 VMware, Inc.
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


public class GuestAccount extends Account {
    private String mDigest;     // for guest grantee
    private String mAccessKey;  // for key grantee
    
    public static final Account ANONYMOUS_ACCT = new GuestAccount("public", null);
    
    /** The pseudo-GUID signifying "all authenticated and unauthenticated users". */
    public static final String GUID_PUBLIC   = "99999999-9999-9999-9999-999999999999";
    
    /** The pseudo-GUID signifying "all authenticated users". */
    public static final String GUID_AUTHUSER = "00000000-0000-0000-0000-000000000000";
    
    private static Map<String, Object> getAnonAttrs() {
        Map<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_uid, "public");
        attrs.put(Provisioning.A_zimbraId, GuestAccount.GUID_PUBLIC);
        return attrs;
    }
    
    public GuestAccount(String emailAddress, String password) {
        super(emailAddress, GuestAccount.GUID_PUBLIC, getAnonAttrs(), null, null);
        mDigest = AuthToken.generateDigest(emailAddress, password);
    }
    
    public GuestAccount(AuthToken auth) {
        // for key grantee type, sometimes there could be no email address
        super(auth.getExternalUserEmail()==null?"":auth.getExternalUserEmail(), GuestAccount.GUID_PUBLIC, getAnonAttrs(), null, null);
        mDigest = auth.getDigest();
        mAccessKey = auth.getAccessKey();
    }
    
    public boolean matches(String emailAddress, String password) {
        if (getName().compareTo(emailAddress) != 0)
            return false;
        String digest = AuthToken.generateDigest(emailAddress, password);
        return (mDigest.compareTo(digest) == 0);
    }
    
    public boolean matchesAccessKey(String emailAddress, String accesskey) {
        /* do not verify emailAddress for key grantees
        if (getName().compareTo(emailAddress) != 0)
            return false;
        */    
        if (mAccessKey == null)
            return false;
        return (mAccessKey.compareTo(accesskey) == 0);
    }
    
    public String getDigest() {
        return mDigest;
    }
    
    public String getAccessKey() {
        return mAccessKey;
    }

}