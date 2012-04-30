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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/** Test JAXB class with a String XmlAttribute and int XmlValue */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="string-attr-int-value")
public class StringAttribIntValue {
    @XmlAttribute(name="attr1", required=true)
    private String attrib1;
    @XmlValue()
    private int myValue;

    public StringAttribIntValue() {}
    public StringAttribIntValue(String a, int v) {
        setAttrib1(a);
        setMyValue(v);
    }

    public String getAttrib1() { return attrib1; }
    public void setAttrib1(String attrib1) { this.attrib1 = attrib1; }
    public int getMyValue() { return myValue; }
    public void setMyValue(int myValue) { this.myValue = myValue; }
}
