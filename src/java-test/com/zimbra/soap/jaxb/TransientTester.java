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
