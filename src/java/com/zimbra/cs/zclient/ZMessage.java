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
import com.zimbra.common.soap.Element;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.cs.zclient.event.ZModifyMessageEvent;

import java.util.ArrayList;
import java.util.List;

public class ZMessage implements ZItem {

    public enum Flag {
        unread('u'),
        flagged('f'),
        highPriority('!'),
        lowPriority('?'),
        attachment('a'),
        replied('r'),
        sentByMe('s'),
        forwarded('w'),
        draft('d'),
        deleted('x'),
        notificationSent('n');

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
    private String mFragment;
    private String mTags;
    private String mFolderId;
    private String mConversationId;
    private String mPartName;
    private long mReceivedDate;
    private long mSentDate;
    private String mMessageIdHeader;
    private List<ZEmailAddress> mAddresses;
    private ZMimePart mMimeStructure;
    private String mContent;
    private String mContentURL;
    private long mSize;
    private String mReplyType;
    private String mInReplyTo;
    private String mOrigId;
    private ZInvite mInvite;
    private ZShare mShare;
        
    public ZMessage(Element e) throws ServiceException {
        mId = e.getAttribute(MailConstants.A_ID);
        mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
        mTags = e.getAttribute(MailConstants.A_TAGS, null);
        mReplyType = e.getAttribute(MailConstants.A_REPLY_TYPE, null);
        mOrigId = e.getAttribute(MailConstants.A_ORIG_ID, null);
        mSubject = e.getAttribute(MailConstants.E_SUBJECT, null);
        mFragment = e.getAttribute(MailConstants.E_FRAG, null);
        mMessageIdHeader = e.getAttribute(MailConstants.E_MSG_ID_HDR, null);
        mInReplyTo = e.getAttribute(MailConstants.E_IN_REPLY_TO, null);

        mReceivedDate = e.getAttributeLong(MailConstants.A_DATE, 0);
        mSentDate = e.getAttributeLong(MailConstants.A_SENT_DATE, 0);
        mFolderId = e.getAttribute(MailConstants.A_FOLDER, null);
        mConversationId = e.getAttribute(MailConstants.A_CONV_ID, null);
        mPartName = e.getAttribute(MailConstants.A_PART, null);
        mSize = e.getAttributeLong(MailConstants.A_SIZE, -1);
        
        Element content = e.getOptionalElement(MailConstants.E_CONTENT);
        if (content != null) {
            mContent = content.getText();
            mContentURL = content.getAttribute(MailConstants.A_URL, null);
        }
        
        mAddresses = new ArrayList<ZEmailAddress>();
        for (Element emailEl: e.listElements(MailConstants.E_EMAIL)) {
            mAddresses.add(new ZEmailAddress(emailEl));
        }
        Element mp = e.getOptionalElement(MailConstants.E_MIMEPART);
        if (mp != null)
            mMimeStructure = new ZMimePart(null, mp);

        Element inviteEl = e.getOptionalElement(MailConstants.E_INVITE);
        if (inviteEl != null)
            mInvite = new ZInvite(inviteEl);

        Element shrEl = e.getOptionalElement("shr");
        if (shrEl != null) {
            String shareContent = shrEl.getAttribute(MailConstants.E_CONTENT);
            if (shareContent != null) {
                mShare = ZShare.parseXml(shareContent);
            }
        }
    }

    public void modifyNotification(ZModifyEvent event) throws ServiceException {
    	if (event instanceof ZModifyMessageEvent) {
    		ZModifyMessageEvent mevent = (ZModifyMessageEvent) event;
            if (mevent.getId().equals(mId)) {
                mFlags = mevent.getFlags(mFlags);
                mTags = mevent.getTagIds(mTags);
                mFolderId = mevent.getFolderId(mFolderId);
                mConversationId = mevent.getConversationId(mConversationId);
            }
        }
    }

    public  ZShare getShare() {
        return mShare;
    }


    /**
     *
     * @return invite object if this message contains an invite, null otherwise.
     */
    public ZInvite getInvite() {
        return mInvite;
    }

    /**
     * 
     * @return Zimbra id of message we are replying to if this is a draft.
     */
    public String getOriginalId() {
        return mOrigId;
    }
    
    /**
     * 
     * @return message-id header of message we are replying to if this is a draft
     */
    public String getInReplyTo() {
        return mInReplyTo;
    }
    
    /**
     * 
     * @return reply type if this is a draft
     */
    public String getReplyType() {
        return mReplyType;
    }
    
    public long getSize() {
        return mSize;
    }

    public String getId() {
        return mId;
    }

    public boolean hasFlags() {
        return mFlags != null && mFlags.length() > 0;        
    }

    public boolean hasTags() {
        return mTags != null && mTags.length() > 0;
    }

    ZSoapSB toString(ZSoapSB sb) {
        sb.beginStruct();
        sb.add("id", mId);
        sb.add("flags", mFlags);
        sb.add("tags", mTags);
        sb.add("subject", mSubject);
        sb.add("fragment", mFragment);
        sb.add("partName", mPartName);
        sb.add("messageIdHeader", mMessageIdHeader);
        sb.addDate("receivedDate", mReceivedDate);
        sb.addDate("sentDate", mSentDate);
        sb.add("folderId", mFolderId);
        sb.add("conversationId", mConversationId);
        sb.add("size", mSize);
        sb.add("content", mContent);
        sb.add("contentURL", mContentURL);
        sb.add("addresses", mAddresses, false, true);
        sb.addStruct("mimeStructure", mMimeStructure.toString());
        if (mInvite != null)
                sb.addStruct("invite", mInvite.toString());
        if (mShare != null)
            sb.addStruct("share", mShare.toString());
        sb.endStruct();
        return sb;
    }

