/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016, 2017 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2005. 6. 21.
 */
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.MailboxBlob;

/**
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
    private List<Integer> mMailboxIdList;
    private boolean mIsFirst = true;
    private Map <Integer,Blob> mailBoxBlobMap;

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
    public DeliveryContext(boolean shared, List<Integer> mboxIdList) {
        mShared = shared;
        mMailboxBlob = null;
        mMailboxIdList = mboxIdList;
        if (mShared) {
            mailBoxBlobMap =  new HashMap<Integer,Blob>();
        }
    }

    public boolean getShared() {
    	return mShared;
    }

    public List<Integer> getMailboxIdList() {
    	return mMailboxIdList;
    }

    public Blob getIncomingBlob() {
        return mIncomingBlob;
    }
    
    public DeliveryContext setIncomingBlob(Blob blob) {
        mIncomingBlob = blob;
        return this;
    }
    
    public DeliveryContext deepsetIncomingBlob(Blob blob) throws IOException {
        if (null != blob && null != mIncomingBlob) {
            mIncomingBlob.copy(blob);
        } else if (null == mIncomingBlob) {
            setIncomingBlob(blob);
        }
        return this;
    }

    public MailboxBlob getMailboxBlob() {
    	return mMailboxBlob;
    }

    public DeliveryContext setMailboxBlob(MailboxBlob mailboxBlob) {
    	mMailboxBlob = mailboxBlob;
        return this;
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

    public void setMailBoxSpecificBlob(int id, Blob blob) {
        if(mailBoxBlobMap != null) {
           mailBoxBlobMap.put(id, blob);
        }
    }

    public void clearMailBoxSpecificBlob(int id) {
        if(mailBoxBlobMap != null) {
            mailBoxBlobMap.remove(id);
        }
    }

   public Blob getMailBoxSpecificBlob(int mailBoxId) {
        if(mailBoxBlobMap != null) {
            return mailBoxBlobMap.get(mailBoxId);
        } else {
            return null;
        }
    }
}
