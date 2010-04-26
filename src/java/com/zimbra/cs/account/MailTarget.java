/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning.DomainBy;

public abstract class MailTarget extends NamedEntry {
    
    protected String mDomain;
    protected String mUnicodeDomain;
    protected String mUnicodeName;
    
    private static final String NO_DOMAIN_ID = "";
    
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
    
    public String getDomainId() {
        String domainId = (String)getCachedData(EntryCacheDataKey.MAILTARGET_DOMAIN_ID);
        
        if (domainId == null) {
            try {
                String dname = getDomainName();
                Provisioning prov = getProvisioning();
                Domain domain =  dname == null ? null : (prov == null ? null : prov.get(DomainBy.name, dname));
                if (domain != null)
                    domainId = domain.getId();
            } catch (ServiceException e) {
                ZimbraLog.account.warn("unable to get domain id for domain " + getDomainName() , e);
            }
            
            if (domainId == null)
                domainId = NO_DOMAIN_ID; // set it to a non-null string so we don't repeatedly look up the domain
            
            setCachedData(EntryCacheDataKey.MAILTARGET_DOMAIN_ID, domainId);
        }
        
        // OK and more efficient to use == instead of equal
        if (domainId == NO_DOMAIN_ID)
            return null;
        else
            return domainId;
    }

}
