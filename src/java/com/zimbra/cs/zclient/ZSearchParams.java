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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;


import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.zclient.ZMailbox.SearchSortBy;

public class ZSearchParams {

    public static final String TYPE_CONVERSATION = "conversation";
    
    public static final String TYPE_MESSAGE = "message";
    
    public static final String TYPE_CONTACT = "contact";
    
    public static final String TYPE_APPOINTMENT = "appointment";
    
    public static String getCanonicalTypes(String list) throws ServiceException  {
        if (list == null || list.length() == 0) return "";

        StringBuilder sb = new StringBuilder();
        for (String s : list.split(",")) {
            if (sb.length() > 0) sb.append(",");
            if (s.startsWith("conv") && TYPE_CONVERSATION.startsWith(s))
                sb.append(TYPE_CONVERSATION);
            else if (s.startsWith("m") && TYPE_MESSAGE.startsWith(s))
                sb.append(TYPE_MESSAGE);
            else if (s.startsWith("cont") && TYPE_CONTACT.startsWith(s)) 
                sb.append(TYPE_CONTACT);
            else if (s.startsWith("a") && TYPE_APPOINTMENT.startsWith(s))
                sb.append(TYPE_APPOINTMENT);
            else
                throw ZClientException.CLIENT_ERROR("invlaid search type: "+s, null);
        }
        return sb.toString();
    }
    
    /**
     *  max number of results to return
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
    private SearchSortBy mSortBy = SearchSortBy.dateDesc;
    
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
    private ZMailbox.Fetch mFetch;
    
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
    
    private Cursor mCursor;

    public ZSearchParams(ZSearchParams that) {
        this.mCursor = that.mCursor;
        this.mFetch = that.mFetch;
        this.mLimit = that.mLimit;
        this.mMarkAsRead = that.mMarkAsRead;
        this.mOffset = that.mOffset;
        this.mPreferHtml = that.mPreferHtml;
        this.mQuery = that.mQuery;
        this.mRecipientMode = that.mRecipientMode;
        this.mSortBy = that.mSortBy;
        this.mTypes = that.mTypes;
    }

    public ZSearchParams(String query) {
        mQuery = query;
    }

    /**
     * init search params (query, types, sortBy) from a search folder.
     * @param folder
     */
    public ZSearchParams(ZSearchFolder folder) {
        mQuery = folder.getQuery();
        mTypes = folder.getTypes();
        mSortBy = folder.getSortBy();
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void setCursor(Cursor cursor) {
        mCursor = cursor;
    }
    
    public ZMailbox.Fetch getFetch() {
        return mFetch;
    }

    public void setFetch(ZMailbox.Fetch fetch) {
        mFetch = fetch;
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

    public SearchSortBy getSortBy() {
        return mSortBy;
    }

    public void setSortBy(SearchSortBy sortBy) {
        mSortBy = sortBy;
    }

    public String getTypes() {
        return mTypes;
    }

    public void setTypes(String types) {
        mTypes = types;
    }
    
    public static class Cursor {
        
        private String mPreviousId;

        private String mPreviousSortValue;

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

        public Cursor(String prevoiusId, String previousSortValue) {
            mPreviousId = prevoiusId;
            mPreviousSortValue = previousSortValue;
        }
        
        public String getPreviousId() {
            return mPreviousId;
        }

        public String getPreviousSortValue() {
            return mPreviousSortValue;
        }
    }
}
