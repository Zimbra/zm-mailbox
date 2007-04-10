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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.cs.zclient.event.ZModifyConversationEvent;
import com.zimbra.common.soap.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class ZSearchResult {

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
                ZAppointmentHit.addInstances(h, mHits, tz);
            } else if (h.getName().equals(MailConstants.E_DOC)) {
                mHits.add(new ZDocumentHit(h));
            } else if (h.getName().equals(MailConstants.E_WIKIWORD)) {
                mHits.add(new ZDocumentHit(h));
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

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("more", mHasMore);
        sb.add("sortBy", mSortBy);
        sb.add("offset", mOffset);
        sb.add("hits", mHits, false, true);
        sb.endStruct();
        return sb.toString();
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

        public String toString() {
            ZSoapSB sb = new ZSoapSB();
            sb.beginStruct();
            sb.add("id", mId);
            sb.add("flags", mFlags);
            sb.add("tags", mTags);
            sb.add("messageCount", mMessageCount);
            sb.endStruct();
            return sb.toString();
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
