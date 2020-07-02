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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.IntervalRuleInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class IntervalRule
implements IntervalRuleInterface {

    /**
     * @zm-api-field-tag rule-interval
     * @zm-api-field-description Rule interval count - a positive integer
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_INTERVAL_IVAL /* ival */, required=true)
    private final int ival;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private IntervalRule() {
        this(-1);
    }

    public IntervalRule(int ival) {
        this.ival = ival;
    }

    public static IntervalRule create(int ival) {
        return new IntervalRule(ival);
    }

    @Override
    public int getIval() { return ival; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("ival", ival)
            .toString();
    }
}
