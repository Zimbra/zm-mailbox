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

package com.zimbra.doc.soap;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.zimbra.soap.type.ZmBoolean;

public class ValueDescription {
    private static Joiner PIPE_JOINER = Joiner.on("|");
    private ValueDescription() {
    }

    public static String getRepresentation(Class<?> klass) {
        String value;
        if (klass.isAssignableFrom(ZmBoolean.class)) {
            value = "0|1";
        } else if (klass.isEnum()) {
            // TODO: get Jaxb XmlEnumValues
            StringBuilder sb = new StringBuilder();
            sb.append(PIPE_JOINER.join(klass.getEnumConstants()));
            value = sb.toString();
        } else {
            String className = klass.getName();
            if (className.contains(".")) {
                className = className.substring(className.lastIndexOf('.') + 1);
            }
            value = className;
        }
        return value;
    }
    
    public static String getRepresentation(String valueName, Class<?> klass) {
        String value;
        if (klass.isAssignableFrom(ZmBoolean.class)) {
            value = "0|1";
        } else if (klass.isEnum()) {
            // TODO: get Jaxb XmlEnumValues
            StringBuilder sb = new StringBuilder();
            sb.append(PIPE_JOINER.join(klass.getEnumConstants()));
            value = sb.toString();
        } else {
            String className = klass.getName();
            if (className.contains(".")) {
                className = className.substring(className.lastIndexOf('.') + 1);
            }
            if (valueName == null) {
                value = className;
            } else {
                if ("String".equals(className)) {
                    if (Strings.isNullOrEmpty(valueName)) {
                        value = "\"...\"";
                    } else {
                        value = String.format("{%s}", valueName);
                    } 
                } else {
                    if (Strings.isNullOrEmpty(valueName)) {
                            value = String.format("(%s)", className);
                    } else {
                        value = String.format("{%s} (%s)", valueName, className);
                    }
                }
            }
        }
        return value;
    }
}
