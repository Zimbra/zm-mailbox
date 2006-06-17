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

package com.zimbra.cs.account.soap;

import java.util.Map;

import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.soap.Element;

public abstract class SoapNamedEntry extends SoapEntry implements NamedEntry {

    protected String mName;
    protected String mId;

    protected SoapNamedEntry(Element e) throws ServiceException {
        super(SoapProvisioning.getAttrs(e));
        mName = e.getAttribute(AdminService.A_NAME);
        mId = e.getAttribute(AdminService.A_ID);
    }
    
    protected SoapNamedEntry(String name, String id, Map<String, Object> attrs) {
        super(attrs);
        mName = name;
        mId = id;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public int compareTo(Object obj) {
        if (!(obj instanceof NamedEntry))
            return 0;
        NamedEntry other = (NamedEntry) obj;
        return getName().compareTo(other.getName());
    }
    

    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(": { name=").append(getName()).append(" id=").append(getId());
        sb.append(mAttrs.toString());
        sb.append("}");
        return sb.toString();           
    }

}
