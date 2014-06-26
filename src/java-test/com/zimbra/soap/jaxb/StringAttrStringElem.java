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

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/** Test JAXB class with a String XmlAttribute and a String XmlElement */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="string-attr-string-elem", namespace="urn:ZimbraTest2")
public class StringAttrStringElem {
    @XmlAttribute(name="attribute-1", required=true)
    private String attr1;
    @XmlElement(name="element1", namespace="urn:ZimbraTest3", required=true)
    private String elem1;
    public StringAttrStringElem() { }
    public String getAttr1() { return attr1; }
    public void setAttr1(String attr1) { this.attr1 = attr1; }
    public String getElem1() { return elem1; }
    public void setElem1(String elem1) { this.elem1 = elem1; }
}
