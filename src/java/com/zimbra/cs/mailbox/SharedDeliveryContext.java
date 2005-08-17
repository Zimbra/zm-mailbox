/*
 * Created on 2005. 6. 21.
 */
package com.zimbra.cs.mailbox;

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

    public SharedDeliveryContext(boolean shared) {
    	mShared = shared;
        mBlob = null;
        mMailboxBlob = null;
    }

    public boolean getShared() {
    	return mShared;
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
}
