/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.soap.mail.type;

import java.util.Arrays;
import java.util.Map;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.google.common.collect.Maps;
import com.zimbra.common.service.ServiceException;

@XmlEnum
public enum ModifyGroupMemberOperation {
    @XmlEnumValue("+") ADD("+"),
    @XmlEnumValue("-") REMOVE("-"),
    @XmlEnumValue("reset") RESET("reset") ;
    
        private static Map<String, ModifyGroupMemberOperation> nameToView = Maps.newHashMap();

        static {
            for (ModifyGroupMemberOperation v : ModifyGroupMemberOperation.values()) {
                nameToView.put(v.toString(), v);
            }
        }

        private String name;

        private ModifyGroupMemberOperation(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static ModifyGroupMemberOperation fromString(String name)
        throws ServiceException {
            ModifyGroupMemberOperation op = nameToView.get(name);
            if (op == null) {
               throw ServiceException.INVALID_REQUEST("unknown Operation: " + name + ", valid values: " +
                       Arrays.asList(ModifyGroupMemberOperation.values()), null);
            }
            return op;
        }

}
