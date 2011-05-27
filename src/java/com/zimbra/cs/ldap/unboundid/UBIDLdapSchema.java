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
package com.zimbra.cs.ldap.unboundid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.unboundid.ldap.sdk.schema.ObjectClassDefinition;
import com.unboundid.ldap.sdk.schema.Schema;

import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZLdapSchema;

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
        
        ObjectClassDefinition getNative() {
            return ocDef;
        }


        @Override
        public String getName() {
            return ocDef.getNameOrOID();
        }
        
        @Override
        public List<String> getSuperiorClasses() throws LdapException {
            return Arrays.asList(ocDef.getSuperiorClasses());
        }

        @Override
        public List<String> getOptionalAttributes() throws LdapException {
            return Arrays.asList(ocDef.getOptionalAttributes());
        }

        @Override
        public List<String> getRequiredAttributes() throws LdapException {
            return Arrays.asList(ocDef.getRequiredAttributes());
        }

        
    }

    @Override
    public ZObjectClassDefinition getObjectClass(String objectClass) 
    throws LdapException {
        ObjectClassDefinition oc = schema.getObjectClass(objectClass);
        if (oc == null) {
            return null;
        } else {
            return new UBIDObjectClassDefinition(oc);
        }
        
    }

    @Override
    public List<ZObjectClassDefinition> getObjectClasses() throws LdapException {
        List<ZObjectClassDefinition> ocList = new ArrayList<ZObjectClassDefinition>();
        
        Set<ObjectClassDefinition> ocs = schema.getObjectClasses();
        for (ObjectClassDefinition oc : ocs) {
            UBIDObjectClassDefinition ubidOC = new UBIDObjectClassDefinition(oc);
            ocList.add(ubidOC);
        }
        
        Comparator comparator = new Comparator<UBIDObjectClassDefinition>() {
            public int compare(UBIDObjectClassDefinition first,
                    UBIDObjectClassDefinition second) {
                return first.getName().compareTo(second.getName());
            }
        };
        
        Collections.sort(ocList, comparator);
        return ocList;
    }

}
