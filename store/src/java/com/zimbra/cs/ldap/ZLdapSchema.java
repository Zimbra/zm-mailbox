/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.ldap;

import java.util.List;

public abstract class ZLdapSchema extends ZLdapElement {
    
    public abstract List<ZObjectClassDefinition> getObjectClasses()
    throws LdapException;
    
    /**
     * Retrieves the object class with the specified name or OID from the server schema.
     * 
     * @param objectClass
     * @return The requested object class, or null if there is no such class defined in the server schema.
     * @throws LdapException
     */
    public abstract ZObjectClassDefinition getObjectClass(String objectClass)
    throws LdapException;

    public abstract static class ZObjectClassDefinition extends ZLdapElement {
        public abstract String getName();
        
        public abstract List<String> getSuperiorClasses() throws LdapException;
        
        public abstract List<String> getOptionalAttributes() throws LdapException;
        
        public abstract List<String> getRequiredAttributes() throws LdapException;
    }
}
