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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient.soap;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZSearchFolder;
import com.zimbra.soap.Element;

class ZSoapSearchFolder implements ZSearchFolder {

    private String mId;
    private String mName;
    private String mParentId;
    private String mQuery;
    private String mTypes;
    private String mSortBy;
    private ZFolder mParent;
    
    ZSoapSearchFolder(Element e, ZFolder parent) throws ServiceException {
        mParent = parent;
        mId = e.getAttribute(MailService.A_ID);
        mName = e.getAttribute(MailService.A_NAME);
        mParentId = e.getAttribute(MailService.A_FOLDER);
        mQuery = e.getAttribute(MailService.A_QUERY);
        mTypes = e.getAttribute(MailService.A_SEARCH_TYPES, null); // TODO FIX
        mSortBy = e.getAttribute(MailService.A_SORTBY, null); // TODO FIX
    }

    public ZFolder getParent() {
        return mParent;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String toString() {
        return String.format("search: { id: %s, name: %s, parentId: %s, query: %s, types: %s, sortBy: %s, path: %s }", 
                mId, mName, mParentId, mQuery, mTypes, mSortBy, getPath());
    }

    public String getParentId() {
        return mParentId;
    }

    public String getPath() {
        // TODO: CACHE? compute upfront?
        if (mParent == null)
            return ZMailbox.PATH_SEPARATOR;
        else {
            String pp = mParent.getPath();
            return pp.length() == 1 ? (pp + mName) : (pp + ZMailbox.PATH_SEPARATOR + mName);
        }
    }

    public String getQuery() {
        return mQuery;
    }

    public String getSortBy() {
        return mSortBy;
    }

    public String getTypes() {
        return mTypes;
    }
}
