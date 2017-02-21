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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;

/** Test JAXB class to exercise a field annotated with {@link XmlElementRefs} and {@link XmlElementRef} */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="elem-ref-tester")
public class ElementRefsTester {
    @XmlElementRefs({
        @XmlElementRef(type=StringAttribIntValue.class), /* note: name not supplied as would be ignored anyway */
        @XmlElementRef(type=EnumAttribs.class)
    })
    private List<Object> elems = Lists.newArrayList();

    public ElementRefsTester() { }

    public List<Object> getElems() { return elems; }
    public void setElems(List<Object> elems) { this.elems = elems; }
}
