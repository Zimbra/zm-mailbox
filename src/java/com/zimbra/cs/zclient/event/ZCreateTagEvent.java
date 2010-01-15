/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
import com.zimbra.cs.zclient.ZItem;
import com.zimbra.cs.zclient.ZTag;

public class ZCreateTagEvent implements ZCreateItemEvent {

    protected ZTag mTag;

    public ZCreateTagEvent(ZTag tag) throws ServiceException {
        mTag = tag;
    }

    /**
     * @return tag id of created tag.
     * @throws com.zimbra.common.service.ServiceException
     */
    public String getId() throws ServiceException {
        return mTag.getId();
    }

    public ZItem getItem() throws ServiceException {
        return mTag;
    }

    public ZTag getTag() {
        return mTag;
    }
    
    public String toString() {
    	return mTag.toString();
    }
}
