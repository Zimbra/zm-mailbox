/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
