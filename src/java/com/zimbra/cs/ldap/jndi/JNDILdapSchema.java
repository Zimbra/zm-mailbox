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
import com.zimbra.cs.ldap.ZLdapSchema.ZObjectClassDefinition;

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

        @Override
        public List<String> getSuperiorClasses() throws LdapException {
            List<String> result = new ArrayList<String>();
            
            try {
                Attributes attributes = ocDef.getAttributes("");
                Map<String, Object> attrs = LegacyLdapUtil.getAttrs(attributes);
                
                for (Map.Entry<String, Object> attr : attrs.entrySet()) {
                    String attrName = attr.getKey();
                    if ("SUP".compareToIgnoreCase(attrName) == 0) {
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
    }

    @Override
    public ZObjectClassDefinition getObjectClassDefinition(String objectClass) 
    throws LdapException {
        try {
            return new JNDIObjectClassDefinition(
                    (DirContext)schema.lookup("ClassDefinition/" + objectClass));
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }

}
