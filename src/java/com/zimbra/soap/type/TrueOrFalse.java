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
