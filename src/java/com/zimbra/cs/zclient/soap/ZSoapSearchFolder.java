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

package com.zimbra.cs.zclient.soap;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.zclient.ZSearchFolder;
import com.zimbra.soap.Element;

class ZSoapSearchFolder extends ZSoapFolder implements ZSearchFolder, ZSoapItem {

    private String mQuery;
    private String mTypes;
    private String mSortBy;
    
    ZSoapSearchFolder(Element e, ZSoapFolder parent, ZSoapMailbox mailbox) throws ServiceException {
        super(e, parent, mailbox);
        mQuery = e.getAttribute(MailService.A_QUERY);
        mTypes = e.getAttribute(MailService.A_SEARCH_TYPES, null); // TODO FIX
        mSortBy = e.getAttribute(MailService.A_SORTBY, null); // TODO FIX
    }

    public String toString() {
        return toString(String.format(",query: %s, types: %s, sortBy: %s", 
                mQuery, mTypes, mSortBy));
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
