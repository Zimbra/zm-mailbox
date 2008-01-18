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

/*
 * Created on Oct 15, 2004
 */
package com.zimbra.cs.index;

import java.util.Comparator;
import java.util.Date;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.util.ItemId;

/**
 * 
 * Base class for a search "hit".  Generally one iterates over a 
 * ZimbraQueryResults to get the hits for a given query.
 *
 */
public abstract class ZimbraHit 
{
    public ZimbraHit(ZimbraQueryResultsImpl results, Mailbox mbx,  float score) {
        mMailbox = mbx;
        mResults = results;
        mScore = score;
    }    

    /**
     * Used for cross-mailbox-search, returns the AccountId of the hit
     * 
     * @return
     * @throws ServiceException
     */
    public String getAcctIdStr() {
        if (mMailbox == null) return "NULL_ACCOUNTID";
        return mMailbox.getAccountId(); 
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

    public Object getSortField(SortBy sortOrder) throws ServiceException {
        switch (sortOrder) {
            case NONE:
                return "";
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
            case TASK_DUE_ASCENDING:
            case TASK_DUE_DESCENDING:
            case TASK_STATUS_ASCENDING:
            case TASK_STATUS_DESCENDING:
            case TASK_PERCENT_COMPLETE_ASCENDING:
            case TASK_PERCENT_COMPLETE_DESCENDING:
                throw new IllegalArgumentException("Wrong hit type for hit "+this.toString()+" with sort order: "+sortOrder);
            default:
                throw new IllegalArgumentException("Unknown sort order: "+sortOrder);
        }
    }

    final public float getScore() { return mScore; }
    public abstract int getItemId() throws ServiceException;

    public ItemId getParsedItemID() throws ServiceException {
        return new ItemId(mMailbox, getItemId());
    }

    protected Mailbox mMailbox;
    protected ZimbraQueryResultsImpl mResults;
    protected long mCachedDate = -1;
    protected String mCachedName = null;
    protected String mCachedSubj = null;
    private float mScore = (float) 1.0;
    protected ImapMessage mCachedImapMessage = null;
    protected int mCachedModseq = -1;

    final Mailbox getMailbox() { return mMailbox; }
    final ZimbraQueryResultsImpl getResults() { return mResults; }

    /**
     * This function is only guaranteed to return a useful value for 
     * sort-by-date, or for local hits
     * 
     * @return
     * @throws ServiceException
     */
    abstract long getDate() throws ServiceException;


    /**
     * This function is only guaranteed to return a useful value for local hits
     * 
     * @return
     * @throws ServiceException
     */
    abstract long getSize() throws ServiceException ;


    /**
     * This function is only guaranteed to return a useful value for local hits 
     * 
     * @return the conversation ID, or 0 if there is not one
     * 
     * @throws ServiceException
     */
    abstract int getConversationId() throws ServiceException;

    /**
     * Compare this hit to other using the sort field only
     * 
     * @param sortOrder
     * @param other
     * @return <0 if "this" is BEFORE other, 0 if EQUAL, >0 if this AFTER other
     * @throws ServiceException
     */
    int compareBySortField(MailboxIndex.SortBy sortOrder, ZimbraHit other) throws ServiceException {
        long retVal = 0;
        final boolean dumpComp = false;

        switch (sortOrder) {
            case NONE:
                throw new IllegalArgumentException("Illegal to use sort comparison on unsorted results");
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


    /**
     * return the MailItem corresponding to this hit, or NULL if one is not available
     * (e.g. for a ProxiedHit)
     */
    public abstract MailItem getMailItem() throws ServiceException;

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
     * Note that this function is only guaranteed to work if the search is sort-by-subject
     * 
     * @return 
     * @throws ServiceException
     */
    abstract String getSubject() throws ServiceException;

    /**
     * Returns the "name" for sorting purposes.
     * 
     * Note that this function is only guaranteed to work if the search is sort-by-name
     * 
     * @return
     * @throws ServiceException
     */
    abstract String getName() throws ServiceException;


    /**
     * @return TRUE if this hit is local to this mailbox (ie not proxied)
     */
    boolean isLocal() {
        return true;
    }

    public ImapMessage getImapMessage() throws ServiceException {
        if (mCachedImapMessage != null)
            return mCachedImapMessage;
        MailItem item = getMailItem();
        if (item == null)
            return null;
        return (mCachedImapMessage = new ImapMessage(item));
    }

    public int getModifiedSequence() throws ServiceException {
        if (mCachedModseq > 0)
            return mCachedModseq;
        MailItem item = getMailItem();
        if (item == null)
            return -1;
        return item.getModifiedSequence();
    }

    final protected void updateScore(float score) {
        if (score > mScore) {
            mScore = score;
        }
    }

    final void cacheSortField(SortBy sortType, Object sortKey) {
        switch(sortType) {
            case DATE_ASCENDING:
            case DATE_DESCENDING:
                mCachedDate = ((Long) sortKey).longValue();
                break;
            case NAME_ASCENDING:
            case NAME_DESCENDING:
                mCachedName = (String) sortKey;
                break;
            case SUBJ_ASCENDING:
            case SUBJ_DESCENDING:
                mCachedSubj = (String) sortKey;
                break;
        }
    }

    final void cacheImapMessage(ImapMessage i4msg) {
        mCachedImapMessage = i4msg;
    }

    final void cacheModifiedSequence(int modseq) {
        mCachedModseq = modseq;
    }

    /**
     * Return a comparator which sorts by the sort field, and THEN by the mail-item-id
     * 
     * @param sortOrder
     */
    static Comparator getSortAndIdComparator(SortBy sortOrder) {
        return new ZimbraHitSortAndIdComparator(sortOrder); 
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
}