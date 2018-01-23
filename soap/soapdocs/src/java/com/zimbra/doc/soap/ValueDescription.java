/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.doc.soap;

import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlEnumValue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.zimbra.soap.type.ZmBoolean;


public class ValueDescription {
    @JsonIgnore
    private final static Joiner PIPE_JOINER = Joiner.on("|");
    private final String valueName;
    private final String representation;
    private final String className;
    private ArrayList<String> enumConsts = null;

    private ValueDescription() {
        valueName = null;
        representation = null;
        className = null;
    }

    private ValueDescription(String valueName, Class<?> klass) {
        this.valueName = valueName;
        className = klass.getName();
        buildEnumConsts(klass);
        representation = buildRepresentation(klass);
    }

    public static ValueDescription create(Class<?> klass) {
        return new ValueDescription(null, klass);
    }

    public static ValueDescription create(String valueName, Class<?> klass) {
        return new ValueDescription(valueName, klass);
    }

    private void buildEnumConsts(Class<?> klass) {
        if (!klass.isEnum()) {
            enumConsts = null;
            return;
        }
        enumConsts = Lists.newArrayList();
        for (Field field : klass.getFields()) {
            if (field.isEnumConstant()) {
                XmlEnumValue xmlEnumVal = field.getAnnotation(XmlEnumValue.class);
                if (xmlEnumVal == null) {
                    enumConsts.add(field.getName());
                } else {
                    enumConsts.add(xmlEnumVal.value());
                }
            }
        }
    }

    private String getEnumRepresentation(Class<?> klass) {
        return PIPE_JOINER.join(enumConsts);
    }

    private String buildRepresentation(Class<?> klass) {
        String value;
        if (klass.isAssignableFrom(ZmBoolean.class)) {
            value = "0|1";
        } else if (klass.isEnum()) {
            value = getEnumRepresentation(klass);
        } else {
            String classNameBase =
                    className.contains(".") ? className.substring(className.lastIndexOf('.') + 1) : className;
            if (valueName == null) {
                value = classNameBase;
            } else {
                if ("String".equals(classNameBase)) {
                    if (Strings.isNullOrEmpty(valueName)) {
                        value = "\"...\"";
                    } else {
                        value = String.format("{%s}", valueName);
                    } 
                } else {
                    if (Strings.isNullOrEmpty(valueName)) {
                            value = String.format("(%s)", classNameBase);
                    } else {
                        value = String.format("{%s} (%s)", valueName, classNameBase);
                    }
                }
            }
        }
        return value;
    }

    public String getValueName() { return valueName; }
    public String getRepresentation() { return representation; }
    public String getClassName() { return className; }
    public ArrayList<String> getEnumConsts() { return enumConsts; }

    public boolean isSame(ValueDescription other) {
        if (other == null) {
            return false;
        }
        if (className == null) {
            return (other.getClassName() == null);
        } else {
            if (other.getClassName() == null) {
                return false;
            }
        }
        return false;
    }
}
