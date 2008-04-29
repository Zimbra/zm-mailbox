/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

package com.zimbra.cs.zclient.event;

import com.zimbra.common.service.ServiceException;

public class ZModifyVoiceMailItemEvent implements ZModifyItemEvent {
	private String mId;
	private boolean mIsHeard;
	private boolean mMadeChange;

	public ZModifyVoiceMailItemEvent(String id, boolean isHeard) throws ServiceException {
		mId = id;
		mIsHeard = isHeard;
		mMadeChange = false;
	}

	/**
	 * @return id
	 */
	public String getId() throws ServiceException {
		return mId;
	}

	/**
	 * @return true if item has been heard
	 */
	public boolean getIsHeard() {
		return mIsHeard;
	}

	/**
	 * Makes note that something actually changed. Used when marking (un)heard
	 * so that we can try to keep track of the folder's unheard count,
	 * which is never updated by the server.
	 */
	public void setMadeChange() {
		mMadeChange = true;
	}

	/**
	 * Returns true if something actually changed.
	 */
	public boolean getMadeChange() {
		return mMadeChange;
	}
}
