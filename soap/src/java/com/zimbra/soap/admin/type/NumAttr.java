/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
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
        return MoreObjects.toStringHelper(this)
            .add("num", num)
            .toString();
    }
}
