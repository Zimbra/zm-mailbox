/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2005. 6. 21.
 */
package com.zimbra.cs.mailbox;

import java.util.List;

import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.MailboxBlob;

/**
 * @author jhahm
 * 
 * Class that facilitates blob file sharing when delivering a message to
 * multiple recipients or when a message is copied upon delivery to one
 * or more folders within the same mailbox due to filter rules.
 * 
 * This class is used to carry information across multiple calls to
 * Mailbox.addMessage() for a single message being delivered.
 */
public class DeliveryContext {

    private boolean mShared;
    private Blob mIncomingBlob;
    private MailboxBlob mMailboxBlob;
    private List<Long> mMailboxIdList;
    private boolean mIsFirst = true;

    /**
     * Constructor for non-shared case
     */
    public DeliveryContext() {
    	mShared = false;
        mMailboxBlob = null;
        mMailboxIdList = null;
    }

    /**
     * Constructor for shared/non-shared cases
     * @param shared
     * @param mboxIdList list of ID of mailboxes being delivered to
     */
    public DeliveryContext(boolean shared, List<Long> mboxIdList) {
    	mShared = shared;
        mMailboxBlob = null;
        mMailboxIdList = mboxIdList;
    }

    public boolean getShared() {
    	return mShared;
    }

    public List<Long> getMailboxIdList() {
    	return mMailboxIdList;
    }

    public Blob getIncomingBlob() {
        return mIncomingBlob;
    }
    
    public void setIncomingBlob(Blob blob) {
        mIncomingBlob = blob;
    }
    
    public MailboxBlob getMailboxBlob() {
    	return mMailboxBlob;
    }

    public void setMailboxBlob(MailboxBlob mailboxBlob) {
    	mMailboxBlob = mailboxBlob;
    }

    /**
     * Tells the caller if this is the first mailbox being delivered to.
     */
    public boolean isFirst() {
        return mIsFirst;
    }
    
    public void setFirst(boolean isFirst) {
        mIsFirst = isFirst;
    }
}
