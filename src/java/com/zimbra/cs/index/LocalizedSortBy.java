/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.index;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
    
    private static void printAll(List<TestHit> test) {
        for (TestHit t: test) {
            System.out.println(t.toString());
        }
    }
    
    public static void main(String args[]) {
        LocalizedSortBy enUSAsc = new LocalizedSortBy(SortBy.Type.NAME_LOCALIZED_ASCENDING,"enUSAsc", 
                                                    SortCriterion.ID, SortDirection.ASCENDING, 
                                                    new Locale("en_US"));
        
        LocalizedSortBy deAsc = new LocalizedSortBy(SortBy.Type.NAME_LOCALIZED_ASCENDING,"deAsc", 
                                                    SortCriterion.ID, SortDirection.ASCENDING, 
                                                    new Locale("de_DE"));
        
        LocalizedSortBy svAsc = new LocalizedSortBy(SortBy.Type.NAME_LOCALIZED_ASCENDING,"svAsc", 
                                                    SortCriterion.ID, SortDirection.ASCENDING, 
                                                    new Locale("sv"));
        
        LocalizedSortBy deDesc = new LocalizedSortBy(SortBy.Type.NAME_LOCALIZED_ASCENDING,"deDesc", 
                                                    SortCriterion.ID, SortDirection.DESCENDING, 
                                                    new Locale("de_DE"));
        
        LocalizedSortBy svDesc = new LocalizedSortBy(SortBy.Type.NAME_LOCALIZED_DESCENDING,"svDesc", 
                                                    SortCriterion.ID, SortDirection.DESCENDING, 
                                                    new Locale("sv"));
        
        
        
        if (false) {
            List<TestHit> test = new ArrayList<TestHit>();
            test.add(new TestHit("Udet", 1));
            test.add(new TestHit("Übelacker", 2));
            test.add(new TestHit("Uell",3));
            test.add(new TestHit("Ülle",4));
            test.add(new TestHit("Ueve",5));
            test.add(new TestHit("Üxküll",6));
            test.add(new TestHit("Uffenbach",7));
            System.out.println("\nSTART:");
            printAll(test);
            
            Collections.sort(test, enUSAsc.getZimbraHitComparator());
            System.out.println("\nAFTER EN_US SORT:");
            printAll(test);
            
            Collections.sort(test, deAsc.getZimbraHitComparator());
            System.out.println("\nAFTER DK_DESC SORT:");
            printAll(test);
        }
        {
            // for Swedish, z<ö, for German ö<z
            List<TestHit> test = new ArrayList<TestHit>();
            test.add(new TestHit("z",3));
            test.add(new TestHit("z",1));
            test.add(new TestHit("z",2));
            test.add(new TestHit("ö",3));
            System.out.println("\nSTART:");
            printAll(test);

            Collections.sort(test, svAsc.getZimbraHitComparator());
            System.out.println("\nAFTER SV_ASC SORT:");
            printAll(test);
            
            Collections.sort(test, svDesc.getZimbraHitComparator());
            System.out.println("\nAFTER SV_DESC SORT:");
            printAll(test);
            
            Collections.sort(test, deAsc.getZimbraHitComparator());
            System.out.println("\nAFTER DK_ASC SORT:");
            printAll(test);
            
            Collections.sort(test, deDesc.getZimbraHitComparator());
            System.out.println("\nAFTER DK_DESC SORT:");
            printAll(test);
        }
    }
}
