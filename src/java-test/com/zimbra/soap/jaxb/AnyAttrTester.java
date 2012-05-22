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
package com.zimbra.soap.jaxb;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Maps;

/**
 * Test JAXB class to exercise a field annotated with {@link XmlAnyAttribute}
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="any-attr-tester")
public class AnyAttrTester {
    @XmlAttribute
    private String given;

    @XmlAnyAttribute
    private Map<javax.xml.namespace.QName,Object> extraAttributes = Maps.newHashMap();

    public AnyAttrTester() { }

    public String getGiven() { return given; }
    public void setGiven(String given) { this.given = given; }

    public Map<javax.xml.namespace.QName,Object> getExtraAttributes() { return extraAttributes; }
    public void setExtraAttributes(Map<javax.xml.namespace.QName,Object> extraAttributes) {
        this.extraAttributes = extraAttributes;
    }
}
