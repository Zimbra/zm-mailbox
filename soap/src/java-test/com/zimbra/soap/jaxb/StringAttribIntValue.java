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
