/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 VMware, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbSearch;
import com.zimbra.cs.db.DbSearch.SearchResult;
import com.zimbra.cs.index.IConstraints;
import com.zimbra.cs.index.SortBy;

/**
 * Utility class to temporarily (in HELIX branch) get around package protection for search code that needs a mailbox txn
 * The related code is significantly refactored for IRONMAIDEN, this is just a stopgap to avoid deadlock (see bug 75182)
 */
public class MailboxSearchHelper {
    
    /**
     * Executes a DB search in a mailbox transaction; maintaining correct order of lock -> connect
     */
    public static void fetch(final List<SearchResult> results, final Mailbox mbox,
        final SortBy sort, final int offset, final int size, final IConstraints constraints, final SearchResult.ExtraData extra, final boolean inDumpster) throws ServiceException {
        
        synchronized (mbox) {
            mbox.beginTransaction("searchHelper", null);
            DbSearch.search(results, mbox.getOperationConnection(), constraints, mbox, sort,
                offset, size, extra, inDumpster);
            // convert to MailItem before leaving this transaction
            // otherwise you can poison MailItem cache with stale data
            if (extra == SearchResult.ExtraData.MAIL_ITEM) {
                for (SearchResult result : results) {
                    result.extraData = mbox.toItem((MailItem.UnderlyingData) result.extraData);
                }
            }
            mbox.endTransaction(true);
        }
    }
}
