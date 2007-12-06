/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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

/*
 * Created on Nov 26, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.pop3;

import com.zimbra.cs.mailbox.Message;


/**
 * one for each message in the mailbox
 * @author schemers
 *
 */
public class Pop3Message {
    boolean mDeleted;
    private int mId;
    private long mSize; // raw size from blob store
    String mDigest;
    
    /**
     * save enough info from the Message so we don't have to keep a reference to it.
     * @param msg
     */
    public Pop3Message(Message msg) {
        this(msg.getId(), msg.getSize(), msg.getDigest());
    }

    public Pop3Message(int id, long size, String digest) {
        mId = id;
        mSize = size;
        mDeleted = false;
        mDigest = digest;
    }
    
    long getSize() {
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