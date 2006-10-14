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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.Element;

public class ZMessage implements ZItem {

    public enum Flag {
        unread('u'),
        flagged('f'),
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
        
    public ZMessage(Element e, Map<String,ZEmailAddress> cache) throws ServiceException {
        mId = e.getAttribute(MailService.A_ID);
        mFlags = e.getAttribute(MailService.A_FLAGS, null);
        mTags = e.getAttribute(MailService.A_TAGS, null);
        Element sub = e.getOptionalElement(MailService.E_SUBJECT);
        if (sub != null) mSubject = sub.getText();
        Element mid = e.getOptionalElement(MailService.E_MSG_ID_HDR);
        if (mid != null) mMessageIdHeader = mid.getText();
        mReceivedDate = e.getAttributeLong(MailService.A_DATE, 0);
        mSentDate = e.getAttributeLong(MailService.A_SENT_DATE, 0);
        mFolderId = e.getAttribute(MailService.A_FOLDER, null);
        mConversationId = e.getAttribute(MailService.A_CONV_ID, null);
        mPartName = e.getAttribute(MailService.A_PART, null);
        mSize = e.getAttributeLong(MailService.A_SIZE);
        Element content = e.getOptionalElement(MailService.E_CONTENT);
        if (content != null) {
            mContent = content.getText();
            mContentURL = content.getAttribute(MailService.A_URL, null);
        }
        
        mAddresses = new ArrayList<ZEmailAddress>();
        for (Element emailEl: e.listElements(MailService.E_EMAIL)) {
            mAddresses.add(ZEmailAddress.getAddress(emailEl, cache));
        }
        Element mp = e.getOptionalElement(MailService.E_MIMEPART);
        if (mp != null)
            mMimeStructure = new ZMimePart(mp);
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
    
    public class ZMimePart {
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
        
        public ZMimePart(Element e) throws ServiceException {
            mPartName = e.getAttribute(MailService.A_PART);
            mName = e.getAttribute(MailService.A_NAME, null);
            mContentType = e.getAttribute(MailService.A_CONTENT_TYPE, null);            
            mContentDisposition = e.getAttribute(MailService.A_CONTENT_DISPOSTION, null);
            mFileName = e.getAttribute(MailService.A_CONTENT_FILENAME, null);
            mContentId = e.getAttribute(MailService.A_CONTENT_ID, null);
            mContentDescription = e.getAttribute(MailService.A_CONTENT_DESCRIPTION, null);
            mContentLocation = e.getAttribute(MailService.A_CONTENT_LOCATION, null);
            mIsBody = e.getAttributeBool(MailService.A_BODY, false);
            mSize = e.getAttributeLong(MailService.A_SIZE, 0);
            Element content = e.getOptionalElement(MailService.E_CONTENT);
            if (content != null) {
                mContent = content.getText();
            }
            
            mChildren = new ArrayList<ZMimePart>();
            for (Element mpEl: e.listElements(MailService.E_MIMEPART)) {
                mChildren.add(new ZMimePart(mpEl));
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

}
