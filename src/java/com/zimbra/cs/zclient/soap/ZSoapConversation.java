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
import com.zimbra.cs.zclient.ZConversation;
import com.zimbra.cs.zclient.ZEmailAddress;
import com.zimbra.soap.Element;

class ZSoapConversation implements ZConversation {

    private String mId;
    private String mFlags;
    private String mSubject;
    private String mTags;
    private int mMessageCount;
    private List<ZMessageSummary> mMessageSummaries;
        
    ZSoapConversation(Element e, Map<String,ZSoapEmailAddress> cache) throws ServiceException {
        mId = e.getAttribute(MailService.A_ID);
        mFlags = e.getAttribute(MailService.A_FLAGS, "");
        mTags = e.getAttribute(MailService.A_TAGS, "");
        mSubject = e.getElement(MailService.E_SUBJECT).getText();        
        mMessageCount = (int) e.getAttributeLong(MailService.A_NUM);
        
        mMessageSummaries = new ArrayList<ZMessageSummary>();
        for (Element msgEl: e.listElements(MailService.E_MSG)) {
            mMessageSummaries.add(new ZSoapMessageSummary(msgEl, cache));
        }        
    }

    public String getId() {
        return mId;
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("id", mId);
        sb.add("tags", mTags);
        sb.add("flags", mFlags);
        sb.add("subject", mSubject);
        sb.add("messageCount", mMessageCount);
        sb.add("messages", mMessageSummaries, false);
        sb.endStruct();
        return sb.toString();
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
    
    private static class ZSoapMessageSummary implements ZMessageSummary {

        private long mDate;
        private String mFlags;
        private String mTags;        
        private String mFragment;
        private String mId;
        private ZEmailAddress mSender;
        private long mSize;
        
        ZSoapMessageSummary(Element e, Map<String,ZSoapEmailAddress> cache) throws ServiceException {
            mId = e.getAttribute(MailService.A_ID);
            mFlags = e.getAttribute(MailService.A_FLAGS, "");
            mDate = e.getAttributeLong(MailService.A_DATE);
            mTags = e.getAttribute(MailService.A_TAGS, "");
            mFragment = e.getElement(MailService.E_FRAG).getText();        
            mSize = e.getAttributeLong(MailService.A_SIZE);
            Element emailEl = e.getOptionalElement(MailService.E_EMAIL);
            if (emailEl != null) mSender = ZSoapEmailAddress.getAddress(emailEl, cache); 
        }
        

        public String toString() {
            ZSoapSB sb = new ZSoapSB();
            sb.beginStruct();
            sb.add("id", mId);
            sb.add("flags", mFlags);
            sb.add("fragment", mFragment);
            sb.add("tags", mTags);
            sb.add("size", mSize);
            sb.addStruct("sender", mSender.toString());
            sb.addDate("date", mDate);
            sb.endStruct();
            return sb.toString();
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

        public String getId() {
            return mId;
        }

        public ZEmailAddress getSender() {
            return mSender;
        }

        public long getSize() {
            return mSize;
        }

        public String getTagIds() {
            return mTags;
        }
    }

}
