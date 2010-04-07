/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;


import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.cs.zclient.ZMailbox.SearchSortBy;
import org.json.JSONException;

import java.util.TimeZone;

public class ZSearchParams implements ToZJSONObject {

    public static final String TYPE_CONVERSATION = "conversation";

    public static final String TYPE_GAL = "gal";
    
    public static final String TYPE_MESSAGE = "message";
    
    public static final String TYPE_CONTACT = "contact";
    
    public static final String TYPE_VOICE_MAIL = "voicemail";

    public static final String TYPE_CALL = "calllog";

    public static final String TYPE_APPOINTMENT = "appointment";

    public static final String TYPE_TASK = "task";

    public static final String TYPE_DOCUMENT = "document";

    public static final String TYPE_BRIEFCASE = "briefcase";
    
    public static final String TYPE_WIKI = "wiki";

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
            else if (s.startsWith("d") && TYPE_DOCUMENT.startsWith(s))
                sb.append(TYPE_DOCUMENT);
            else if (s.startsWith("b") && TYPE_BRIEFCASE.startsWith(s))
                sb.append(TYPE_BRIEFCASE);
            else if (s.startsWith("w") && TYPE_WIKI.startsWith(s))
                sb.append(TYPE_WIKI);
            else if (s.startsWith("t") && TYPE_TASK.startsWith(s))
                sb.append(TYPE_TASK);
            else if (s.startsWith("g") && TYPE_GAL.startsWith(s))
                sb.append(TYPE_GAL);
            else
                throw ZClientException.CLIENT_ERROR("invalid search type: "+s, null);
        }
        return sb.toString();
    }
    
    /**
     *  max number of results to return
     */
    private int mLimit = 100;
    
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
     * the field to use for text content without an operator.
     */
    private String mField;
    /**
     * the search query to run
     */
    private String mQuery;
    
    private Cursor mCursor;

    private long mCalExpandInstStart;
    private long mCalExpandInstEnd;

    private TimeZone mTimeZone;
    /**
     * used only for equals/hascode purposes
     */
    private String mConvId;

    public int hashCode() {
        if (mConvId != null)
            return (mQuery+mConvId).hashCode();
        else
            return mQuery.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (! (obj instanceof ZSearchParams)) return false;

        ZSearchParams that = (ZSearchParams) obj;

        if (    this.mLimit != that.mLimit ||
                this.mOffset != that.mOffset ||
                this.mSortBy != that.mSortBy ||
                this.mFetch != that.mFetch ||
                this.mPreferHtml != that.mPreferHtml ||
                this.mMarkAsRead != that.mMarkAsRead ||
                this.mRecipientMode != that.mRecipientMode ||
                this.mCalExpandInstStart != that.mCalExpandInstStart ||
                this.mCalExpandInstEnd != that.mCalExpandInstEnd)
            return false;

        if (    !StringUtil.equal(this.mTypes, that.mTypes) ||
                !StringUtil.equal(this.mQuery, that.mQuery) ||
                !StringUtil.equal(this.mConvId, that.mConvId) ||
                !StringUtil.equal(this.mField, that.mField))
            return false;

        if ((this.mTimeZone == null && that.mTimeZone != null) ||
                (this.mTimeZone != null && !this.mTimeZone.equals(that.mTimeZone)))
            return false;

        if (this.mCursor == null || that.mCursor == null)
            return this.mCursor == that.mCursor;
        else
            return this.mCursor.equals(that.mCursor);
    }

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
        this.mField = that.mField;
        this.mConvId = that.mConvId;
        this.mCalExpandInstEnd = that.mCalExpandInstEnd;
        this.mCalExpandInstStart = that.mCalExpandInstStart;
        this.mTimeZone = that.mTimeZone;
    }

    public ZSearchParams(String query) {
        mQuery = query;
    }

    /**
     * init search params (query, types, sortBy) from a search folder.
     * @param folder init from a search folder.
     */
    public ZSearchParams(ZSearchFolder folder) {
        mQuery = folder.getQuery();
        mTypes = folder.getTypes();
        mSortBy = folder.getSortBy();
    }

    public void setTimeZone(TimeZone tz) { this.mTimeZone = tz; }

    public TimeZone getTimeZone() { return this.mTimeZone; }
    
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

    public void setConvId(String convId) {
        mConvId = convId;
    }

    public String getConvId() {
        return mConvId;
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

    public long getCalExpandInstStart() {
        return mCalExpandInstStart;
    }

    public void setCalExpandInstStart(long calExpandInstStart) {
        mCalExpandInstStart = calExpandInstStart;
    }

    public long getCalExpandInstEnd() {
        return mCalExpandInstEnd;
    }

    public void setCalExpandInstEnd(long calExpandInstEnd) {
        mCalExpandInstEnd = calExpandInstEnd;
    }

    public String getField() {
        return mField;
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        if (mFetch != null) zjo.put("fetch", mFetch.name());
        zjo.put("limit", mLimit);
        zjo.put("markAsRead", mMarkAsRead);
        zjo.put("offset", mOffset);
        zjo.put("preferHtml", mPreferHtml);
        zjo.put("query", mQuery);
        zjo.put("recipientMode", mRecipientMode);
        if (mSortBy != null) zjo.put("sortBy", mSortBy.name());
        zjo.put("types", mTypes);
        zjo.put("field", mField);
        zjo.put("convId", mConvId);
        zjo.put("calExpandInstEnd", mCalExpandInstEnd);
        zjo.put("calExpandInstStart", mCalExpandInstStart);
        if (mTimeZone != null) zjo.put("timeZone", mTimeZone.getID());
        if (mCursor != null) zjo.put("cursor", mCursor);
        return zjo;
    }

    public String toString() {
       return String.format("[ZSearchParams %s]", mQuery == null ? "" : mQuery);
    }

    public String dump() {
       return ZJSONObject.toString(this);
    }

    /**
     *  by default, text without an operator searches the CONTENT field.  By setting the
     * {field} value, you can control the default operator. Specify any of the text operators that are
     * available in query.txt, e.g. 'content:' [the default] or 'subject:', etc.  The date operators
     * (date, after, before) and the "item:" operator should not be specified as default fields
     * because of quirks in the search grammar.
     *
     * @param field
     */
    public void setField(String field) {
        mField = field;
    }
    
    public static class Cursor implements ToZJSONObject {
        
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

        public int hashCode() {
            return (mPreviousId + mPreviousSortValue).hashCode();
        }

        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (! (obj instanceof Cursor)) return false;
            Cursor that = (Cursor) obj;
            if (!StringUtil.equal(this.mPreviousId, that.mPreviousId)) return false;
            if (!StringUtil.equal(this.mPreviousSortValue, that.mPreviousSortValue)) return false;
            return true;
        }

        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("previousId", mPreviousId);
            zjo.put("previousSortValue", mPreviousSortValue);
            return zjo;
        }

        public String toString() {
            return String.format("[Cursor id=%s sort=%s]", mPreviousId, mPreviousSortValue);
        }

        public String dump() {
            return ZJSONObject.toString(this);
        }

    }
}
