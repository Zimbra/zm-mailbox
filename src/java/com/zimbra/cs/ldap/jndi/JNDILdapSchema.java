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
package com.zimbra.cs.ldap.jndi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZLdapSchema;

public class JNDILdapSchema extends ZLdapSchema {

    DirContext schema;
    
    JNDILdapSchema(DirContext schema) {
        this.schema = schema;
    }
    
    @Override
    public void debug() {
        // TODO Auto-generated method stub
        
    }
    
    public static class JNDIObjectClassDefinition extends ZObjectClassDefinition {
        private DirContext ocDef;
        
        @Override
        public void debug() {
            // TODO Auto-generated method stub
            
        }
        
        private JNDIObjectClassDefinition(DirContext ocSchema) {
            this.ocDef = ocSchema;
        }
        
        private List<String> getProperty(String property) throws LdapException {
            List<String> result = new ArrayList<String>();
            
            try {
                Attributes attributes = ocDef.getAttributes("");
                Map<String, Object> attrs = LegacyLdapUtil.getAttrs(attributes);
                
                for (Map.Entry<String, Object> attr : attrs.entrySet()) {
                    String attrName = attr.getKey();
                    if (property.compareToIgnoreCase(attrName) == 0) {
                         Object value = attr.getValue();
                        if (value instanceof String)
                            result.add(((String)value).toLowerCase());
                        else if (value instanceof String[]) {
                            for (String v : (String[])value)
                                result.add(v.toLowerCase());
                        }
                    }
                }
            } catch (NamingException e) {
                throw JNDILdapException.mapToLdapException(e);
            }
            
            return result;
        }


        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public List<String> getSuperiorClasses() throws LdapException {
            return getProperty("SUP");
        }

        @Override
        public List<String> getOptionalAttributes() throws LdapException {
            return getProperty("MAY");
        }

        @Override
        public List<String> getRequiredAttributes() throws LdapException {
            return getProperty("MUST");
        }

    }

    @Override
    public ZObjectClassDefinition getObjectClass(String objectClass) 
    throws LdapException {
        try {
            return new JNDIObjectClassDefinition(
                    (DirContext)schema.lookup("ClassDefinition/" + objectClass));
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }

    @Override
    public List<ZObjectClassDefinition> getObjectClasses() throws LdapException {
        throw new UnsupportedOperationException();
    }

}
