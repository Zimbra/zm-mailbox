/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import com.zimbra.client.event.ZModifyConversationEvent;
import com.zimbra.client.event.ZModifyEvent;
import com.zimbra.client.event.ZModifyMessageEvent;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.zclient.ZClientException;

public class ZConversation implements ZItem, ToZJSONObject {

    public enum Flag {
        unread('u'),
        draft('d'),
        flagged('f'),
        highPriority('!'),
        lowPriority('?'),
        sentByMe('s'),
        replied('r'),
        forwarded('w'),
        attachment('a');

        private char mFlagChar;

        public char getFlagChar() { return mFlagChar; }

        public static String toNameList(String flags) {
            if (flags == null || flags.length() == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i=0; i < flags.length(); i++) {
                String v = null;
                for (Flag f : Flag.values()) {
                    if (f.getFlagChar() == flags.charAt(i)) {
                        v = f.name();
                        break;
                    }
                }
                if (sb.length() > 0) sb.append(", ");
                sb.append(v == null ? flags.substring(i, i+1) : v);
            }
            return sb.toString();
        }

        Flag(char flagChar) {
            mFlagChar = flagChar;

        }
    }

    private String mId;
    private String mFlags;
    private String mSubject;
    private String mTags;
    private int mMessageCount;
    private List<ZMessageSummary> mMessageSummaries;
    private ZMailbox mMailbox;

    public ZConversation(Element e, ZMailbox mailbox) throws ServiceException {
        mId = e.getAttribute(MailConstants.A_ID);
        mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
        mTags = e.getAttribute(MailConstants.A_TAGS, null);
        mSubject = e.getAttribute(MailConstants.E_SUBJECT, null);
        mMessageCount = (int) e.getAttributeLong(MailConstants.A_NUM);

        mMessageSummaries = new ArrayList<ZMessageSummary>();
        for (Element msgEl: e.listElements(MailConstants.E_MSG)) {
            mMessageSummaries.add(new ZMessageSummary(msgEl));
        }
    }

    public void modifyNotification(ZModifyEvent event) throws ServiceException {
    	if (event instanceof ZModifyConversationEvent) {
    		ZModifyConversationEvent cevent = (ZModifyConversationEvent) event;
            if (cevent.getId().equals(mId)) {
                mFlags = cevent.getFlags(mFlags);
                mTags = cevent.getTagIds(mTags);
                mSubject = cevent.getSubject(mSubject);
                //mFragment = cevent.getFragment(mFragment);
                mMessageCount = cevent.getMessageCount(mMessageCount);
                //mRecipients = cevent.getRecipients(mRecipients);
            }
        }
    }

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public String getUuid() {
        return null;
    }

    public ZMailbox getMailbox() {
        return mMailbox;
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("id", mId);
        jo.put("tagIds", mTags);
        jo.put("flags", mFlags);
        jo.put("subject", mSubject);
        jo.put("messageCount", mMessageCount);
        jo.put("messageSummaries", mMessageSummaries);
        return jo;
    }

