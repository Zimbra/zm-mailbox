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

import com.zimbra.cs.zclient.ZEmailAddress;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZMessage.ZMimePart;

public class ZMessageBean {

	private ZMessage mMsg;
	
	public ZMessageBean(ZMessage msg) {
		mMsg = msg;
	}
	   
    public String getId() { return mMsg.getId(); }
    
    /**
     * @return comma-separated list of tag ids
     */
    public String getTagIds() { return mMsg.getTagIds(); }
    
    public String getFlags() { return mMsg.getFlags(); }
    
    public boolean getHasFlags() { return mMsg.hasFlags(); }
    
    public boolean getHasTags() { return mMsg.hasTags(); }
    
    public boolean getIsUnread() { return mMsg.isUnread(); }

    public boolean getIsFlagged() { return mMsg.isFlagged(); }

    public boolean getHasAttachment() { return mMsg.hasAttachment(); }

    public boolean getIsRepliedTo() { return mMsg.isRepliedTo(); }

    public boolean getIsSentByMe() { return mMsg.isSentByMe(); }

    public boolean getIsForwarded() { return mMsg.isForwarded(); }

    public boolean getIsDraft() { return mMsg.isDraft(); }

    public boolean getIsDeleted() { return mMsg.isDeleted(); }

    public boolean getIsNotificationSent() { return mMsg.isNotificationSent(); }
    
    public String getSubject() { return mMsg.getSubject(); }
    
    public String getFolderId() { return mMsg.getFolderId(); }
    
    public String getConversationId() { return mMsg.getConversationId(); }

    public Date getReceivedDate() { return new Date(mMsg.getReceivedDate()); }
    
    public Date getSentDate() { return new Date(mMsg.getSentDate()); }

    public String getMessageIdHeader() { return mMsg.getMessageIdHeader(); }
    
    public List<ZEmailAddress> getEmailAddresses() { return mMsg.getEmailAddresses(); }
    
    public ZMimePart getMimeStructure() { return mMsg.getMimeStructure(); }

    public long getSize() { return mMsg.getSize(); }
    
    /** content of the message, if raw is specified. if message too big or not ASCII, a content servlet URL is returned */
    public String getContent() { return mMsg.getContent(); }
    
    /** if raw is specified and message too big or not ASCII, a content servlet URL is returned */
    public String getContentURL() { return mMsg.getContentURL(); }

}
