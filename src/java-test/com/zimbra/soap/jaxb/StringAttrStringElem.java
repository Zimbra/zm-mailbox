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
