/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Oct 15, 2004
 */
package com.zimbra.cs.index;

import java.util.Comparator;
import java.util.Date;

import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;

public abstract class ZimbraHit 
{
    
    Mailbox mMailbox;
    ZimbraQueryResultsImpl mResults;
    
    final public Mailbox getMailbox() { return mMailbox; }
    final ZimbraQueryResultsImpl getResults() { return mResults; }
    
    public String getMailboxIdStr() throws ServiceException {
    	if (mMailbox == null) {
    		return "NULL_MAILBOXID";
    	}
    	return Integer.toString(mMailbox.getId()); 
    }
    
    public String getAcctIdStr() throws ServiceException {
    	if (mMailbox == null) return "NULL_ACCOUNTID";
    	return mMailbox.getAccountId(); 
    }
    
    
    public ZimbraHit(ZimbraQueryResultsImpl results, Mailbox mbx,  float score) {
        mMailbox = mbx;
        mResults = results;
        mScore = score;
    }
    
    public String toString() {
        StringBuffer toRet = new StringBuffer("MB");
        toRet.append(getMailbox().getId());
        toRet.append(" ");
        try {
            toRet.append(getItemId());
            toRet.append("-\"");
            toRet.append(getName());
            toRet.append("\"-\"");
            toRet.append(getSubject());
            toRet.append("\"-\"");
            toRet.append((new Date(getDate())).toString());
            toRet.append("\"");
            toRet.append("\"-(");
            toRet.append(getDate());
            toRet.append(")");
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        return toRet.toString();
    }
    
    protected long mCachedDate = -1;
    protected String mCachedName = null;
    protected String mCachedSubj = null;

    private float mScore = (float) 1.0;
    final public float getScore() {
        return mScore;
    }

    final public void cacheSortField(SortBy sortType, Object sortKey) {
        switch(sortType) {
        case DATE_ASCENDING:
        case DATE_DESCENDING:
            mCachedDate = ((Long)sortKey).longValue();
            break;
        case NAME_ASCENDING:
        case NAME_DESCENDING:
            mCachedName = (String)sortKey;
            break;
        case SUBJ_ASCENDING:
        case SUBJ_DESCENDING:
            mCachedSubj = (String)sortKey;
            break;
        }
    }

    /**
     * @return TRUE if this hit is not in trash and not in spam
     */
    abstract boolean inMailbox() throws ServiceException; 
    abstract boolean inTrash() throws ServiceException; 
    abstract boolean inSpam() throws ServiceException; 
    
    public abstract long getDate() throws ServiceException;
    
    public abstract int getSize() throws ServiceException ;
    
    public abstract int getConversationId() throws ServiceException;
    public abstract int getItemId() throws ServiceException;
    public abstract byte getItemType() throws ServiceException;
    
    
    /**
     * @param item which has been preloaded from the database or some other cache
     */
    abstract void setItem(MailItem item) throws ServiceException;
    
    /**
     * @return TRUE if our associated MailItem is already loaded (or we don't have one, 
     * ie ProxiedHit )
     */
    abstract boolean itemIsLoaded() throws ServiceException;
    
    /**
     * Returns the logical "subject" -- by which we mean the subject for sorting purposes.
     * 
     * @return 
     * @throws ServiceException
     */
    public abstract String getSubject() throws ServiceException;

    /**
     * Returns the "name" for sorting purposes.
     * 
     * @return
     * @throws ServiceException
     */
    public abstract String getName() throws ServiceException;
    
    final protected void updateScore(float score) {
        if (score > mScore) {
            mScore = score;
        }
    }
    
    private static class ZimbraHitSortAndIdComparator implements Comparator 
    {
        SortBy mSortOrder;
        
        ZimbraHitSortAndIdComparator(SortBy sortOrder){
            mSortOrder = sortOrder;
        }
        
        public int compare(Object o1, Object o2) {
            ZimbraHit lhs = (ZimbraHit)o1;
            ZimbraHit rhs = (ZimbraHit)o2;
            
            try {
                int retVal = lhs.compareBySortField(mSortOrder, rhs);
                if (retVal == 0) {
					int lhsId = lhs.getItemId();
					if (lhsId <= 0) {
						lhsId = lhs.getConversationId();
					}
					int rhsId = rhs.getItemId();
					if (rhsId <= 0) {
						rhsId = rhs.getConversationId();
					}
					
                    if (!mSortOrder.isDescending()) { 
                        retVal = lhsId - rhsId;
                    } else {
                        retVal = rhsId - lhsId;
                    }
                }
                return retVal;
            } catch (ServiceException e) {
                e.printStackTrace();
                return 0;
            }
        }
    }
    
    /**
     * Return a comparator which sorts by the sort field, and THEN by the mail-item-id
     * 
     * @param sortOrder
     */
    static public Comparator getSortAndIdComparator(SortBy sortOrder) {
        return new ZimbraHitSortAndIdComparator(sortOrder); 
    }
    
    /**
     * Compare this hit to other using the sort field only
     * 
     * @param sortOrder
     * @param other
     * @return <0 if "this" is BEFORE other, 0 if EQUAL, >0 if this AFTER other
     * @throws ServiceException
     */
    final int compareBySortField(MailboxIndex.SortBy sortOrder, ZimbraHit other) throws ServiceException {
        long retVal = 0;
        final boolean dumpComp = false;
        
        switch (sortOrder) {
        case DATE_ASCENDING:
            if (dumpComp) {
                System.out.println("Comparing DateAsc: \""+getDate()+"\" to \"" + other.getDate()+"\"");
                System.out.println("\tMySubj: \""+getSubject()+"\" other: \"" + other.getSubject()+"\"");
            }
            retVal = (other.getDate() - getDate());
            break;
        case DATE_DESCENDING: /* 5...4...3...*/
            if (dumpComp) {
                System.out.println("Comparing DateDesc: \""+getDate()+"\" to \"" + other.getDate()+"\"");
                System.out.println("\tMySubj: \""+getSubject()+"\" other: \"" + other.getSubject()+"\"");
            }
            retVal = (getDate() - other.getDate());
            break;
        case SUBJ_ASCENDING:
            if (dumpComp) {
                System.out.println("Comparing SubjAsc: \""+getSubject()+"\" to \"" + other.getSubject()+"\"");
            }
            /***
             * We to compare(toUpper()) instead of compareIgnoreCase or using a collator because that's the only 
             * method that seems to give us the same results as the sorts from SQL server -- esp the [] characters 
             */
            retVal = -1 * (getSubject().toUpperCase().compareTo(other.getSubject().toUpperCase()));
            break;
        case SUBJ_DESCENDING:
            if (dumpComp) {
                System.out.println("Comparing SubjDesc: \""+getSubject()+"\" to \"" + other.getSubject()+"\"");
            }
            /***
             * We to compare(toUpper()) instead of compareIgnoreCase or using a collator because that's the only 
             * method that seems to give us the same results as the sorts from SQL server -- esp the [] characters 
             */
            retVal = (getSubject().toUpperCase().compareTo(other.getSubject().toUpperCase()));
           break;
        case NAME_ASCENDING:
            if (dumpComp) {
                System.out.println("Comparing NameAsc: \""+getName()+"\" to \"" + other.getName()+"\"");
            }
            /***
             * We to compare(toUpper()) instead of compareIgnoreCase or using a collator because that's the only 
             * method that seems to give us the same results as the sorts from SQL server -- esp the [] characters 
             */
            retVal = -1 * (getName().toUpperCase().compareTo(other.getName().toUpperCase()));
            break;
        case NAME_DESCENDING:
            if (dumpComp) {
                System.out.println("Comparing NameDesc: \""+getName()+"\" to \"" + other.getName()+"\"");
            }
            /***
             * We to compare(toUpper()) instead of compareIgnoreCase or using a collator because that's the only 
             * method that seems to give us the same results as the sorts from SQL server -- esp the [] characters 
             */
            retVal = (getName().toUpperCase().compareTo(other.getName().toUpperCase()));
            break;
        case SCORE_DESCENDING:
            if (dumpComp) {
                System.out.println("Comparing ScoreDesc: \""+getScore()+"\" to \"" + other.getScore()+"\"");
            }
            retVal = (long)(10000L*(getScore() - other.getScore()));
        	break;
        default:
            throw new IllegalArgumentException("Unknown sort order: "+sortOrder);
        }

        if (dumpComp) {
            System.out.print("\tReturning: "+retVal+" asint: "+(int)retVal+"\n");
        }
        
        if (retVal == 0)
            return 0;
        else if (retVal > 0) 
            return -1;
        else 
            return 1; 
    }
    
    public final Object getSortField(SortBy sortOrder) throws ServiceException {
    	switch (sortOrder) {
    	case DATE_ASCENDING:
    	case DATE_DESCENDING: /* 5...4...3...*/
    		return getDate();
    	case SUBJ_ASCENDING:
    	case SUBJ_DESCENDING:
    		return getSubject().toUpperCase();
    	case NAME_ASCENDING:
    	case NAME_DESCENDING:
    		return getName().toUpperCase();
    	case SCORE_DESCENDING:
    		return getScore();
    	default:
    		throw new IllegalArgumentException("Unknown sort order: "+sortOrder);
    	}
    }    
}