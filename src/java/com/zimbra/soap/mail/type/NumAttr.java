/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.NumAttrInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class NumAttr
implements NumAttrInterface {

    /**
     * @zm-api-field-tag num
     * @zm-api-field-description Number
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_COUNT_NUM /* num */, required=true)
    private final int num;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private NumAttr() {
        this(-1);
    }

    public NumAttr(int num) {
        this.num = num;
    }

    @Override
    public NumAttrInterface create(int num) {
        return new NumAttr(num);
    }

    @Override
    public int getNum() { return num; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("num", num)
            .toString();
    }
}
