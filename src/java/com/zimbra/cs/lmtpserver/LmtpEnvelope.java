/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 7, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.lmtpserver;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.zimbra.common.util.ZimbraLog;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


public class LmtpEnvelope {
	
	private List<LmtpAddress> mRecipients; 
	private List<LmtpAddress> mLocalRecipients;
	private List<LmtpAddress> mRemoteRecipients;
    private Multimap<String, LmtpAddress> mRemoteServerToRecipientsMap;
    private LmtpAddress mSender;
    private int mSize;
    private LmtpBodyType mBodyType;
    
    public LmtpEnvelope() {
    	mRecipients = new LinkedList<LmtpAddress>();
    	mLocalRecipients = new LinkedList<LmtpAddress>();
    	mRemoteRecipients = new LinkedList<LmtpAddress>();
    	mRemoteServerToRecipientsMap = ArrayListMultimap.create();
    }
    
    public boolean hasSender() {
    	return mSender != null;
    }
    
    public boolean hasRecipients() {
    	return mRecipients.size() > 0;
    }
    
    public void setSender(LmtpAddress sender) {
    	mSender = sender;
    }
    
    public void addLocalRecipient(LmtpAddress recipient) {
    	mRecipients.add(recipient);
    	mLocalRecipients.add(recipient);
    }

    public void addRemoteRecipient(LmtpAddress recipient) {
        if (recipient.getRemoteServer() == null) {
            ZimbraLog.lmtp.error("Server for remote recipient %s has not been set", recipient);
            return;
        }
    	mRecipients.add(recipient);
    	mRemoteRecipients.add(recipient);
        mRemoteServerToRecipientsMap.put(recipient.getRemoteServer(), recipient);
    }

    public List<LmtpAddress> getRecipients() {
    	return mRecipients;
    }
    
    public List<LmtpAddress> getLocalRecipients() {
    	return mLocalRecipients;
    }

    public List<LmtpAddress> getRemoteRecipients() {
    	return mRemoteRecipients;
    }

    public Multimap<String, LmtpAddress> getRemoteServerToRecipientsMap() {
    	return mRemoteServerToRecipientsMap;
    }

    public LmtpAddress getSender() {
    	return mSender;
    }
	
    public LmtpBodyType getBodyType() {
		return mBodyType;
	}
	
    public void setBodyType(LmtpBodyType bodyType) {
		mBodyType = bodyType;
	}
	
    public int getSize() {
		return mSize;
	}
	
    public void setSize(int size) {
		mSize = size;
	}
}