    /**
     *
     * @return the part name if this message is actually a part of another message
     */
    public String getPartName() {
        return mPartName;
    }
    
    public String toString() {
        return toString(new ZSoapSB()).toString();
    }
    
    public String getFlags() {
        return mFlags;
    }

    public String getSubject() {
        return mSubject;
    }

    public String getFragment() {
        return mFragment;
    }

    public String getTagIds() {
        return mTags;
    }

    public String getConversationId() {
        return mConversationId;
    }

    public List<ZEmailAddress> getEmailAddresses() {
        return mAddresses;
    }

    public String getFolderId() {
        return mFolderId;
    }

    public String getMessageIdHeader() {
        return mMessageIdHeader;
    }

    public ZMimePart getMimeStructure() {
        return mMimeStructure;
    }

    public long getReceivedDate() {
        return mReceivedDate;
    }

    public long getSentDate() {
        return mSentDate;
    }
    
    /** content of the message, if raw is specified. if message too big or not ASCII, a content servlet URL is returned */
    public String getContent() {
        return mContent;
    }
    
    /** if raw is specified and message too big or not ASCII, a content servlet URL is returned */
    public String getContentURL() {
        return mContentURL;
    }
    
    public static class ZMimePart {
        private String mPartName;
        private String mName;
        private String mContentType;
        private String mContentDisposition;
        private String mFileName;
        private String mContentId;
        private String mContentLocation;
        private String mContentDescription;
        private String mContent;
        private boolean mIsBody;
        private List<ZMimePart> mChildren;
        private long mSize;
        private ZMimePart mParent;
        
        public ZMimePart(ZMimePart parent, Element e) throws ServiceException {
            mParent = parent;
            mPartName = e.getAttribute(MailConstants.A_PART);
            mName = e.getAttribute(MailConstants.A_NAME, null);
            mContentType = e.getAttribute(MailConstants.A_CONTENT_TYPE, null);
            mContentDisposition = e.getAttribute(MailConstants.A_CONTENT_DISPOSTION, null);
            mFileName = e.getAttribute(MailConstants.A_CONTENT_FILENAME, null);
            mContentId = e.getAttribute(MailConstants.A_CONTENT_ID, null);
            mContentDescription = e.getAttribute(MailConstants.A_CONTENT_DESCRIPTION, null);
            mContentLocation = e.getAttribute(MailConstants.A_CONTENT_LOCATION, null);
            mIsBody = e.getAttributeBool(MailConstants.A_BODY, false);
            mSize = e.getAttributeLong(MailConstants.A_SIZE, 0);
            mContent = e.getAttribute(MailConstants.E_CONTENT, null);
            mChildren = new ArrayList<ZMimePart>();
            for (Element mpEl: e.listElements(MailConstants.E_MIMEPART)) {
                mChildren.add(new ZMimePart(this, mpEl));
            }
        }

        ZSoapSB toString(ZSoapSB sb) {
            sb.beginStruct();
            sb.add("partName", mPartName);
            sb.add("content", mContent);
            sb.add("contentType", mContentType);
            sb.add("contentDisposition", mContentDisposition);
            sb.add("contentId", mContentId);
            sb.add("contentLocation", mContentLocation);
            sb.add("contentDescription", mContentDescription);
            sb.add("isBody", mIsBody);
            sb.add("size", mSize);
            sb.add("name", mName);
            sb.add("fileName", mFileName);
            sb.add("children", mChildren, false, false);
            sb.endStruct();
            return sb;
        }

        public ZMimePart getParent() {
            return mParent;
        }
        
        /** "" means top-level part, 1 first part, 1.1 first part of a multipart inside of 1. */
        public String getPartName() {
            return mPartName;
        }
        
        /** name attribute from the Content-Type param list */
        public String getName() {
            return mName;
        }

        /** MIME Content-Type */
        public String getContentType() {
            return mContentType;
        }

        /** MIME Content-Disposition */
        public String getContentDispostion() {
            return mContentDisposition;
        }
        
        /** filename attribute from the Content-Disposition param list */
        public String getFileName() {
            return mFileName;
        }
        
        /** MIME Content-ID (for display of embedded images) */
        public String getContentId() {
            return mContentId;
        }
        
        /** MIME/Microsoft Content-Location (for display of embedded images) */
        public String getContentLocation() {
            return mContentLocation;
        }
        
        /** MIME Content-Description.  Note cont-desc is not currently used in the code. */
        public String getContentDescription() {
            return mContentDescription;
        }
        
        /** content of the part, if requested */
        public String getContent() {
            return mContent;
        }
        
        /** set to 1, if this part is considered to be the "body" of the message for display purposes */
        public boolean isBody() {
            return mIsBody;
        }
        
        /** get child parts */
        public List<ZMimePart> getChildren() {
            return mChildren;
        }
        
        public long getSize() {
            return mSize;
        }
        
        public String toString() {
            return toString(new ZSoapSB()).toString();
        }

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
    
    public boolean isHighPriority() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.highPriority.getFlagChar()) != -1;
    }

    public boolean isLowPriority() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.lowPriority.getFlagChar()) != -1;
    }

}
