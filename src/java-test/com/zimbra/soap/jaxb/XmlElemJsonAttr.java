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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

/** Test JAXB class to demonstrate affect of {@link ZimbraJsonAttribute} annotation */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="XmlElemJsonAttr")
public class XmlElemJsonAttr {
    @ZimbraJsonAttribute
    @XmlElement(name="xml-elem-json-attr", required=false)
    private String xmlElemJsonAttr;

    @XmlElement(name="classic-elem", required=false)
    private String defaultElem;

    public XmlElemJsonAttr() { }

    public XmlElemJsonAttr(String xmlElemJsonAttr, String defaultElem) {
        setXmlElemJsonAttr(xmlElemJsonAttr);
        setDefaultElem(defaultElem);
    }

    public String getXmlElemJsonAttr() { return xmlElemJsonAttr; }
    public void setXmlElemJsonAttr(String xmlElemJsonAttr) { this.xmlElemJsonAttr = xmlElemJsonAttr; }

    public String getDefaultElem() { return defaultElem; }
    public void setDefaultElem(String defaultElem) { this.defaultElem = defaultElem; }
}
