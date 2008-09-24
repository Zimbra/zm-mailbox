/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.cs.zclient.event.ZModifyConversationEvent;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class ZSearchResult implements ToZJSONObject {

    private List<ZSearchHit> mHits;
    private ZConversationSummary mConvSummary;
    private boolean mHasMore;
    private String mSortBy;
    private int mOffset;

    private ZSearchResult(Element e) throws ServiceException {
        init(e, e, null);
    }

    public ZSearchResult(Element e, boolean convNest, TimeZone tz) throws ServiceException {
        if (!convNest) {
            init(e, e, tz);
        } else {
            Element c = e.getElement(MailConstants.E_CONV);
            mConvSummary = new ZConversationSummary(c);
            init(e, c, tz);
        }
    }

    public ZSearchResult(List<ZSearchHit> hits,
                         ZConversationSummary convSummary,
                         boolean hasMore,
                         String sortBy,
                         int offset) {
        mHits = hits;
        mConvSummary = convSummary;
        mHasMore = hasMore;
        mSortBy = sortBy;
        mOffset = offset;
    }

    private void init(Element resp, Element hits, TimeZone tz) throws ServiceException {
        mSortBy = resp.getAttribute(MailConstants.A_SORTBY);
        mHasMore = resp.getAttributeBool(MailConstants.A_QUERY_MORE);
        mOffset = (int) resp.getAttributeLong(MailConstants.A_QUERY_OFFSET);
        mHits = new ArrayList<ZSearchHit>();
        for (Element h: hits.listElements()) {
            if (h.getName().equals(MailConstants.E_CONV)) {
                mHits.add(new ZConversationHit(h));
            } else if (h.getName().equals(MailConstants.E_MSG)) {
                mHits.add(new ZMessageHit(h));
            } else if (h.getName().equals(MailConstants.E_CONTACT)) {
                mHits.add(new ZContactHit(h));
            } else if (h.getName().equals(MailConstants.E_APPOINTMENT)) {
                ZAppointmentHit.addInstances(h, mHits, tz, false);
            } else if (h.getName().equals(MailConstants.E_TASK)) {
                ZAppointmentHit.addInstances(h, mHits, tz, true);
            } else if (h.getName().equals(MailConstants.E_DOC)) {
                mHits.add(new ZDocumentHit(h));
            } else if (h.getName().equals(MailConstants.E_WIKIWORD)) {
                mHits.add(new ZDocumentHit(h));
	        } else if (h.getName().equals(VoiceConstants.E_VOICEMSG)) {
	        	mHits.add(new ZVoiceMailItemHit(h));
	        } else if (h.getName().equals(VoiceConstants.E_CALLLOG)) {
	        	mHits.add(new ZCallHit(h));
	        }
        }
    }

    /**
     * @return ZSearchHit objects from search
     */
    public List<ZSearchHit> getHits() {
        return mHits;
    }

    public ZConversationSummary getConversationSummary() {
        return mConvSummary;
    }
    
    /**
     * @return true if there are more search results on the server
     */
    public boolean hasMore() {
        return mHasMore;
    }
    
    /**
     * @return the sort by value
     */
    public String getSortBy() {
        return mSortBy;
    }

    /**
     * @return offset of the search
     */
    public int getOffset() {
        return mOffset;
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("more", mHasMore);
        zjo.put("sortBy", mSortBy);
        zjo.put("offset", mOffset);
        zjo.put("hits", mHits);
        return zjo;
    }

    public String toString() {
       return ZJSONObject.toString(this);
    }

    /*
     * TODO: this class is really not a ZSearchHit, but for now that works best do to ZSearchPagerCache. modifyNotication handling
     */
    public class ZConversationSummary implements ZSearchHit {

        private String mId;
        private String mFlags;
        private String mTags;
        private int mMessageCount;

        public ZConversationSummary(Element e) throws ServiceException {
            mId = e.getAttribute(MailConstants.A_ID);
            mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
            mTags = e.getAttribute(MailConstants.A_TAGS, null);
            mMessageCount = (int) e.getAttributeLong(MailConstants.A_NUM);
        }

        public void modifyNotification(ZModifyEvent event) throws ServiceException {
            if (event instanceof ZModifyConversationEvent) {
                ZModifyConversationEvent cevent = (ZModifyConversationEvent) event;
                mFlags = cevent.getFlags(mFlags);
                mTags = cevent.getTagIds(mTags);
                mMessageCount = cevent.getMessageCount(mMessageCount);
            }
        }

        public String getId() {
            return mId;
        }

        public String getSortField() {
            return null;
        }

        public float getScore() {
            return 0;
        }

        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("id", mId);
            zjo.put("flags", mFlags);
            zjo.put("tags", mTags);
            zjo.put("messageCount", mMessageCount);
            return zjo;
        }

        public String toString() {
            return ZJSONObject.toString(this);
        }

        public String getFlags() {
            return mFlags;
        }
        
        public String getTagIds() {
            return mTags;
        }

        public int getMessageCount() {
            return mMessageCount;
        }

        public boolean hasFlags() {
            return mFlags != null && mFlags.length() > 0;
        }

        public boolean hasTags() {
            return mTags != null && mTags.length() > 0;
        }

        public boolean hasAttachment() {
            return hasFlags() && mFlags.indexOf(ZConversation.Flag.attachment.getFlagChar()) != -1;
        }

        public boolean isFlagged() {
            return hasFlags() && mFlags.indexOf(ZConversation.Flag.flagged.getFlagChar()) != -1;
        }

        public boolean isSentByMe() {
            return hasFlags() && mFlags.indexOf(ZConversation.Flag.sentByMe.getFlagChar()) != -1;
        }

        public boolean isUnread() {
            return hasFlags() && mFlags.indexOf(ZConversation.Flag.unread.getFlagChar()) != -1;
        }

        public boolean isDraft() {
            return hasFlags() && mFlags.indexOf(ZConversation.Flag.draft.getFlagChar()) != -1;
        }
    }
}
