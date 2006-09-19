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

import java.util.TimeZone;

import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;
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
public final class SearchParams 
{
    public int getLimit() { return mLimit; }
    public int getOffset() { return mOffset; }
    public String getQueryStr() { return mQueryStr; }
    public String getTypesStr() { return mGroupByStr; }
    public byte[] getTypes() { return types; }
    public String getSortByStr() { return mSortByStr; }
    public MailboxIndex.SortBy getSortBy() { return mSortBy; }
    public boolean getFetchFirst() { return mFetchFirst; }
    public boolean getMarkRead() { return mMarkRead; }
    public boolean getWantHtml() { return mWantHtml; }
    public OutputParticipants getWantRecipients() { return mRecipients ? OutputParticipants.PUT_RECIPIENTS : OutputParticipants.PUT_SENDERS; }

    public void setQueryStr(String queryStr) { mQueryStr = queryStr; }
    public void setOffset(int offset) { mOffset = offset; }

    public void setLimit(int limit) { mLimit = limit; }

    // since the results are iterator-based, the "limit" is really the same as the chunk size at this point
    // ie, the limit is used to tell the system approximately how many results you want and it tries to get them
    // in a single chunk --- but it isn't until you do the results iteration that the limit is enforced.
    public void setChunkSize(int chunkSize) { setLimit(chunkSize); } 

    public void setTypesStr(String groupByStr) throws ServiceException {
        mGroupByStr = groupByStr;
        types = MailboxIndex.parseGroupByString(getTypesStr());
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
    public void setSortByStr(String sortByStr) throws ServiceException { 
        mSortByStr = sortByStr;
        mSortBy = MailboxIndex.SortBy.lookup(sortByStr);
        if (mSortBy == null) {
            mSortBy = SortBy.DATE_DESCENDING;
            mSortByStr = mSortBy.toString();
        }
    }
    public void setFetchFirst(boolean fetch) { mFetchFirst = fetch; }
    public void setMarkRead(boolean read) { mMarkRead = read; }
    public void setWantHtml(boolean html) { mWantHtml = html; }
    public void setWantRecipients(boolean recips) { mRecipients = recips; }
    public void setTimeZone(TimeZone tz) { mTimeZone = tz; }

    public boolean hasCursor() { return mHasCursor; }
    public void setCursor(ItemId prevMailItemId, String prevSort, int prevOffset) {
        mHasCursor = true;
        mPrevMailItemId = prevMailItemId;
        mPrevSortValueStr = prevSort;
        try {
            mPrevSortValueLong = Long.parseLong(prevSort);
        } catch (NumberFormatException e) {
            mPrevSortValueLong = 0;
        }
        mPrevOffset = prevOffset;
    }
    public ItemId getPrevMailItemId() { return mPrevMailItemId; }
    public String getPrevSortValueStr() { return mPrevSortValueStr; }
    public long getPrevSortValueLong() { return mPrevSortValueLong; }
    public int getPrevOffset() { return mPrevOffset; }
    public TimeZone getTimeZone() { return mTimeZone; }

    private String mQueryStr;
    private int mOffset;
    private int mLimit;
    private boolean mFetchFirst;
    private boolean mMarkRead;
    private boolean mWantHtml;
    private boolean mRecipients;
    
    private TimeZone mTimeZone; // timezone that the query should be parsed in (for date/time queries)

    private boolean mHasCursor = false;

    /////////////////////
    // "Cursor" Data -- the three pieces of info below are enough for us to find out place in
    // the previous result set, even if entries have been added or removed from the result
    // set:
    private ItemId mPrevMailItemId; // the mail item ID of the last item in the previous result set
    private String mPrevSortValueStr; // the sort value of the last item in the previous result set
    private long mPrevSortValueLong; // the sort value of the last item in the previous result set
    private int mPrevOffset; // the offset of the last item in the previous result set 


    // unparsed -- these need to go away!
    private String mGroupByStr;
    private String mSortByStr;

    // parsed:
    private MailboxIndex.SortBy mSortBy;
    private byte[] types; // types to seach for
}