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
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.cs.zclient.event.ZModifyMessageEvent;
import com.zimbra.common.soap.Element;

import java.util.ArrayList;
import java.util.List;

public class ZMessageHit implements ZSearchHit {

    private String mId;
    private String mFlags;
    private String mFragment;
    private String mSubject;
    private String mSortField;
    private String mTags;
    private String mConvId;
    private String mFolderId;
    private float mScore;
    private long mDate;
    private int mSize;
    private boolean mContentMatched;
    private List<String> mMimePartHits;
    private ZEmailAddress mSender;
    private List<ZEmailAddress> mAddresses;
    private ZMessage mMessage;
    private boolean mIsInvite;

    public ZMessageHit(Element e) throws ServiceException {
        mId = e.getAttribute(MailConstants.A_ID);
        mFolderId = e.getAttribute(MailConstants.A_FOLDER);
        mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
        mDate = e.getAttributeLong(MailConstants.A_DATE);
        mTags = e.getAttribute(MailConstants.A_TAGS, null);
        mFragment = e.getAttribute(MailConstants.E_FRAG, null);
        mSubject = e.getAttribute(MailConstants.E_SUBJECT, null);
        mSortField = e.getAttribute(MailConstants.A_SORT_FIELD, null);
        mSize = (int) e.getAttributeLong(MailConstants.A_SIZE);
        mConvId = e.getAttribute(MailConstants.A_CONV_ID);
        mScore = (float) e.getAttributeDouble(MailConstants.A_SCORE, 0);
        mContentMatched = e.getAttributeBool(MailConstants.A_CONTENTMATCHED, false);
        mMimePartHits = new ArrayList<String>();
        for (Element hp: e.listElements(MailConstants.E_HIT_MIMEPART)) {
            mMimePartHits.add(hp.getAttribute(MailConstants.A_PART));
        }
        for (Element emailEl : e.listElements(MailConstants.E_EMAIL)) {
            String t = emailEl.getAttribute(MailConstants.A_ADDRESS_TYPE, null);
            if (ZEmailAddress.EMAIL_TYPE_FROM.equals(t)) {
                mSender = new ZEmailAddress(emailEl);
                break;
            }
        }
        mAddresses = new ArrayList<ZEmailAddress>();
        for (Element emailEl : e.listElements(MailConstants.E_EMAIL)) {
            mAddresses.add(new ZEmailAddress(emailEl));
        }

        Element mp = e.getOptionalElement(MailConstants.E_MIMEPART);
        if (mp != null) {
            mMessage = new ZMessage(e);
        }
        mIsInvite = e.getOptionalElement(MailConstants.E_INVITE) != null;
    }

    public String getId() {
        return mId;
    }

    public boolean getContentMatched() {
        return mContentMatched;
    }

    public boolean getIsInvite() {
        return mIsInvite;
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("id", mId);
        sb.add("conversationId", mConvId);
        sb.add("flags", mFlags);
        sb.add("isInvite", mIsInvite);
        sb.add("fragment", mFragment);
        sb.add("subject", mSubject);
        sb.addDate("date", mDate);
        sb.add("size", mSize);
        if (mSender != null) sb.addStruct("sender", mSender.toString());
        sb.add("sortField", mSortField);
        sb.add("score", mScore);
        sb.add("mimePartHits", mMimePartHits, true, false);
        sb.add("addresses", mAddresses, false, true);
        if (mMessage != null) sb.addStruct("message", mMessage.toString());
        sb.endStruct();
        return sb.toString();
    }

    public String getFlags() {
        return mFlags;
    }

    public long getDate() {
        return mDate;
    }

    public String getFragment() {
        return mFragment;
    }

    public String getSortField() {
        return mSortField;
    }

    public String getSubject() {
        return mSubject;
    }

    public String getTagIds() {
        return mTags;
    }
    
    public String getConversationId() {
        return mConvId;
    }

    public List<String> getMimePartHits() {
        return mMimePartHits;
    }

    public ZMessage getMessage() {
        return mMessage;
    }

    public float getScore() {
        return mScore;
    }

    public List<ZEmailAddress> getAddresses() {
        return mAddresses;
    }

    public ZEmailAddress getSender() {
        return mSender;
    }

    public long getSize() {
        return mSize;
    }

    public boolean hasFlags() {
        return mFlags != null && mFlags.length() > 0;
    }

    public boolean hasTags() {
        return mTags != null && mTags.length() > 0;
    }
    public boolean hasAttachment() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.attachment.getFlagChar()) != -1;
    }

    public boolean isDeleted() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.deleted.getFlagChar()) != -1;
    }

    public boolean isDraft() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.draft.getFlagChar()) != -1;
    }

    public boolean isFlagged() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.flagged.getFlagChar()) != -1;
    }

    public boolean isHighPriority() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.highPriority.getFlagChar()) != -1;
    }

    public boolean isLowPriority() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.lowPriority.getFlagChar()) != -1;
    }

    public boolean isForwarded() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.forwarded.getFlagChar()) != -1;
    }

    public boolean isNotificationSent() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.notificationSent.getFlagChar()) != -1;
    }

    public boolean isRepliedTo() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.replied.getFlagChar()) != -1;
    }

    public boolean isSentByMe() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.sentByMe.getFlagChar()) != -1;
    }

    public boolean isUnread() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.unread.getFlagChar()) != -1;
    }

    public String getFolderId() {
        return mFolderId;
    }

	public void modifyNotification(ZModifyEvent event) throws ServiceException {
		if (event instanceof ZModifyMessageEvent) {
			ZModifyMessageEvent mevent = (ZModifyMessageEvent) event;
            mFlags = mevent.getFlags(mFlags);
            mTags = mevent.getTagIds(mTags);
            mFolderId = mevent.getFolderId(mFolderId);
            mConvId = mevent.getConversationId(mConvId);
            /* updated fetched message if we have one */
            if (getMessage() != null)
                getMessage().modifyNotification(event);
        }
	}
}
