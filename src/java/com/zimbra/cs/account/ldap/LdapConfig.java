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

/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account.ldap;

import java.util.Set;

import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LdapConfig extends LdapEntry implements Config {

    private Set mInheritableAccountAttrs;
    private Set mInheritableDomainAttrs;
    private Set mInheritableServerAttrs;
    
    LdapConfig(String dn, Attributes attrs) {
        super(dn, attrs);
        mInheritableAccountAttrs = getMultiAttrSet(Provisioning.A_zimbraCOSInheritedAttr);
        mInheritableDomainAttrs = getMultiAttrSet(Provisioning.A_zimbraDomainInheritedAttr);
        mInheritableServerAttrs = getMultiAttrSet(Provisioning.A_zimbraServerInheritedAttr);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Config#isInheritedAccountAttr(java.lang.String)
     */
    public boolean isInheritedAccountAttr(String name) {
        return mInheritableAccountAttrs.contains(name);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Config#isInheritedDomainAttr(java.lang.String)
     */
    public boolean isInheritedDomainAttr(String name) {
        return mInheritableDomainAttrs.contains(name);        
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Config#isInheritedServerAttr(java.lang.String)
     */
    public boolean isInheritedServerAttr(String name) {
        return mInheritableServerAttrs.contains(name);
    }
}
