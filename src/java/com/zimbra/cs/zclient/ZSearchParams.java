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

package com.zimbra.cs.zclient;

public class ZSearchParams {

    /** sort by date descending */
    public static final String SORT_BY_DATE_DESC = "dateDesc"; 
    
    /** sort by date ascending */
    public static final String SORT_BY_DATE_ASC = "dateAsc";
    
    /** sort by subject descending */
    public static final String SORT_BY_SUBJECT_DESC = "subjectDesc";
    
    /** sort by subject ascending */
    public static final String SORT_BY_SUBJECT_ASC = "subjectAsc";
    
    /** sort by name descending */
    public static final String SORT_BY_NAME_DESC = "nameDesc";
    
    /** sort by name ascending */
    public static final String SORT_BY_NAME_ASC = "nameAsc";
    

    public static final String TYPE_CONVERSATION = "conversation";
    
    public static final String TYPE_MESSAGE = "message";
    
    public static final String TYPE_CONTACT = "contact";
    
    public static final String TYPE_APPOINTMENT = "appointment";
    
    
    /**
     *  max number of results to return (0 to return all)
     */
    private int mLimit;
    
    /**
     * offset is an integer specifying the 0-based offset into the results list to return as
     * the first result for this search operation. 
     */
    private int mOffset;
    
    /**
     * dateDesc|dateAsc|subjDesc|subjAsc|nameDesc|nameAsc(default is "dateDesc")
     */
    private String mSortBy = SORT_BY_DATE_DESC;
    
    /**
     * comma-separated list.  Legal values are:
     *          conversation|message|contact|appointment|note
     *          (default is "conversation" {@link #TYPE_CONVERSATION})
     *
     */
    private String mTypes = TYPE_CONVERSATION;

    /**
     * fetch the first part (messages only at this point) in the result
     */
    private boolean mFetchFirstMessage;
    
    /**
     * if fetchFirstMessage is true, grab the HTML part if available
     */
    private boolean mPreferHtml;
    
    /**
     * if fetchFirstMessage is true, mark first message as read
     */
    private boolean mMarkAsRead;
 
    /**
     * if recip="1" is specified:
     * + returned sent messages will contain the set of "To:" recipients instead of the sender
     * + returned conversations whose first hit was sent by the user will contain
     *  that hit's "To:" recipients instead of the conversation's sender list

     */
    private boolean mRecipientMode;
    
    /**
     * the search query to run
     */
    private String mQuery;
    
    /**
     * cursorPreviousId and cursorPreviousSortValue 
     * correspond to the last hit on the previous page (assuming you're
     * going forward -- if you're backing up then th
     * ey should be the first
     * hit on the previous page)....the server uses those parameters to find
     * the spot in the new results that corresponds to your old position:
     * even if some entries have been removed or added to the search results
     * (e.g. if you are searching is:unread and you read some)
     */
    
    private String mCursorPreviousId;
    
    /**
     * @see #cursorPreviousId
     */
    private String mCursorPreviousSortValue;

    public ZSearchParams(String query) {
        mQuery = query;
    }

    public String getCursorPreviousId() {
        return mCursorPreviousId;
    }

    public void setCursorPreviousId(String cursorPreviousId) {
        mCursorPreviousId = cursorPreviousId;
    }

    public String getCursorPreviousSortValue() {
        return mCursorPreviousSortValue;
    }

    public void setCursorPreviousSortValue(String cursorPreviousSortValue) {
        mCursorPreviousSortValue = cursorPreviousSortValue;
    }

    public boolean isFetchFirstMessage() {
        return mFetchFirstMessage;
    }

    public void setFetchFirstMessage(boolean fetchFirstMessage) {
        mFetchFirstMessage = fetchFirstMessage;
    }

    public int getLimit() {
        return mLimit;
    }

    public void setLimit(int limit) {
        mLimit = limit;
    }

    public boolean isMarkAsRead() {
        return mMarkAsRead;
    }

    public void setMarkAsRead(boolean markAsRead) {
        mMarkAsRead = markAsRead;
    }

    public int getOffset() {
        return mOffset;
    }

    public void setOffset(int offset) {
        mOffset = offset;
    }

    public boolean isPreferHtml() {
        return mPreferHtml;
    }

    public void setPeferHtml(boolean preferHtml) {
        mPreferHtml = preferHtml;
    }

    public String getQuery() {
        return mQuery;
    }

    public void setQuery(String query) {
        mQuery = query;
    }

    public boolean isRecipientMode() {
        return mRecipientMode;
    }

    public void setRecipientMode(boolean recipientMode) {
        mRecipientMode = recipientMode;
    }

    public String getSortBy() {
        return mSortBy;
    }

    public void setSortBy(String sortBy) {
        mSortBy = sortBy;
    }

    public String getTypes() {
        return mTypes;
    }

    public void setTypes(String types) {
        mTypes = types;
    }
}
