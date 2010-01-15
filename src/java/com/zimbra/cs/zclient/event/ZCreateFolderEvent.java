/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient.event;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZItem;

public class ZCreateFolderEvent implements ZCreateItemEvent {

    protected ZFolder mFolder;

    public ZCreateFolderEvent(ZFolder folder) throws ServiceException {
        mFolder = folder;
    }

    /**
     * @return id of created folder
     * @throws com.zimbra.common.service.ServiceException
     */
    public String getId() throws ServiceException {
        return mFolder.getId();
    }

    public ZItem getItem() throws ServiceException {
        return mFolder;
    }

    public ZFolder getFolder() {
        return mFolder;
    }
    
    public String toString() {
    	return mFolder.toString();
    }
}
