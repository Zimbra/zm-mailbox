/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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
