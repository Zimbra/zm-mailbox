/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
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
