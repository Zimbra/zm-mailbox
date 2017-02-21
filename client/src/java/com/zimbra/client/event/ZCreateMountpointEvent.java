/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
import com.zimbra.client.ZItem;
import com.zimbra.client.ZMountpoint;

public class ZCreateMountpointEvent implements ZCreateItemEvent {

    protected ZMountpoint mMountpoint;

    public ZCreateMountpointEvent(ZMountpoint mountpoint) throws ServiceException {
        mMountpoint = mountpoint;
    }

    /**
     * @return id of created mountpoint
     * @throws com.zimbra.common.service.ServiceException
     */
    public String getId() throws ServiceException {
        return mMountpoint.getId();
    }

    public ZItem getItem() throws ServiceException {
        return mMountpoint;
    }

    public ZMountpoint getMountpoint() {
        return mMountpoint;
    }
    
    public String toString() {
    	return mMountpoint.toString();
    }
}
