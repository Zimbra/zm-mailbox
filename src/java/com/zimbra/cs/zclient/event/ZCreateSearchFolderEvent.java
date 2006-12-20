/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient.event;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.zclient.ZItem;
import com.zimbra.cs.zclient.ZSearchFolder;

public class ZCreateSearchFolderEvent implements ZCreateItemEvent {

    protected ZSearchFolder mSearchFolder;

    public ZCreateSearchFolderEvent(ZSearchFolder searchFolder) throws ServiceException {
        mSearchFolder = searchFolder;
    }

    /**
     * @return id of created search folder.
     * @throws com.zimbra.common.service.ServiceException
     */
    public String getId() throws ServiceException {
        return mSearchFolder.getId();
    }

    public ZItem getItem() throws ServiceException {
        return mSearchFolder;
    }

    public ZSearchFolder getSearchFolder() {
        return mSearchFolder;
    }
    
    public String toString() {
    	return mSearchFolder.toString();
    }
}
