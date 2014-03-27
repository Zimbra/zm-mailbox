/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
