/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 7, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.lmtpserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;


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

    public List<LmtpAddress> getLocalRecipients() {
        List<LmtpAddress> list = new ArrayList<>();
        for (LmtpAddress recipient: mRecipients) {
            if (recipient.isOnLocalServer()) {
                list.add(recipient);
            }
        }
        return list;
    }

    public List<LmtpAddress> getLocalRecipients(Collection<LmtpAddress> recipients) {
        List<LmtpAddress> list = new ArrayList<>();
        for (LmtpAddress recipient: recipients) {
            if (recipient.isOnLocalServer()) {
                list.add(recipient);
            }
        }
        return list;
    }

    public List<LmtpAddress> getRemoteRecipients() {
        List<LmtpAddress> list = new ArrayList<>();
        for (LmtpAddress recipient: mRecipients) {
            if (!recipient.isOnLocalServer()) {
                list.add(recipient);
            }
        }
        return list;
    }

    public List<LmtpAddress> getRemoteRecipients(Collection<LmtpAddress> recipients) {
        List<LmtpAddress> list = new ArrayList<>();
        for (LmtpAddress recipient: recipients) {
            if (!recipient.isOnLocalServer()) {
                list.add(recipient);
            }
        }
        return list;
    }

    public List<LmtpAddress> getRemoteRecipients(LmtpReply filter) {
        List<LmtpAddress> matches = new ArrayList<>();
        for (LmtpAddress lmtpAddress: getRemoteRecipients()) {
            if (lmtpAddress.getDeliveryStatus() == filter) {
                matches.add(lmtpAddress);
            }
        }
        return matches;
    }

    public Multimap<String, LmtpAddress> getRemoteServerToRecipientsMap() {
        Multimap<String, LmtpAddress> multimap = ArrayListMultimap.create();
        for (LmtpAddress recipient: mRecipients) {
            if (!recipient.isOnLocalServer()) {
                multimap.put(recipient.getRemoteServer(), recipient);
            }
        }
        return multimap;
    }

    public Multimap<String, LmtpAddress> getRemoteServerToRecipientsMap(Collection<LmtpAddress> recipients) {
        Multimap<String, LmtpAddress> multimap = ArrayListMultimap.create();
        for (LmtpAddress recipient: recipients) {
            multimap.put(recipient.getRemoteServer(), recipient);
        }
        return multimap;
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