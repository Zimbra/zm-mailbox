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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class Prop {

    /**
     * @zm-api-field-tag prop-zimlet-name
     * @zm-api-field-description Zimlet name
     */
    @XmlAttribute(name=AccountConstants.A_ZIMLET /* zimlet */, required=true)
    private String zimlet;

    /**
     * @zm-api-field-tag prop-name
     * @zm-api-field-description Property name
     */
    @XmlAttribute(name=AccountConstants.A_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-tag prop-value
     * @zm-api-field-description Property value
     */
    @XmlValue
    private String value;
    public String getZimlet() {
        return zimlet;
    }
    public void setZimlet(String zimlet) {
        this.zimlet = zimlet;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    private static final String SEPARATOR = ":";
    private String serialization;

    public Prop() {
        //required for jaxb binding
    }

    public Prop(String zimlet, String name, String value) {
            this.zimlet = zimlet;
            this.name = name;
            this.value = value;
            this.serialization = makeSerialization();
    }

    public Prop(String serialization) throws IllegalArgumentException {
        this.serialization = serialization;
        int sep1 = serialization.indexOf(SEPARATOR);
        int sep2 = serialization.indexOf(SEPARATOR, sep1+1);
        if (sep1 < 0 || sep2 < 0) {
            throw new IllegalArgumentException(serialization);
        }
        zimlet = serialization.substring(0, sep1);
        name = serialization.substring(sep1+1, sep2);
        value = serialization.substring(sep2+1);
    }

    private String makeSerialization() {
        return zimlet + SEPARATOR + name + SEPARATOR + value;
    }

    public String getSerialization() {
        if (serialization == null) {
            serialization = makeSerialization();
        }
        return serialization;
    }

    public boolean matches(Prop other) {
        return (zimlet.equals(other.zimlet) && name.equals(other.name));
    }
    public void replace(Prop other) {
        this.zimlet = other.zimlet;
        this.name = other.name;
        this.value = other.value;
        this.serialization = other.serialization;
    }

    public static Multimap<String, String> toMultimap(List<Prop> props, String userPropKey) {
        Multimap<String, String> map = ArrayListMultimap.create();
        for (Prop p : props) {
            map.put(userPropKey, p.getSerialization());
        }
        return map;
    }
}
