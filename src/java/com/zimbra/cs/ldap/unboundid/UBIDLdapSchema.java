package com.zimbra.cs.ldap.unboundid;

import java.util.Arrays;
import java.util.List;

import com.unboundid.ldap.sdk.schema.ObjectClassDefinition;
import com.unboundid.ldap.sdk.schema.Schema;

import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZLdapSchema;
import com.zimbra.cs.ldap.ZLdapSchema.ZObjectClassDefinition;

public class UBIDLdapSchema extends ZLdapSchema {

    private Schema schema;
    
    UBIDLdapSchema(Schema schema) {
        this.schema = schema;
    }
    
    @Override
    public void debug() {
        // TODO Auto-generated method stub
        
    }
    
    public static class UBIDObjectClassDefinition extends ZObjectClassDefinition {

        private ObjectClassDefinition ocDef;
        
        private UBIDObjectClassDefinition(ObjectClassDefinition ocDef) {
            this.ocDef = ocDef;
        }
        
        @Override
        public void debug() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public List<String> getSuperiorClasses() throws LdapException {
            return Arrays.asList(ocDef.getSuperiorClasses());
        }
        
    }

    @Override
    public ZObjectClassDefinition getObjectClassDefinition(String objectClass) 
    throws LdapException {
        ObjectClassDefinition ocDef = schema.getObjectClass(objectClass);
        if (ocDef == null) {
            return null;
        } else {
            return new UBIDObjectClassDefinition(ocDef);
        }
        
    }

}
