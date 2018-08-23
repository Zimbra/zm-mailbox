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

package com.zimbra.soap.admin.type;

import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.XParamInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class XParam implements XParamInterface {

    /**
     * @zm-api-field-tag xparam-name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=MailConstants.A_NAME, required=true)
    private final String name;

    /**
     * @zm-api-field-tag xparam-name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=MailConstants.A_VALUE, required=true)
    private final String value;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private XParam() {
        this((String) null, (String) null);
    }

    public XParam(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public XParamInterface createFromNameAndValue(String name, String value) {
        return new XParam(name, value);
    }

    @Override
    public String getName() { return name; }
    @Override
    public String getValue() { return value; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name)
            .add("value", value)
            .toString();
    }

    public static Iterable <XParam> fromInterfaces(Iterable <XParamInterface> ifs) {
        if (ifs == null)
            return null;
        List <XParam> newList = Lists.newArrayList();
        for (XParamInterface listEnt : ifs) {
            newList.add((XParam) listEnt);
        }
        return newList;
    }

    public static List <XParamInterface> toInterfaces(Iterable <XParam> params) {
        if (params == null)
            return null;
        List <XParamInterface> newList = Lists.newArrayList();
        Iterables.addAll(newList, params);
        return newList;
    }
}
