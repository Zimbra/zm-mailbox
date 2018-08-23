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
import com.zimbra.soap.base.ByHourRuleInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class ByHourRule implements ByHourRuleInterface {

    /**
     * @zm-api-field-tag hour-list
     * @zm-api-field-description Comma separated list of hours where hour is a number between 0 and 23
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_BYHOUR_HRLIST /* hrlist */, required=true)
    private final String list;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ByHourRule() {
        this((String) null);
    }

    public ByHourRule(String list) {
        this.list = list;
    }

    @Override
    public ByHourRuleInterface create(String list) {
        return new ByHourRule(list);
    }

    @Override
    public String getList() { return list; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("list", list)
            .toString();
    }
}
