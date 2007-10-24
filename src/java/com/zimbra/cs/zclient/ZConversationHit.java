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
import com.zimbra.cs.zclient.event.ZModifyConversationEvent;
import com.zimbra.cs.zclient.event.ZModifyEvent;

import java.util.ArrayList;
import java.util.List;


public class ZConversationHit implements ZSearchHit {

    private String mId;
    private String mFlags;
    private String mFragment;
    private String mSubject;
    private String mSortField;
    private String mTags;
    private float mScore;
    private int mMessageCount;
    private long mDate;
    private List<String> mMessageIds;
    private List<ZEmailAddress> mRecipients;
        
    public ZConversationHit(Element e) throws ServiceException {
        mId = e.getAttribute(MailConstants.A_ID);
        mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
        mDate = e.getAttributeLong(MailConstants.A_DATE);
        mTags = e.getAttribute(MailConstants.A_TAGS, null);
        Element fr = e.getOptionalElement(MailConstants.E_FRAG);
        if (fr != null) mFragment = fr.getText();
        Element su = e.getOptionalElement(MailConstants.E_SUBJECT);
        if (su != null) mSubject = su.getText();
        mSortField = e.getAttribute(MailConstants.A_SORT_FIELD, null);
        mMessageCount = (int) e.getAttributeLong(MailConstants.A_NUM);
        mScore = (float) e.getAttributeDouble(MailConstants.A_SCORE, 0);
        mMessageIds = new ArrayList<String>();
        for (Element m: e.listElements(MailConstants.E_MSG)) {
            mMessageIds.add(m.getAttribute(MailConstants.A_ID));
        }
        
        mRecipients = new ArrayList<ZEmailAddress>();
        for (Element emailEl: e.listElements(MailConstants.E_EMAIL)) {
            mRecipients.add(new ZEmailAddress(emailEl));
        }        
    }

    public void modifyNotification(ZModifyEvent event) throws ServiceException {
    	if (event instanceof ZModifyConversationEvent) {
    		ZModifyConversationEvent cevent = (ZModifyConversationEvent) event;
    		mFlags = cevent.getFlags(mFlags);
    		mTags = cevent.getTagIds(mTags);
    		mSubject = cevent.getSubject(mSubject);
    		mFragment = cevent.getFragment(mFragment);
    		mMessageCount = cevent.getMessageCount(mMessageCount);
    		mRecipients = cevent.getRecipients(mRecipients);
    	}
    }

    public String getId() {
        return mId;
    }
    
    public float getScore() {
        return mScore;
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("id", mId);
        sb.add("flags", mFlags);
        sb.add("tags", mTags);
        sb.add("fragment", mFragment);
        sb.add("subject", mSubject);
        sb.addDate("date", mDate);
        sb.add("sortField", mSortField);
        sb.add("messageCount", mMessageCount);
        sb.add("messageIds", mMessageIds, true, true);
        sb.add("recipients", mRecipients, false, true);
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
    
    public int getMessageCount() {
        return mMessageCount;
    }

    public List<String> getMatchedMessageIds() {
        return mMessageIds;
    }

    public List<ZEmailAddress> getRecipients() {
        return mRecipients;
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

    public boolean isHighPriority() {
        return hasFlags() && mFlags.indexOf(ZConversation.Flag.highPriority.getFlagChar()) != -1;
    }

    public boolean isLowPriority() {
        return hasFlags() && mFlags.indexOf(ZConversation.Flag.lowPriority.getFlagChar()) != -1;
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
