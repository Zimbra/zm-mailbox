/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
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
 * Created on 2005. 6. 21.
 */
package com.zimbra.cs.mailbox;

import java.util.List;

import com.zimbra.cs.store.Blob;

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
public class SharedDeliveryContext {

    private boolean mShared;
    private Blob mBlob;
    private MailboxBlob mMailboxBlob;
    private List<Integer> mMailboxIdList;
    private Blob mPreexistingBlob;
    private boolean mIsFirst = true;

    /**
     * Constructor for non-shared case
     */
    public SharedDeliveryContext() {
    	mShared = false;
        mBlob = null;
        mMailboxBlob = null;
        mMailboxIdList = null;
    }

    /**
     * Constructor for shared/non-shared cases
     * @param shared
     * @param mboxIdList list of ID of mailboxes being delivered to
     */
    public SharedDeliveryContext(boolean shared, List<Integer> mboxIdList) {
    	mShared = shared;
        mBlob = null;
        mMailboxBlob = null;
        mMailboxIdList = mboxIdList;
    }

    public boolean getShared() {
    	return mShared;
    }

    public List<Integer> getMailboxIdList() {
    	return mMailboxIdList;
    }

    public Blob getBlob() {
    	return mBlob;
    }

    public void setBlob(Blob blob) {
    	mBlob = blob;
    }
    
    public MailboxBlob getMailboxBlob() {
    	return mMailboxBlob;
    }

    public void setMailboxBlob(MailboxBlob mailboxBlob) {
    	mMailboxBlob = mailboxBlob;
    }

    /**
     * Sets the blob stored on disk before mailbox delivery
     * is started.
     * @param blob the blob or <tt>null</tt> if the blob needs to be stored
     * in the incoming directory during delivery.
     */
    public void setPreexistingBlob(Blob blob) {
        mPreexistingBlob = blob;
    }
    
    public Blob getPreexistingBlob() {
        return mPreexistingBlob;
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
