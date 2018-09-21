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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.ByMonthDayRuleInterface;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_BY_MONTH_DAY_RULE, description="By-month-day rule")
public class ByMonthDayRule implements ByMonthDayRuleInterface {

    /**
     * @zm-api-field-tag modaylist
     * @zm-api-field-description Comma separated list of day numbers from either the start (positive) or the
     * end (negative) of the month - format : <b>[[+]|-]num[,...]</b>   where num between 1 to 31
     * <br />
     * e.g. <b>modaylist="1,+2,-7"</b>
     * <br />
     * means first day of the month, plus the 2nd day of the month, plus the 7th from last day of the month.
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_BYMONTHDAY_MODAYLIST /* modaylist */, required=true)
    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.LIST, description="Comma separated list of day numbers from either the start (positive) or the end (negative) of the month - format : <b>[[+]|-]num[,...]</b>   where num between 1 to 31")
    private final String list;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ByMonthDayRule() {
        this((String) null);
    }

    public ByMonthDayRule(@GraphQLNonNull @GraphQLInputField String list) {
        this.list = list;
    }

    @Override
    public ByMonthDayRuleInterface create(String list) {
        return new ByMonthDayRule(list);
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
