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
import com.zimbra.soap.base.WkstRuleInterface;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_WEEK_START_RULE, description="Week-start rule")
public class WkstRule
implements WkstRuleInterface {

    /**
     * @zm-api-field-tag weekday
     * @zm-api-field-description Weekday -  <b>SU|MO|TU|WE|TH|FR|SA</b>
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_DAY, required=true)
    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.DAY, description="Weekday - SU|MO|TU|WE|TH|FR|SA")
    private final String day;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private WkstRule() {
        this((String) null);
    }

    public WkstRule(@GraphQLNonNull @GraphQLInputField String day) {
        this.day = day;
    }

    @Override
    public WkstRuleInterface create(String day) {
        return new WkstRule(day);
    }

    @Override
    public String getDay() { return day; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("day", day).toString();
    }
}
