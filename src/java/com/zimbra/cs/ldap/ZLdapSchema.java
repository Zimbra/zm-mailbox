/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
