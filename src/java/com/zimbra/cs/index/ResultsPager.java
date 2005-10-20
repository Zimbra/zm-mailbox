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
    
    private String mPrevSortValueStr;
    private long mPrevSortValueLong;
    
    private int mPrevOffset;
    private int mNumResultsRequested;

    private boolean mFixedOffset;
    
    private List mHits;
    
    static public ResultsPager create(ZimbraQueryResults results, SearchParams params) throws ServiceException
    {
        ResultsPager toRet;
        
        if (!params.hasCursor() || params.getOffset()==0) {
            toRet = new ResultsPager(results, params.getLimit(), params.getOffset());
        } else {
            // are we paging FORWARD or BACKWARD?  If requested starting-offset is the same or bigger then the cursor's offset, 
            // then we're going FORWARD, otherwise we're going BACKWARD
            boolean forward = true;
            if (params.getOffset() < params.getPrevOffset()) {
                forward = false;
            }
            
            toRet = new ResultsPager(results, params.getSortBy(), params.getPrevMailItemId(), params.getPrevSortValueStr(), params.getPrevSortValueLong(), params.getPrevOffset(), forward, params.getLimit());
        }
        return toRet;
    }
    
    public ResultsPager(ZimbraQueryResults results, int sortOrder, int prevItemId, String prevSortValueStr, long prevSortValueLong, int prevOffset, 
            boolean forward, int numResultsRequested) throws ServiceException {
        mResults = results;
        mSortOrder = sortOrder;
        mPrevMailItemId = prevItemId;
        mPrevSortValueStr = prevSortValueStr;
        mPrevSortValueLong = prevSortValueLong;
        mPrevOffset = prevOffset;
        mNumResultsRequested = numResultsRequested;
        
        mFixedOffset = false;
        
        if (forward) {
            forward();
        } else {
            backward();
        }
    }

    /**
     * Simple offset
     * 
     * @param results
     * @param numResults
     * @param offset
     * @throws ServiceException
     */
    public ResultsPager(ZimbraQueryResults results, int numResults, int offset) throws ServiceException {
        mResults = results;
        mNumResultsRequested = numResults;
        mFixedOffset = true;
        
        if (offset > 0) {
            mResults.skipToHit(offset-1);
        } else {
            mResults.resetIterator();
        }
        forward();
    }
    
    public List getHits() { return mHits; }
    public boolean hasNext() throws ServiceException { return mResults.hasNext(); }
    
    private ZimbraHit getDummyHit() {
        long dateVal = 0;
        String strVal = "";
        strVal = mPrevSortValueStr;
        dateVal = mPrevSortValueLong;
        
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
            
            // if (hit COMES AFTER prevSortValue) {
            if (hit.compareBySortField(mSortOrder, prevHit) > 0) {
                return hit;
            }

//            if (offset > mPrevOffset) {
//                throw new NewResultsAtHeadException();
//            }
            
            hit = mResults.getNext();
        }

        // end of line
        return null;
    }
    
    private void forward() throws ServiceException {
        mHits = new ArrayList(mNumResultsRequested);
        
        ZimbraHit hit;

        if (!mFixedOffset) {
            hit = forwardFindFirst();
        } else {
            hit = mResults.getNext();
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
            offset++;
            
            if (hit.getItemId() == mPrevMailItemId) {
                // found old one -- DON'T include it in list
                break;
            }
            
      
            // if (hit COMES AFTER prevSortValue) {
            if (hit.compareBySortField(mSortOrder, prevHit) > 0) {
                break;
            }
            
//            if (offset > mPrevOffset) {
//                throw new NewResultsAtHeadException();
//            }
            
            // okay, so it isn't time to stop yet.
            // add this hit onto our growing list.... and also take
            // existing things off the list if the list is already 
            // as big as we need it..
            if (offset >= mNumResultsRequested) {
                ll.removeFirst();
            }
            ll.addLast(hit);
            
            hit = mResults.getNext();
        }

        // possible that we backed up to the start of the results set....so
        // we do have to check and see if we got enough results...
        while (ll.size() < mNumResultsRequested && hit != null) {
            ll.addLast(hit);
            hit = mResults.getNext();
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
