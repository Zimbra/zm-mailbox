/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;

/**
 * 
 */
public class LocalizedSortBy extends SortBy {
    LocalizedSortBy(Type t, String str, SortCriterion criterion, SortDirection direction,
                    Locale locale) {
        super(t,str,criterion,direction);
        mLocale = locale;
        System.out.println(mLocale);
    }
    
    public Comparator<ZimbraHit> getZimbraHitComparator() {
        Collator col = Collator.getInstance(mLocale);
        return new NameComparator(col);
    };
    
    private Locale mLocale;
    
    private class NameComparator implements Comparator<ZimbraHit> {
        private Collator mCol;
        NameComparator(Collator col) {
            mCol = col;
        }
        public int compare(ZimbraHit lhs, ZimbraHit rhs) {
            int toRet = 0;            
            try {
                toRet = mCol.compare(lhs.getName(), rhs.getName());
                if (toRet == 0) {
                    toRet = lhs.getItemId() - rhs.getItemId();
                }
                if (isDescending()) {
                    toRet = -1 * toRet;
                }
            } catch (ServiceException e) {
                ZimbraLog.index.debug("Caught exception "+e+" trying to compare "
                                      +lhs+" to "+rhs, e);
                toRet = 0;
            }
            return toRet;
        }
    }
    
    public static class TestHit extends ZimbraHit
    {
        private String mName;
        private int mItemId;
        TestHit(String name, int id) {
            super(null, null, 0);
            mName = name;
            mItemId = id;
        }

        @Override
        int getConversationId() throws ServiceException {
            return 0;
        }

        @Override
        long getDate() throws ServiceException {
            return 0;
        }

        @Override
        public int getItemId() throws ServiceException {
            return mItemId;
        }

        @Override
        public MailItem getMailItem() throws ServiceException {
            return null;
        }

        @Override
        String getName() throws ServiceException {
            return mName;
        }

        @Override
        long getSize() throws ServiceException {
            return 0;
        }

        @Override
        String getSubject() throws ServiceException {
            return null;
        }

        @Override
        boolean itemIsLoaded() throws ServiceException {
            return false;
        }

        @Override
        void setItem(MailItem item) throws ServiceException {
        }
        
        public String toString() {
            return mName+","+mItemId;
        }
    }
}
