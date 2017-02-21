/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client.event;

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
