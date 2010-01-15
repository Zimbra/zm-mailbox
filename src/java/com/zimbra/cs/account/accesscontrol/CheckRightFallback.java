/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;

public abstract class CheckRightFallback {
    protected Right mRight;

    void setRight(Right right) {
        mRight = right;
    }
    
    public Boolean checkRight(Account authedAcct, Entry target, boolean asAdmin) {
        try {
            return doCheckRight(authedAcct, target, asAdmin);
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("caught exception in checkRight fallback" +
                    ", checkRight fallback for right [" + mRight.getName() +"] skipped", e);
            return null;
        }
    }
    
    protected abstract Boolean doCheckRight(Account grantee, Entry target, boolean asAdmin) throws ServiceException;
    
}
