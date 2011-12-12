/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.qa.unittest.prov.ldap;

import java.io.File;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.FileUtil;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.RightModifier;

class ACLTestUtil {
    
    static enum AllowOrDeny {
        ALLOW(true, false),
        DELEGABLE(true, true),
        DENY(false, false);
        
        boolean mAllow;
        boolean mDelegable;
        
        AllowOrDeny(boolean allow, boolean delegable) {
            mAllow = allow;
            mDelegable = delegable;
        }
        
        boolean deny() {
            return !mAllow;
        }
        
        boolean allow() {
            return mAllow;
        }
        
        boolean delegable() {
            return mDelegable;
        }
        
        RightModifier toRightModifier() {
            if (deny())
                return RightModifier.RM_DENY;
            else if (delegable())
                return RightModifier.RM_CAN_DELEGATE;
            else
                return null;
        }
    }
    
    static enum GetOrSet {
        GET(true),
        SET(false);
        
        boolean mGet;
        
        GetOrSet(boolean get) {
            mGet = get;
        }
        
        boolean isGet() {
            return mGet;
        }
    }
    
    static Right getRight(String right) throws ServiceException {
        return RightManager.getInstance().getRight(right);
    }
    
    static void installUnitTestRights() throws Exception {
        FileUtil.copy("data/unittest/ldap/rights-unittest.xml", "/opt/zimbra/conf/rights/rights-unittest.xml");
    }
    
    static void uninstallUnitTestRights() throws Exception {
        FileUtil.delete(new File("/opt/zimbra/conf/rights/rights-unittest.xml"));
    }
}
