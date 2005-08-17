/*
 * Created on Oct 15, 2004
 */
package com.zimbra.cs.index;

import java.util.Comparator;
import java.util.Date;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;

public abstract class LiquidHit 
{
    
    Mailbox mMailbox;
    LiquidQueryResultsImpl mResults;
    
    final public Mailbox getMailbox() { return mMailbox; }
    final LiquidQueryResultsImpl getResults() { return mResults; }
    
    public String getMailboxIdStr() throws ServiceException { return Integer.toString(mMailbox.getId()); } 
    
    public LiquidHit(LiquidQueryResultsImpl results, Mailbox mbx,  float score) {
        mMailbox = mbx;
        mResults = results;
        mScore = score;
//        assert(mbx != null);
        assert(mResults != null);
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

    private float mScore = (float) -3.575;
    final public float getScore() {
        return mScore;
    }

    final public void cacheSortField(int sortType, Object sortKey) {
        switch(sortType) {
        case MailboxIndex.SEARCH_ORDER_DATE_ASC:
        case MailboxIndex.SEARCH_ORDER_DATE_DESC:
            mCachedDate = ((Long)sortKey).longValue();
            break;
        case MailboxIndex.SEARCH_ORDER_NAME_ASC:
        case MailboxIndex.SEARCH_ORDER_NAME_DESC:
            mCachedName = (String)sortKey;
            break;
        case MailboxIndex.SEARCH_ORDER_SUBJ_ASC:
        case MailboxIndex.SEARCH_ORDER_SUBJ_DESC:
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
    abstract void setItem(MailItem item);
    
    /**
     * @return TRUE if our associated MailItem is already loaded (or we don't have one, 
     * ie ProxiedHit )
     */
    abstract boolean itemIsLoaded();
    
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
    
    private static class LiquidHitSortAndIdComparator implements Comparator 
    {
        int mSortOrder;
        
        LiquidHitSortAndIdComparator(int sortOrder){
            mSortOrder = sortOrder;
        }
        
        public int compare(Object o1, Object o2) {
            LiquidHit lhs = (LiquidHit)o1;
            LiquidHit rhs = (LiquidHit)o2;
            
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
					
                    if (!MailboxIndex.isSortDescending(mSortOrder)) { 
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
    static public Comparator getSortAndIdComparator(int sortOrder) {
        return new LiquidHitSortAndIdComparator(sortOrder); 
    }
    
    /**
     * Compare this hit to other using the sort field only
     * 
     * @param sortOrder
     * @param other
     * @return <0 if "this" is BEFORE other, 0 if EQUAL, <0 if this AFTER other
     * @throws ServiceException
     */
    final int compareBySortField(int sortOrder, LiquidHit other) throws ServiceException {
        long retVal = 0;
        final boolean dumpComp = false;
        
        switch (sortOrder) {
        case MailboxIndex.SEARCH_ORDER_DATE_ASC:
            if (dumpComp) {
                System.out.println("Comparing DateAsc: \""+getDate()+"\" to \"" + other.getDate()+"\"");
                System.out.println("\tMySubj: \""+getSubject()+"\" other: \"" + other.getSubject()+"\"");
            }
            retVal = (other.getDate() - getDate());
            break;
        case MailboxIndex.SEARCH_ORDER_DATE_DESC: /* 5...4...3...*/
            if (dumpComp) {
                System.out.println("Comparing DateDesc: \""+getDate()+"\" to \"" + other.getDate()+"\"");
                System.out.println("\tMySubj: \""+getSubject()+"\" other: \"" + other.getSubject()+"\"");
            }
            retVal = (getDate() - other.getDate());
            break;
        case MailboxIndex.SEARCH_ORDER_SUBJ_ASC:
            if (dumpComp) {
                System.out.println("Comparing SubjAsc: \""+getSubject()+"\" to \"" + other.getSubject()+"\"");
            }
            /***
             * We to compare(toUpper()) instead of compareIgnoreCase or using a collator because that's the only 
             * method that seems to give us the same results as the sorts from SQL server -- esp the [] characters 
             */
            retVal = -1 * (getSubject().toUpperCase().compareTo(other.getSubject().toUpperCase()));
            break;
        case MailboxIndex.SEARCH_ORDER_SUBJ_DESC:
            if (dumpComp) {
                System.out.println("Comparing SubjDesc: \""+getSubject()+"\" to \"" + other.getSubject()+"\"");
            }
            /***
             * We to compare(toUpper()) instead of compareIgnoreCase or using a collator because that's the only 
             * method that seems to give us the same results as the sorts from SQL server -- esp the [] characters 
             */
            retVal = (getSubject().toUpperCase().compareTo(other.getSubject().toUpperCase()));
           break;
        case MailboxIndex.SEARCH_ORDER_NAME_ASC:
            if (dumpComp) {
                System.out.println("Comparing NameAsc: \""+getName()+"\" to \"" + other.getName()+"\"");
            }
            /***
             * We to compare(toUpper()) instead of compareIgnoreCase or using a collator because that's the only 
             * method that seems to give us the same results as the sorts from SQL server -- esp the [] characters 
             */
            retVal = -1 * (getName().toUpperCase().compareTo(other.getName().toUpperCase()));
            break;
        case MailboxIndex.SEARCH_ORDER_NAME_DESC:
            if (dumpComp) {
                System.out.println("Comparing NameDesc: \""+getName()+"\" to \"" + other.getName()+"\"");
            }
            /***
             * We to compare(toUpper()) instead of compareIgnoreCase or using a collator because that's the only 
             * method that seems to give us the same results as the sorts from SQL server -- esp the [] characters 
             */
            retVal = (getName().toUpperCase().compareTo(other.getName().toUpperCase()));
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
}