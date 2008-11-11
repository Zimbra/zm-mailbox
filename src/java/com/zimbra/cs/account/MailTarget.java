/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.account;

import java.util.Map;

public abstract class MailTarget extends NamedEntry {
    
    protected String mDomain;
    protected String mUnicodeDomain;
    protected String mUnicodeName;
    
    public MailTarget(String name, String id, Map<String,Object> attrs, Map<String, Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);
        int index = name.indexOf('@');
        if (index != -1)  {
            String local = name.substring(0, index);
            mDomain = name.substring(index+1);
            mUnicodeDomain = IDNUtil.toUnicodeDomainName(mDomain);
            mUnicodeName = local + "@" + mUnicodeDomain;
        } else
            mUnicodeName = name;
    }

    /**
     * @return the domain name for this account (foo.com), or null if an admin account. 
     */
    public String getDomainName() {
        return mDomain;
    }
    
    public String getUnicodeDomainName() {
        return mUnicodeDomain;
    }
    
    public String getUnicodeName() {
        return mUnicodeName;
    }

}
