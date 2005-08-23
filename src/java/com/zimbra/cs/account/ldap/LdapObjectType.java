/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on Apr 14, 2005
 *
 */
package com.zimbra.cs.account.ldap;

import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.object.ObjectType;

/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LdapObjectType extends LdapEntry implements ObjectType {

    /**
     * @param dn
     * @param attrs
     */
    LdapObjectType(String dn, Attributes attrs) {
        super(dn, attrs);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.object.ObjectType#getType()
     */
    public String getType() {
        return super.getAttr(Provisioning.A_zimbraObjectType);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.object.ObjectType#getDescription()
     */
    public String getDescription() {
        return super.getAttr(Provisioning.A_description, "");
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.object.ObjectType#isIndexingEnabled()
     */
    public boolean isIndexingEnabled() {
        return super.getBooleanAttr(Provisioning.A_zimbraObjectIndexingEnabled, true);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.object.ObjectType#isStoreMatched()
     */
    public boolean isStoreMatched() {
        return super.getBooleanAttr(Provisioning.A_zimbraObjectStoreMatched, false);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.object.ObjectType#getHandlerClass()
     */
    public String getHandlerClass() {
        return super.getAttr(Provisioning.A_zimbraObjectHandlerClass, null);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.object.ObjectType#getHandlerConfig()
     */
    public String getHandlerConfig() {
        return super.getAttr(Provisioning.A_zimbraObjectHandlerConfig, null);
    }

}
