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
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 23, 2004
 *
 */
package com.zimbra.cs.account.ldap;

import javax.naming.directory.Attributes;


/**
 * And LdapEntry class that has a getId (zimbraId attr) and the concept of a name that is unique within the zimbra* objectClass.
 * 
 * @author schemers
 *
 */
public abstract class LdapNamedEntry extends LdapEntry implements Comparable {

    LdapNamedEntry(String dn, Attributes attrs) {
        super(dn, attrs);
    }
    
    public abstract String getId();
    
    public abstract String getName();

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object obj) {
        if (!(obj instanceof LdapNamedEntry))
            return 0;
        LdapNamedEntry other = (LdapNamedEntry) obj;
        return getName().compareTo(other.getName());
    }
}
