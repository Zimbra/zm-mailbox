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

package com.zimbra.cs.zclient.soap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZEmailAddress;
import com.zimbra.soap.Element;

class ZSoapMessage implements ZMessage {

    private String mId;
    private String mFlags;
    private String mSubject;
    private String mTags;
    private String mFolderId;
    private String mConversationId;
    private long mReceivedDate;
    private long mSentDate;
    private String mMessageIdHeader;
    private List<ZEmailAddress> mAddresses;
    private ZMimePart mMimeStructure;
    private String mContent;
    private String mContentURL;
    private long mSize;
        
    ZSoapMessage(Element e, Map<String,ZSoapEmailAddress> cache) throws ServiceException {
        mId = e.getAttribute(MailService.A_ID);
        mFlags = e.getAttribute(MailService.A_FLAGS, null);
        mTags = e.getAttribute(MailService.A_TAGS, null);
        mSubject = e.getElement(MailService.E_SUBJECT).getText();
        mMessageIdHeader = e.getElement(MailService.E_MSG_ID_HDR).getText();
        mReceivedDate = e.getAttributeLong(MailService.A_DATE, 0);
        mSentDate = e.getAttributeLong(MailService.A_SENT_DATE, 0);
        mFolderId = e.getAttribute(MailService.A_FOLDER);
        mConversationId = e.getAttribute(MailService.A_CONV_ID, null);
        mSize = e.getAttributeLong(MailService.A_SIZE);        
        Element content = e.getOptionalElement(MailService.E_CONTENT);
        if (content != null) {
            mContent = content.getText();
            mContentURL = content.getAttribute(MailService.A_URL, null);
        }
        
        mAddresses = new ArrayList<ZEmailAddress>();
        for (Element emailEl: e.listElements(MailService.E_EMAIL)) {
            mAddresses.add(ZSoapEmailAddress.getAddress(emailEl, cache));
        }
        Element mp = e.getOptionalElement(MailService.E_MIMEPART);
        if (mp != null)
            mMimeStructure = new ZSoapMimePart(mp);
    }

    public long getSize() {
        return mSize;
    }

    public String getId() {
        return mId;
    }

    ZSoapSB toString(ZSoapSB sb) {
        sb.beginStruct();
        sb.add("id", mId);
        sb.add("flags", mFlags);
        sb.add("tags", mTags);
        sb.add("subject", mSubject);
        sb.add("messageIdHeader", mMessageIdHeader);
        sb.addDate("receivedDate", mReceivedDate);
        sb.addDate("sentDate", mSentDate);
        sb.add("folderId", mFolderId);
        sb.add("conversationId", mConversationId);
        sb.add("size", mSize);
        sb.add("content", mContent);
        sb.add("contentURL", mContentURL);
        sb.add("addresses", mAddresses, false);
        sb.addStruct("mimeStructure", mMimeStructure.toString());
        sb.endStruct();        
        return sb;
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

    public String getContent() {
        return mContent;
    }

    public String getContentURL() {
        return mContentURL;
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
    
    private static class ZSoapMimePart implements ZMimePart {
        
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
        
        ZSoapMimePart(Element e) throws ServiceException {
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
                mChildren.add(new ZSoapMimePart(mpEl));
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
            sb.add("children", mChildren, false);
            sb.endStruct();
            return sb;
        }

        public String toString() {
            return toString(new ZSoapSB()).toString();
        }

        public List<ZMimePart> getChildren() {
            return mChildren;
        }

        public String getContent() {
            return mContent;
        }
        
        public String getContentType() {
            return mContentType;
        }

        public String getContentDescription() {
            return mContentDescription;
        }

        public String getContentDispostion() {
            return mContentDisposition;
        }

        public String getContentId() {
            return mContentId;
        }

        public String getContentLocation() {
            return mContentLocation;
        }

        public String getFileName() {
            return mFileName;
        }

        public String getName() {
            return mName;
        }

        public String getPartName() {
            return mPartName;
        }

        public boolean isBody() {
            return mIsBody;
        }
        
        public long getSize() {
            return mSize;
        }
    }

}
