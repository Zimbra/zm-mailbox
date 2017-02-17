/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/** Test JAXB class with an XmlElement list of enums */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="wrapper-absent-if-empty")
public class WrapperAbsentIfEmpty {
    @XmlTransient
    private final List<Integer> numbers = Lists.newArrayList();

    public WrapperAbsentIfEmpty() { }

    @XmlElementWrapper(name = "numbers", required=false)
    @XmlElement(name = "number", required=false)
    public List<Integer> getNumbers() {
        if (numbers.isEmpty()) {
            return null;
        } else {
            return numbers;
        }
    }

    public void setNumbers(List<Integer> entries) {
        this.numbers.clear();
        if (entries != null) {
            Iterables.addAll(this.numbers,entries);
        }
    }

    public void addNumber(Integer number) {
        this.numbers.add(number);
    }
}
