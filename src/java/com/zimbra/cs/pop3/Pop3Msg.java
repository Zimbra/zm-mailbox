/*
 * Created on Nov 26, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.pop3;

import com.liquidsys.coco.mailbox.Message;


/**
 * one for each message in the mailbox
 * @author schemers
 *
 */
class Pop3Msg {
    boolean mDeleted;
    private Pop3Mbx mMbx;
    private int mId;
    private int mSize; // raw size from blob store
    String mDigest;
    
    /**
     * save enough info from the Message so we don't have to keep a reference to it.
     * @param m
     */
    Pop3Msg(Message m) {
        mId = m.getId();
        mSize = (int) m.getSize();
        mDeleted = false;
        mDigest = m.getDigest();
    }
    
    int getSize() {
        return mSize;
    }
    
    int getId() {
        return mId;
    }
    
    boolean isDeleted() {
        return mDeleted;
    }
    
    String getDigest() {
        return mDigest;
    }
}