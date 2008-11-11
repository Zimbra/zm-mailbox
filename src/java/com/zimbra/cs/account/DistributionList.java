/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;

public class DistributionList extends ZAttrDistributionList {
    
    /*
     * This is for sanity checking purpose.
     * 
     * Certain calls on DistributionList should only be made if the DistributionList
     * object was obtained from prov.getAclGroup, *not* prov.get(DistributionListBy), 
     * *not* prov.searchObjects.   Some calls are vice versa.
     * 
     * mIsAclGroup is true if this object was loaded by prov.getAclGroup.
     */
    boolean mIsAclGroup;
    
    protected DistributionList(String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(name, id, attrs, prov);
    }

    public String[] getAllMembers() throws ServiceException {
        if (mIsAclGroup)
            throw ServiceException.FAILURE("internal error", null);
        
        return getMultiAttr(Provisioning.A_zimbraMailForwardingAddress);
    }
    
    public String[] getAliases() throws ServiceException {
        if (mIsAclGroup)
            throw ServiceException.FAILURE("internal error", null);
        
        return getMultiAttr(Provisioning.A_zimbraMailAlias);
    }
    
    @Override
    protected void resetData() {
        super.resetData();
        if (mIsAclGroup)
            trimForAclGroup();
    }

    private void trimForAclGroup() {
        /*
         * Hack.
         * 
         * We do not want to cache zimbraMailAlias/zimbraMailForwardingAddress, they can be big.
         * zimbraMailAlias was loaded for computing the upward membership and is now no longer 
         * needed.  Remove it before caching.
         */ 
        Map<String, Object> attrs = getAttrs(false);
        attrs.remove(Provisioning.A_zimbraMailAlias);
        attrs.remove(Provisioning.A_zimbraMailForwardingAddress);
    }
    
    public boolean isAclGroup() {
        return mIsAclGroup;
    }
    
    public void turnToAclGroup() {
        mIsAclGroup = true;
        trimForAclGroup();
    }
}
