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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
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