/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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

import java.util.LinkedList;
import java.util.List;


public class LmtpEnvelope {
	
	private List<LmtpAddress> mRecipients; 
    private LmtpAddress mSender;
    private int mSize;
    private LmtpBodyType mBodyType;
    
    public LmtpEnvelope() {
    	mRecipients = new LinkedList<LmtpAddress>();
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
    
    public void addRecipient(LmtpAddress recipient) {
    	mRecipients.add(recipient);
    }

    public List<LmtpAddress> getRecipients() {
    	return mRecipients;
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