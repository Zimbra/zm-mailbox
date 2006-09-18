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
package com.zimbra.cs.jsp.bean;

import java.util.Date;
import java.util.List;

import com.zimbra.cs.zclient.ZConversationHit;
import com.zimbra.cs.zclient.ZEmailAddress;

public class ZConversationHitBean extends ZSearchHitBean {

    private ZConversationHit mHit;
    
    public ZConversationHitBean(ZConversationHit hit) {
        super(hit, HitType.conversation);
        mHit = hit;
    }

    /**
     * @return conversation's id
     */
    public String getId() { return mHit.getId(); }
    
    /** change to a method that returns tag names? public String getTagIds(); */
    
    public Date getDate() { return new Date(mHit.getDate()); }
    
    public boolean getHasFlags() { return mHit.hasFlags(); }
    
    public boolean getHasTags() { return mHit.hasTags(); }
    
    public boolean getIsUnread() { return mHit.isUnread(); }

    public boolean getIsFlagged() { return mHit.isFlagged(); }

    public boolean getIsSentByMe() { return mHit.isSentByMe(); }

    public boolean getHasAttachment() { return mHit.hasAttachment(); }

    public String getSubject() { return mHit.getSubject(); }
    
    public String getFragment() { return mHit.getFragment(); }
    
    public int getMessageCount() { return mHit.getMessageCount(); }
    
    public List<String> getMatchedMessageIds() { return mHit.getMatchedMessageIds(); }
    
    public List<ZEmailAddress> getRawRecipients() { return mHit.getRecipients(); }
    
    public String getRecipients() { return BeanUtils.getAddrs(mHit.getRecipients()); }    
}
