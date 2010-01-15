/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.util;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailServiceException;

/**
 * A short identifier which identifies the synchronization state between a client and the server.
 * 
 * The sync token can have two forms:
 * 
 * 1)   "INTEGER"  -- this is the highest change ID the client knows about (see Mailbox.getLastChangeID)
 * 
 *    OR
 *    
 * 2)   "INTEGER-INTEGER" 
 *         -- the first integer is the highest change ID that the client has *all* the data for
 *         -- the second integer is the highest item id in the NEXT CHANGE ID that the client has data for
 *         
 *         e.g. "4-32" means "I have all of change 4, AND I have up to item 32 in change 5" 
 *    
 */
public class SyncToken {
    private int mChangeId;
    private int mOffsetInNext = -1;
    
    public SyncToken(int changeid) {
        assert(changeid >= 0);
        mChangeId = changeid;
    }
    
    public SyncToken(int changeid, int offsetInNextChange) throws ServiceException {
        assert(changeid >= 0 && offsetInNextChange >= 0);
        mChangeId = changeid;
        mOffsetInNext = offsetInNextChange;
    }
    
    public SyncToken(String s) throws ServiceException {
        int idx = s.indexOf('-'); 
        if (idx < 0) {
            mChangeId = Integer.parseInt(s);
        } else {
            if (idx == s.length()-1) 
                throw MailServiceException.INVALID_SYNC_TOKEN(s);
            String lhs = s.substring(0, idx);
            mChangeId = Integer.parseInt(lhs);
            String rhs = s.substring(idx+1);
            mOffsetInNext = Integer.parseInt(rhs);
            if (mOffsetInNext < 0)
                throw MailServiceException.INVALID_SYNC_TOKEN(s);
        }
    }
    
    public int getChangeId() { return mChangeId; }
    public boolean hasOffsetInNext() { return mOffsetInNext > 0; }
    public int getOffsetInNext() { return mOffsetInNext; }
    public String toString() {
        if (mOffsetInNext < 0) {
            return Integer.toString(mChangeId);
        } else {
            return new StringBuilder(mChangeId).append('-').append(mOffsetInNext).toString();
        }
    }
    
    /**
     * TRUE if this syncToken is AFTER or UP-TO-DATE with the passed-in token
     * 
     * @param changeId
     * @return
     */
    public boolean after(int changeId) {
        return mChangeId >= changeId; 
    }
    public boolean after(int changeId, int offset) {
        if (mChangeId < changeId)
            return false;
        if (mChangeId > changeId)
            return true;
        return (mOffsetInNext >= offset);
    }
}
