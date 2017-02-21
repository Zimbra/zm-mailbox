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

package com.zimbra.soap.type;

import java.util.Arrays;
import java.util.Map;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.google.common.collect.Maps;
import com.zimbra.common.service.ServiceException;

/**
 * Type that renders "true" for true and "false" for false for BOTH XML and JSON SOAP variants.
 * Compare with {@link ZmBoolean} and {@link ZeroOrOne}
 */
@XmlEnum
public enum TrueOrFalse {
    @XmlEnumValue("true") TRUE("true"),
    @XmlEnumValue("false") FALSE("false");

    private static Map<String, TrueOrFalse> nameToView = Maps.newHashMap();

    static {
        for (TrueOrFalse v : TrueOrFalse.values()) {
            nameToView.put(v.toString(), v);
        }
    }

    private String name;

    private TrueOrFalse(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static TrueOrFalse fromString(String name)
    throws ServiceException {
        TrueOrFalse op = nameToView.get(name);
        if (op == null) {
           throw ServiceException.INVALID_REQUEST("unknown value: " + name + ", valid values: " +
                   Arrays.asList(TrueOrFalse.values()), null);
        }
        return op;
    }

    public static TrueOrFalse fromBool(boolean val) {
        if (val) {
            return TRUE;
        } else {
            return FALSE;
        }
    }
    public static boolean toBool(TrueOrFalse val) {
        if (val.equals(TRUE)) {
            return true;
        } else {
            return false;
        }
    }
}
