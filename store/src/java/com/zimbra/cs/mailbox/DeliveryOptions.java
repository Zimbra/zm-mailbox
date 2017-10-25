/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.mailbox;

import java.util.Collection;

import com.zimbra.cs.mailbox.MailItem.CustomMetadata;

/**
 * Specifies options for new messages created with {@link Mailbox#addMessage}.
 */
public class DeliveryOptions {

    private int mFolderId = -1;
    private boolean mNoICal = false;
    private int mFlags = 0;
    private String[] mTags = null;
    private int mConversationId = Mailbox.ID_AUTO_INCREMENT;
    private String mRecipientEmail = ":API:";
    private Message.DraftInfo mDraftInfo = null;
    private CustomMetadata mCustomMetadata = null;
    private Mailbox.MessageCallbackContext mCallbackContext = null;

    public int getFolderId() { return mFolderId; }
    public boolean getNoICal() { return mNoICal; }
    public int getFlags() { return mFlags; }
    public String[] getTags() { return mTags; }
    public int getConversationId() { return mConversationId; }
    public String getRecipientEmail() { return mRecipientEmail; }
    public Message.DraftInfo getDraftInfo() { return mDraftInfo; }
    public CustomMetadata getCustomMetadata() { return mCustomMetadata; }
    public Mailbox.MessageCallbackContext getCallbackContext() { return mCallbackContext; }

    public DeliveryOptions setFolderId(int folderId) {
        mFolderId = folderId;
        return this;
    }

    public DeliveryOptions setFolderId(Folder folder) {
        mFolderId = folder.getId();
        return this;
    }

    public DeliveryOptions setNoICal(boolean noICal) {
        mNoICal = noICal;
        return this;
    }

    public DeliveryOptions setFlags(int flags) {
        mFlags = flags;
        return this;
    }

    public DeliveryOptions setTags(Collection<String> tags) {
        mTags = tags == null ? null : tags.toArray(new String[tags.size()]);
        return this;
    }

    public DeliveryOptions setTags(String[] tags) {
        mTags = tags;
        return this;
    }

    public DeliveryOptions setConversationId(int conversationId) {
        mConversationId = conversationId;
        return this;
    }

    public DeliveryOptions setRecipientEmail(String recipientEmail) {
        mRecipientEmail = recipientEmail;
        return this;
    }

    public DeliveryOptions setDraftInfo(Message.DraftInfo draftInfo) {
        mDraftInfo = draftInfo;
        return this;
    }

    public DeliveryOptions setCustomMetadata(CustomMetadata customMetadata) {
        mCustomMetadata = customMetadata;
        return this;
    }

    public DeliveryOptions setCallbackContext(Mailbox.MessageCallbackContext callbackContext) {
        mCallbackContext = callbackContext;
        return this;
    }
}
