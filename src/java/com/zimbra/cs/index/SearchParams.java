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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Mar 30, 2005
 *
 * Simple class that encapsulates all of the parameters involved in a Search request.
 * Not used everywhere, need to convert all code to use this....
 * 
 */
package com.zimbra.cs.index;

import java.util.Locale;
import java.util.TimeZone;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.mail.ToXML.OutputParticipants;
import com.zimbra.cs.service.util.ItemId;


/**
 * @author tim
 * 
 * To initialize, set:
 *   -- query str
 *   -- offset
 *   -- limit
 *   -- typesStr (sets type value)
 *   -- sortByStr (sets sortBy value)
 *
 */
public final class SearchParams {
    
    private static final int MAX_OFFSET = 10000000; // 10M
    private static final int MAX_LIMIT = 10000000; // 10M
    

    public enum ExpandResults {
        NONE, FIRST, HITS, ALL;

        public static ExpandResults get(String value) {
            if (value == null)
                return NONE;
            value = value.toUpperCase();
            try {
                return valueOf(value);
            } catch (IllegalArgumentException iae) {
                if (value.equals("1") || value.equals("TRUE"))
                    return FIRST;
                return NONE;
            }
        }
    };

    public int getLimit() { return mLimit; }
    public int getOffset() { return mOffset; }
    public String getQueryStr() { return mQueryStr; }
    public String getTypesStr() { return mGroupByStr; }
    public byte[] getTypes() { return types; }
    public String getSortByStr() { return mSortByStr; }
    public MailboxIndex.SortBy getSortBy() { return mSortBy; }
    public ExpandResults getFetchFirst() { return mFetchFirst; }
    public boolean getMarkRead() { return mMarkRead; }
    public boolean getWantHtml() { return mWantHtml; }
    public boolean getNeuterImages() { return mNeuterImages; }
    public OutputParticipants getWantRecipients() { return mRecipients ? OutputParticipants.PUT_RECIPIENTS : OutputParticipants.PUT_SENDERS; }

    public void setQueryStr(String queryStr) { mQueryStr = queryStr; }
    public void setOffset(int offset) { mOffset = offset; if (mOffset > MAX_OFFSET) mOffset = MAX_OFFSET; }
    public void setLimit(int limit) { mLimit = limit; if (mLimit > MAX_LIMIT) mLimit = MAX_LIMIT; }

    /**
     * 
     * since the results are iterator-based, the "limit" is really the same as the chunk size + offset
     * ie, the limit is used to tell the system approximately how many results you want and it tries to get them
     * in a single chunk --- but it isn't until you do the results iteration that the limit is enforced. 
     * 
     * @param chunkSize
     */
    public void setChunkSize(int chunkSize) {
        setLimit(chunkSize + mOffset); 
    } 

    public void setTypesStr(String groupByStr) throws ServiceException {
        mGroupByStr = groupByStr;
        types = MailboxIndex.parseTypesString(getTypesStr());
    }
    public void setTypes(byte[] _types) { 
        types = _types;
        boolean atFirst = true;
        StringBuilder s = new StringBuilder();
        for (byte b : _types) {
            if (!atFirst)
                s.append(',');
            s.append(MailItem.getNameForType(b));
            atFirst = false;
        }
        mGroupByStr = s.toString();
    }


    public void setSortBy(SortBy sortBy) {
        mSortBy = sortBy;
        mSortByStr = mSortBy.toString(); 
    }
    public void setSortByStr(String sortByStr) { 
        mSortByStr = sortByStr;
        mSortBy = MailboxIndex.SortBy.lookup(sortByStr);
        if (mSortBy == null) {
            mSortBy = SortBy.DATE_DESCENDING;
            mSortByStr = mSortBy.toString();
        }
    }
    public void setFetchFirst(ExpandResults fetch) { mFetchFirst = fetch; }
    public void setMarkRead(boolean read) { mMarkRead = read; }
    public void setWantHtml(boolean html) { mWantHtml = html; }
    public void setNeuterImages(boolean neuter) { mNeuterImages = neuter; }
    public void setWantRecipients(boolean recips) { mRecipients = recips; }
    public void setTimeZone(TimeZone tz) { mTimeZone = tz; }
    public void setLocale(Locale loc) { mLocale = loc; }

    public boolean hasCursor() { return mHasCursor; }
    public void setCursor(ItemId prevMailItemId, String prevSort, int prevOffset, String endSort) {
        mHasCursor = true;
        mPrevMailItemId = prevMailItemId;
        mPrevSortValueStr = prevSort;
        try {
            mPrevSortValueLong = Long.parseLong(prevSort);
        } catch (NumberFormatException e) {
            mPrevSortValueLong = 0;
        }
        mPrevOffset = prevOffset;
        mEndSortValueStr = endSort;
        mEndSortValueLong = -1;
        if (mEndSortValueStr != null) {
            try {
                mEndSortValueLong = Long.parseLong(mEndSortValueStr);
            } catch (NumberFormatException e) {
                mEndSortValueLong = Long.MAX_VALUE;
            }
        }
    }
    public ItemId getPrevMailItemId() { return mPrevMailItemId; }
    public String getPrevSortValueStr() { return mPrevSortValueStr; }
    public long getPrevSortValueLong() { return mPrevSortValueLong; }
    public int getPrevOffset() { return mPrevOffset; }
    public boolean hasEndSortValue() { return mEndSortValueStr != null; }
    public String getEndSortValueStr() { return mEndSortValueStr; }
    public long getEndSortValueLong() { return mEndSortValueLong; }
    
    public TimeZone getTimeZone() { return mTimeZone; }
    public Locale getLocale() { return mLocale; }
    
    public boolean getPrefetch() { return mPrefetch; }
    public void setPrefetch(boolean truthiness) { mPrefetch = truthiness; }
    public Mailbox.SearchResultMode getMode() { return mMode; }
    public void setMode(Mailbox.SearchResultMode mode) { mMode = mode; }
    public boolean getEstimateSize() { return mEstimateSize; }
    
    
    /**
     * @param estimateSize
     */
    public void setEstimateSize(boolean estimateSize) { mEstimateSize = estimateSize; }

    private String mQueryStr;
    private int mOffset;
    private int mLimit;
    private ExpandResults mFetchFirst;
    private boolean mMarkRead;
    private boolean mWantHtml;
    private boolean mNeuterImages;
    private boolean mRecipients;
    
    private TimeZone mTimeZone; // timezone that the query should be parsed in (for date/time queries)
    private Locale mLocale; 

    private boolean mHasCursor = false;

    /////////////////////
    // "Cursor" Data -- the three pieces of info below are enough for us to find out place in
    // the previous result set, even if entries have been added or removed from the result
    // set:
    private ItemId mPrevMailItemId; // the mail item ID of the last item in the previous result set
    private String mPrevSortValueStr; // the sort value of the last item in the previous result set
    private long mPrevSortValueLong; // the sort value of the last item in the previous result set
    private int mPrevOffset; // the offset of the last item in the previous result set
    private String mEndSortValueStr; // where to end the search. Hits >= this value are NOT included in the result set.
    private long mEndSortValueLong; // where to end the search. Hits >= this value are NOT included in the result set.


    // unparsed -- these need to go away!
    private String mGroupByStr;
    private String mSortByStr;

    // parsed:
    private MailboxIndex.SortBy mSortBy;
    private byte[] types; // types to seach for
    
    private boolean mPrefetch = true;
    private Mailbox.SearchResultMode mMode = Mailbox.SearchResultMode.NORMAL;
    
    private boolean mEstimateSize = false; // ask or a size estimate.  Note that this might have a nontrivial performance impact
}