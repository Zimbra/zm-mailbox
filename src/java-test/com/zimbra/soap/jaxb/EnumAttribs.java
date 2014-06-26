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

/** Test JAXB class with 2 enum XmlAttributes */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="enumEttribs")
public class EnumAttribs {
    @XmlAttribute(name="fold1", required=true)
    private ViewEnum fold1;
    @XmlAttribute(name="fold2", required=true)
    private ViewEnum fold2;

    public EnumAttribs() {}

    public ViewEnum getFold1() { return fold1; }
    public void setFold1(ViewEnum fold1) { this.fold1 = fold1; }
    public ViewEnum getFold2() { return fold2; }
    public void setFold2(ViewEnum fold2) { this.fold2 = fold2; }
}
