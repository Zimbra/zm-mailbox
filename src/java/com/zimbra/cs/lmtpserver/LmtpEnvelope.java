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
	
	private List mRecipients; 
    private LmtpAddress mSender;
    private int mSize;
    private LmtpBodyType mBodyType;
    
    public LmtpEnvelope() {
    	mRecipients = new LinkedList();
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

    public List getRecipients() {
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