/*
 * Created on Mar 30, 2005
 *
 * Simple class that encapsulates all of the parameters involved in a Search request.
 * Not used everywhere, need to convert all code to use this....
 * 
 */
package com.zimbra.cs.index;

import com.zimbra.cs.service.ServiceException;


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
    public int getSortBy() { return sortBy; }
    public boolean getFetchFirst() { return mFetchFirst; }
    public boolean getMarkRead() { return mMarkRead; }
    public boolean getWantHtml() { return mWantHtml; }
    public boolean getWantRecipients() { return mRecipients; }
    
    public void setQueryStr(String queryStr) { mQueryStr = queryStr; }
    public void setOffset(int offset) { mOffset = offset; }
    public void setLimit(int limit) { mLimit = limit; }
    public void setTypesStr(String groupByStr) throws ServiceException {
        mGroupByStr = groupByStr;
        types = MailboxIndex.parseGroupByString(getTypesStr());
    }
    public void setSortByStr(String sortByStr) throws ServiceException { 
        mSortByStr = sortByStr;
        sortBy = MailboxIndex.parseSortByString(mSortByStr);        
    }
    public void setFetchFirst(boolean fetch) { mFetchFirst = fetch; }
    public void setMarkRead(boolean read) { mMarkRead = read; }
    public void setWantHtml(boolean html) { mWantHtml = html; }
    public void setWantRecipients(boolean recips) { mRecipients = recips; }


    private String mQueryStr;
    private int mOffset;
    private int mLimit;
    private boolean mFetchFirst;
    private boolean mMarkRead;
    private boolean mWantHtml;
    private boolean mRecipients;

    // unparsed -- these need to go away!
    private String mGroupByStr;
    private String mSortByStr;
    
    // parsed:
    private int sortBy; // parsed value of sort by string
    private byte[] types; // types to seach for
}