    @Override
    public String toString() {
        return String.format("[ZConversation %s]", mId);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public String getFlags() {
        return mFlags;
    }

    public String getSubject() {
        return mSubject;
    }

    public String getTagIds() {
        return mTags;
    }

    public int getMessageCount() {
        return mMessageCount;
    }

    public List<ZMessageSummary> getMessageSummaries() {
        return mMessageSummaries;
    }

    public class ZMessageSummary implements ZItem, ToZJSONObject {

        private long mDate;
        private String mFlags;
        private String mTags;
        private String mFragment;
        private String mId;
        private String mFolderId;
        private ZEmailAddress mSender;
        private long mSize;
        private Element mElement;
        private String mSubject;

        public ZMessageSummary(Element e) throws ServiceException {
            mElement = e;
            mId = e.getAttribute(MailConstants.A_ID);
            mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
            mDate = e.getAttributeLong(MailConstants.A_DATE);
            mTags = e.getAttribute(MailConstants.A_TAGS, null);
            mFolderId = e.getAttribute(MailConstants.A_FOLDER, null);
            mFragment = e.getAttribute(MailConstants.E_FRAG, null);
            mSize = e.getAttributeLong(MailConstants.A_SIZE);
            Element emailEl = e.getOptionalElement(MailConstants.E_EMAIL);
            if (emailEl != null) mSender = new ZEmailAddress(emailEl);
            mSubject = e.getAttribute(MailConstants.E_SUBJECT, null);
        }

        public Element getElement() { return mElement; }

        public void modifyNotification(ZModifyMessageEvent mevent) throws ServiceException {
            if (mevent.getId().equals(mId)) {
                mFlags = mevent.getFlags(mFlags);
                mTags = mevent.getTagIds(mTags);
                mFolderId = mevent.getFolderId(mFolderId);
            }
        }

        @Override
        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject jo = new ZJSONObject();
            jo.put("id", mId);
            jo.put("folderId", mFolderId);
            jo.put("flags", mFlags);
            jo.put("fragment", mFragment);
            jo.put("tags", mTags);
            jo.put("size", mSize);
            jo.put("sender", mSender);
            jo.put("date", mDate);
            jo.put("hasAttachment", hasAttachment());
            jo.put("hasFlags", hasFlags());
            jo.put("hasTags", hasTags());
            jo.put("isDeleted", isDeleted());
            jo.put("isDraft", isDraft());
            jo.put("isFlagged", isFlagged());
            jo.put("isHighPriority", isHighPriority());
            jo.put("isLowPriority", isLowPriority());
            jo.put("isForwarded", isForwarded());
            jo.put("isNotificationSent", isNotificationSent());
            jo.put("isRepliedTo", isRepliedTo());
            jo.put("isSentByMe", isSentByMe());
            jo.put("isUnread", isUnread());
            jo.put("subject", mSubject);
            return jo;
        }

        @Override
        public String toString() {
            return String.format("[ZMessageSummary %s]", mId);
        }

        public String dump() {
            return ZJSONObject.toString(this);
        }

        public long getDate() {
            return mDate;
        }

        public String getFlags() {
            return mFlags;
        }

        public String getFragment() {
            return mFragment;
        }

        @Override
        public String getId() {
            return mId;
        }

        @Override
        public String getUuid() {
            return null;
        }

        public ZEmailAddress getSender() {
            return mSender;
        }

        public long getSize() {
            return mSize;
        }

        public String getFolderId() {
            return mFolderId;
        }

        public String getTagIds() {
            return mTags;
        }

        public String getSubject() {
            return mSubject;
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

    public boolean isForwarded() {
        return hasFlags() && mFlags.indexOf(ZConversation.Flag.forwarded.getFlagChar()) != -1;
    }

    public boolean isRepliedTo() {
        return hasFlags() && mFlags.indexOf(ZConversation.Flag.replied.getFlagChar()) != -1;
    }

    public boolean isDraft() {
        return hasFlags() && mFlags.indexOf(ZConversation.Flag.draft.getFlagChar()) != -1;
    }

    public boolean isHighPriority() {
        return hasFlags() && mFlags.indexOf(ZConversation.Flag.highPriority.getFlagChar()) != -1;
    }

    public boolean isLowPriority() {
        return hasFlags() && mFlags.indexOf(ZConversation.Flag.lowPriority.getFlagChar()) != -1;
    }

    public void delete(String tc) throws ServiceException {
        getMailbox().deleteConversation(getId(), tc);
    }

    public void deleteItem(String tc) throws ServiceException {
        delete(tc);
    }

    public void trash(String tc) throws ServiceException {
        getMailbox().trashConversation(getId(), tc);
    }

    public void markRead(boolean read, String tc) throws ServiceException {
        getMailbox().markConversationRead(getId(), read, tc);
    }

    public void flag(boolean flag, String tc) throws ServiceException {
        getMailbox().flagConversation(getId(), flag, tc);
    }

    public void tag(String nameOrId, boolean tagged, String tc) throws ServiceException {
        ZTag tag = mMailbox.getTag(nameOrId);
        if (tag == null)
            throw ZClientException.CLIENT_ERROR("unknown tag: "+nameOrId, null);
        else
           tag(tag, tagged, tc);
    }

    public void tag(ZTag tag, boolean tagged, String tc) throws ServiceException {
        mMailbox.tagConversation(mId, tag.getId(), tagged, tc);
    }

    public void move(String pathOrId, String tc) throws ServiceException {
        ZFolder destFolder = mMailbox.getFolder(pathOrId);
        if (destFolder == null)
            throw ZClientException.CLIENT_ERROR("unknown folder: "+pathOrId, null);
        else
            move(destFolder, tc);
    }

    public void move(ZFolder destFolder, String tc) throws ServiceException {
        mMailbox.moveConversation(mId, destFolder.getId(), tc);
    }

    public void markSpam(boolean spam, String pathOrId, String tc) throws ServiceException {
        ZFolder destFolder = mMailbox.getFolder(pathOrId);
        if (destFolder == null)
            throw ZClientException.CLIENT_ERROR("unknown folder: "+pathOrId, null);
        else
            markSpam(spam, destFolder, tc);
    }

    public void markSpam(boolean spam, ZFolder destFolder, String tc) throws ServiceException {
        getMailbox().markConversationSpam(getId(), spam, destFolder == null ? null : destFolder.getId(), tc);
    }
}
