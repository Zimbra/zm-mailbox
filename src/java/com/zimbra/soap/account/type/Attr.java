/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.soap.account.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.soap.base.KeyAndValue;
import com.zimbra.soap.type.ZmBoolean;

/**
 * e.g. For element named "attr":
 *          <attr name="{name}" [pd="true"]>{value}</attr>
 *
 * Note:  where the attribute name is "n" rather than "name" use {@link KeyValuePair}
 */
public class Attr implements KeyAndValue {

    public static Function<Attr, Attr> COPY = new Function<Attr, Attr>() {
        @Override
        public Attr apply(Attr from) {
            return new Attr(from);
        }
    };

    @XmlAttribute(name=AccountConstants.A_NAME /* name */, required=true)
    private String name;

    // If true, flags that the real value of this attribute has not been provided - i.e. value is set to ""
    @XmlAttribute(name=AccountConstants.A_PERM_DENIED /* pd */, required=false)
    private ZmBoolean permDenied;

    @XmlValue
    private String value;

    public Attr() {
    }

    public Attr(Attr attr) {
        name = attr.getName();
        value = attr.getValue();
        permDenied = ZmBoolean.fromBool(attr.getPermDenied());
    }

    public Attr(String name) {
        setName(name);
    }

    public Attr(String name, String value) {
        setName(name);
        setValue(value);
    }

    public static Attr forName(String name) {
        return new Attr(name);
    }

    public static Attr forNameAndValue(String name, String value) {
        return new Attr(name, value);
    }

    public static Attr forNameWithPermDenied(String name) {
        Attr attr = new Attr(name, "");
        attr.setPermDenied(true);
        return attr;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getPermDenied() { return ZmBoolean.toBool(permDenied); }
    public void setPermDenied(Boolean permDenied) { this.permDenied = ZmBoolean.fromBool(permDenied); }

    @Override
    public String getValue() { return value; }
    @Override
    public void setValue(String value) { this.value = value; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("value", value)
            .toString();
    }

    public static Multimap<String, String> toMultimap(List<? extends Attr> attrs) {
        Multimap<String, String> map = ArrayListMultimap.create();
        if (attrs != null) {
            for (Attr a : attrs) {
                map.put(a.getName(), a.getValue());
            }
        }
        return map;
    }

    public static List<Attr> fromMultimap(Multimap<String, String> attrMap) {
        List<Attr> attrs = new ArrayList<Attr>();
        if (attrMap != null) {
            for (Map.Entry<String, String> entry : attrMap.entries()) {
                attrs.add(new Attr(entry.getKey(), entry.getValue()));
            }
        }
        return attrs;
    }

    public static List <Attr> fromMap(Map<String, ? extends Object> attrs)
    throws ServiceException {
        List<Attr> newAttrs = Lists.newArrayList();
        if (attrs == null) return newAttrs;

        for (Entry<String, ? extends Object> entry : attrs.entrySet()) {
            String key = (String) entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                newAttrs.add(new Attr(key, (String) null));
            } else if (value instanceof String) {
                newAttrs.add(new Attr(key, (String) value));
            } else if (value instanceof String[]) {
                String[] values = (String[]) value;
                if (values.length == 0) {
                    // an empty array == removing the attr
                    newAttrs.add(new Attr(key, (String) null));
                } else {
                    for (String v: values) {
                        newAttrs.add(new Attr(key, v));
                    }
                }
            } else {
                throw ZClientException.CLIENT_ERROR(
                        "invalid attr type: " + key + " "
                        + value.getClass().getName(), null);
            }
        }
        return newAttrs;
    }

    @Override
    public void setKey(String key) { setName(key); }

    @Override
    public String getKey() { return getName(); }

}
