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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;

/**
 * Test JAXB class to exercise a field annotated with {@link XmlAnyElement}
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="any-elem-tester")
public class AnyTester {
    @XmlElement
    private String given;
    
    @XmlAnyElement
    private List<org.w3c.dom.Element> elems = Lists.newArrayList();

    public AnyTester() { }

    public List<org.w3c.dom.Element> getElems() { return elems; }
    public void setElems(List<org.w3c.dom.Element> elems) { this.elems = elems; }

    public String getGiven() { return given; }
    public void setGiven(String given) { this.given = given; }
}
