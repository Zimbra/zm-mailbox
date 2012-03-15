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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

import com.zimbra.soap.base.XPropInterface;
import com.zimbra.soap.base.XParamInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class XProp implements XPropInterface {

    /**
     * @zm-api-field-tag xprop-name
     * @zm-api-field-description XPROP name
     */
    @XmlAttribute(name=MailConstants.A_NAME, required=true)
    private final String name;

    /**
     * @zm-api-field-tag xprop-value
     * @zm-api-field-description XPROP value
     */
    @XmlAttribute(name=MailConstants.A_VALUE, required=true)
    private final String value;

    /**
     * @zm-api-field-description XPARAMs
     */
    @XmlElement(name=MailConstants.E_CAL_XPARAM, required=false)
    private List<XParam> xParams = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private XProp() {
        this((String) null, (String) null);
    }

    public XProp(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public XPropInterface createFromNameAndValue(String name, String value) {
        return new XProp(name, value);
    }

    public void setXParams(Iterable <XParam> xParams) {
        this.xParams.clear();
        if (xParams != null) {
            Iterables.addAll(this.xParams,xParams);
        }
    }

    public XProp addXParam(XParam xParam) {
        this.xParams.add(xParam);
        return this;
    }

    @Override
    public String getName() { return name; }
    @Override
    public String getValue() { return value; }

    public List<XParam> getXParams() {
        return Collections.unmodifiableList(xParams);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("value", value)
            .add("xParams", xParams)
            .toString();
    }

    @Override
    public void setXParamInterfaces(Iterable<XParamInterface> xParams) {
        setXParams(XParam.fromInterfaces(xParams));
    }

    @Override
    public void addXParamInterface(XParamInterface xParam) {
        addXParam((XParam) xParam);
    }

    @Override
    public List<XParamInterface> getXParamInterfaces() {
        return XParam.toInterfaces(xParams);
    }

    public static Iterable <XProp> fromInterfaces(Iterable <XPropInterface> params) {
        if (params == null)
            return null;
        List <XProp> newList = Lists.newArrayList();
        for (XPropInterface param : params) {
            newList.add((XProp) param);
        }
        return newList;
    }

    public static List <XPropInterface> toInterfaces(Iterable <XProp> params) {
        if (params == null)
            return null;
        List <XPropInterface> newList = Lists.newArrayList();
        Iterables.addAll(newList, params);
        return newList;
    }
}
