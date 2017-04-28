/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.client;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.client.event.ZModifyEvent;
import com.zimbra.client.event.ZModifyMessageEvent;
import org.json.JSONException;

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
    private long mDate;
    private int mSize;
    private boolean mContentMatched;
    private List<String> mMimePartHits;
    private ZEmailAddress mSender;
    private List<ZEmailAddress> mAddresses;
    private ZMessage mMessage;
    private boolean mIsInvite;
    private long mAutoSendTime;

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
        mContentMatched = e.getAttributeBool(MailConstants.A_CONTENTMATCHED, false);
        mAutoSendTime = e.getAttributeLong(MailConstants.A_AUTO_SEND_TIME, -1);
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
            mMessage = new ZMessage(e, null); // TODO: pass in ref
        }
        mIsInvite = e.getOptionalElement(MailConstants.E_INVITE) != null;
    }

    @Override
    public String getId() {
        return mId;
    }

    public boolean getContentMatched() {
        return mContentMatched;
    }

    public boolean getIsInvite() {
        return mIsInvite;
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("id", mId);
        zjo.put("conversationId", mConvId);
        zjo.put("flags", mFlags);
        zjo.put("isInvite", mIsInvite);
        zjo.put("fragment", mFragment);
        zjo.put("subject", mSubject);
        zjo.put("date", mDate);
        zjo.put("size", mSize);
        zjo.put("sender", mSender);
        zjo.put("sortField", mSortField);
        zjo.putList("mimePartHits", mMimePartHits);
        zjo.put("addresses", mAddresses);
        zjo.put("message", mMessage);
        return zjo;
    }

    @Override
    public String toString() {
        return String.format("[ZMessageHit %s]", mId);
    }

    public String dump() {
        return ZJSONObject.toString(this);
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

    @Override
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
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.ATTACHED.getFlagChar()) != -1;
    }

    public boolean isDeleted() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.DELETED.getFlagChar()) != -1;
    }

    public boolean isDraft() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.DRAFT.getFlagChar()) != -1;
    }

    public boolean isFlagged() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.FLAGGED.getFlagChar()) != -1;
    }

    public boolean isHighPriority() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.HIGH_PRIORITY.getFlagChar()) != -1;
    }

    public boolean isLowPriority() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.LOW_PRIORITY.getFlagChar()) != -1;
    }

    public boolean isForwarded() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.FORWARDED.getFlagChar()) != -1;
    }

    public boolean isNotificationSent() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.NOTIFIED.getFlagChar()) != -1;
    }

    public boolean isRepliedTo() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.REPLIED.getFlagChar()) != -1;
    }

    public boolean isSentByMe() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.FROM_ME.getFlagChar()) != -1;
    }

    public boolean isUnread() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.UNREAD.getFlagChar()) != -1;
    }

    public String getFolderId() {
        return mFolderId;
    }

    public long getAutoSendTime() {
        return mAutoSendTime;
    }

    @Override
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
