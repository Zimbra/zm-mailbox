package com.zimbra.cs.ldap;

import java.util.List;

public abstract class ZLdapSchema extends ZLdapElement {
    
    /**
     * Retrieves the object class with the specified name or OID from the server schema.
     * 
     * @param objectClass
     * @return The requested object class, or null if there is no such class defined in the server schema.
     * @throws LdapException
     */
    public abstract ZObjectClassDefinition getObjectClassDefinition(String objectClass)
    throws LdapException;

    public abstract static class ZObjectClassDefinition extends ZLdapElement {
        public abstract List<String> getSuperiorClasses() throws LdapException;
    }
}
