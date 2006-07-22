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
import com.zimbra.cs.zclient.ZGrant;
import com.zimbra.soap.Element;

class ZSoapGrant implements ZGrant {

    private String mArgs;
    private String mGranteeName;
    private String mGranteeId;
    private GranteeType mGranteeType;
    private boolean mInherit;
    private String mPermissions;

    ZSoapGrant(Element e) throws ServiceException {
        mArgs = e.getAttribute(MailService.A_ARGS, null);
        mPermissions = e.getAttribute(MailService.A_RIGHTS);
        mGranteeName = e.getAttribute(MailService.A_DISPLAY, null);
        mGranteeId = e.getAttribute(MailService.A_ZIMBRA_ID, null);        
        mGranteeType = GranteeType.fromString(e.getAttribute(MailService.A_GRANT_TYPE));
        mInherit = e.getAttributeBool(MailService.A_INHERIT);
    }
    
    public String getArgs() {
        return mArgs;
    }

    public String getGranteeName() {
        return mGranteeName;
    }

    public String getGranteeId() {
        return mGranteeId;
    }

    public GranteeType getGranteeType() {
        return mGranteeType;
    }

    public boolean getInherit() {
        return mInherit;
    }

    public String getPermissions() {
        return mPermissions;
    }
    
    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("type", mGranteeType.name());
        sb.add("name", mGranteeName);
        sb.add("permissions", mPermissions);
        sb.add("inherit", mInherit);
        sb.add("args", mArgs);
        sb.endStruct();
        return sb.toString();
    }

    private boolean hasPerm(Permission p) {
        return (mPermissions != null) && mPermissions.indexOf(p.getPermissionChar()) != -1;
    }
    public boolean canAdminister() {
        return hasPerm(Permission.administer);
    }

    public boolean canDelete() {
        return hasPerm(Permission.delete);
    }

    public boolean canInsert() {
        return hasPerm(Permission.insert);
    }

    public boolean canRead() {
        return hasPerm(Permission.read);        
    }

    public boolean canWorkflow() {
        return hasPerm(Permission.workflow);
    }

    public boolean canWrite() {
        return hasPerm(Permission.write);
    }
}
