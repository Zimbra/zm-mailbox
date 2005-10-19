package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;

/**
 * @author tim
 * 
 * A helper class which deals with paging through search results (see bug 2937)
 *
 */
public class ResultsPager 
{
    private ZimbraQueryResults mResults;
    private int mSortOrder;
    private int mPrevMailItemId;
    
    private Object mPrevSortValue;
    private int mPrevOffset;
    private int mNumResultsRequested;
    
    private boolean mFirstHit;
    
    private List mHits;
    
    public ResultsPager(ZimbraQueryResults results, int sortOrder, int prevItemId, Object prevSortValue, int prevOffset, 
            boolean forward, int numResultsRequested) throws ServiceException {
        mResults = results;
        mSortOrder = sortOrder;
        mPrevMailItemId = prevItemId;
        mPrevSortValue = prevSortValue;
        mPrevOffset = prevOffset;
        mNumResultsRequested = numResultsRequested;
        
        mFirstHit = false;
        
        if (forward) {
            forward();
        } else {
            backward();
        }
    }
    
    public ResultsPager(ZimbraQueryResults results, int numResults) throws ServiceException {
        mFirstHit = true;
        mResults = results;
        mNumResultsRequested = numResults;
        forward();
    }
    
    public List getHits() { return mHits; }
    public boolean hasNext() throws ServiceException { return mResults.hasNext(); }
    
    private ZimbraHit getDummyHit() {
        long dateVal = 0;
        String strVal = "";
        if (mPrevSortValue instanceof String) {
            strVal = (String)mPrevSortValue;
        } else {
            dateVal = ((Long)mPrevSortValue).longValue();
        }
        
        return new DummyHit(strVal, strVal, dateVal, mPrevMailItemId);
    }
    
    private ZimbraHit forwardFindFirst() throws ServiceException {
        int offset = 0;
        
        ZimbraHit prevHit = getDummyHit();
        
        ZimbraHit hit = mResults.getFirstHit();
        while(hit != null) {
            offset++;
            
            if (hit.getItemId() == mPrevMailItemId) {
                // found it!
                return mResults.getNext();
            } 
            
//            if (hit COMES AFTER prevSortValue) {
//                return hit;
//            }
            if (hit.compareBySortField(mSortOrder, prevHit) > 0) {
                return hit;
            }
            
            if (offset > mPrevOffset) {
                throw new NewResultsAtHeadException();
            }
            
            
            hit = mResults.getNext();
        }

        // end of line
        return null;
    }
    
    private void forward() throws ServiceException {
        mHits = new ArrayList(mNumResultsRequested);
        
        ZimbraHit hit;

        if (!mFirstHit) {
            hit = forwardFindFirst();
        } else {
            hit = mResults.getFirstHit();
        }

        for (int i = 0; hit != null && i < mNumResultsRequested; i++) {
            mHits.add(i, hit);
            hit = mResults.getNext();
        }
    }
    
    private void backward() throws ServiceException {
        LinkedList ll = new LinkedList();
        mHits = ll;

        int offset = 0;
        ZimbraHit hit = mResults.getFirstHit();
        ZimbraHit prevHit = getDummyHit();
        
        
        while(hit != null) {
            // time to stop?
            
            
            if (hit.getItemId() == mPrevMailItemId) {
                // found old one -- DON'T include it in list
                return;
            }
            
//          if (hit COMES AFTER prevSortValue) {
//          return;
//      }
      
            if (hit.compareBySortField(mSortOrder, prevHit) > 0) {
                return;
            }
            
            if (offset > mPrevOffset) {
                throw new NewResultsAtHeadException();
            }
            
            // okay, so it isn't time to stop yet.
            // add this hit onto our growing list.... and also take
            // existing things off the list if the list is already 
            // as big as we need it..
            if (offset >= mNumResultsRequested) {
                ll.removeFirst();
            }
            ll.addLast(hit);
            
            offset++;
        }
        
        
    }
    
    public static class NewResultsAtHeadException extends ServiceException
    {
        NewResultsAtHeadException() {
            super("There are new results at the front of the search results", "SERVICE.new_results", false);
        }
    }
    
    private static class DummyHit extends ZimbraHit
    {
        private int mItemId;
        private long mDate;
        private String mName;
        private String mSubject;
        
        DummyHit(String name, String subject, long date, int itemId)
        {
            super(null, null, 0);
            mName = name;
            mSubject = subject;
            mDate = date;
            mItemId = itemId;
        }
        
        boolean inMailbox() { return false; } 
        boolean inTrash() { return false; }
        boolean inSpam() { return false; }
        public long getDate() { return mDate; } 
        public int getSize() { return 0; }
        public int getConversationId() { return 0; }
        public int getItemId() { return mItemId; }
        public byte getItemType() { return 0; }
        void setItem(MailItem item) {}
        boolean itemIsLoaded() { return false; }
        public String getSubject() { return mSubject; }
        public String getName() { return mName; }
    }
    
}
