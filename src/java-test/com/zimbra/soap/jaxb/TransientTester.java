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
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlRootElement;

/** Test JAXB class to exercise a field annotated with {@link XmlTransient} */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="transient-tester")
public class TransientTester {
    @XmlTransient
    private String toBeIgnored;

    @XmlAttribute(name="attr", required=true)
    private Integer nummer;

    public TransientTester() { }

    public TransientTester(String str, Integer num) {
        setToBeIgnored(str);
        setNummer(num);
    }

    public String getToBeIgnored() { return toBeIgnored; }
    public void setToBeIgnored(String toBeIgnored) { this.toBeIgnored = toBeIgnored; }

    public Integer getNummer() { return nummer; }
    public void setNummer(Integer nummer) { this.nummer = nummer; }

}
