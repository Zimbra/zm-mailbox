/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
