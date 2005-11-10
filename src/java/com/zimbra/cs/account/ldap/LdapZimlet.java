/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap;

import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.object.ObjectType;

public class LdapZimlet extends LdapEntry implements Zimlet, ObjectType {

	public LdapZimlet(String dn, Attributes attrs) {
		super(dn, attrs);
	}
    public String getType() {
        return getAttr(Provisioning.A_cn);
    }
    
    public String getDescription() {
        return getAttr(Provisioning.A_zimbraZimletDescription);
    }
    
    public boolean isIndexingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraZimletIndexingEnabled, false);
    }
    
    public boolean isStoreMatched() {
        return false;
    }
    
    public String getHandlerClass() {
        return getAttr(Provisioning.A_zimbraZimletHandlerClass);
    }
    
    public String getHandlerConfig() {
        return getAttr(Provisioning.A_zimbraZimletHandlerConfig);
    }

    public String getServerIndexRegex() {
    	return getAttr(Provisioning.A_zimbraZimletServerIndexRegex);
    }
}
