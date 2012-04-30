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

/** Test JAXB class with an enum XmlAttribute and an enum XmlElement */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="enumAttribAndEnumElem")
public class EnumAttribEnumElem {
    @XmlAttribute(name="fold1", required=true)
    private ViewEnum fold1;
    @XmlElement(name="fold2", required=false)
    private ViewEnum fold2;

    public EnumAttribEnumElem() {}

    public ViewEnum getFold1() { return fold1; }
    public void setFold1(ViewEnum fold1) { this.fold1 = fold1; }
    public ViewEnum getFold2() { return fold2; }
    public void setFold2(ViewEnum fold2) { this.fold2 = fold2; }
}
