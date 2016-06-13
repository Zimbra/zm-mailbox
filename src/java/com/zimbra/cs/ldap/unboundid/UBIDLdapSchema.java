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
    }

    public static class UBIDObjectClassDefinition extends ZObjectClassDefinition {

        private ObjectClassDefinition ocDef;

        private UBIDObjectClassDefinition(ObjectClassDefinition ocDef) {
            this.ocDef = ocDef;
        }

        @Override
        public void debug() {
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
