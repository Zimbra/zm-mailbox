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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/** Test JAXB class for exercising changes in namespace associated with elements */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="ns-delta", namespace="urn:ZimbraTest4")
public class NamespaceDeltaElem {
    @XmlElement(name="strAttrStrElem", namespace="urn:ZimbraTest5")
    private StringAttrStringElem sase;
    public NamespaceDeltaElem() { }
    public StringAttrStringElem getSase() { return sase; }
    public void setSase(StringAttrStringElem sase) { this.sase = sase; }
}
