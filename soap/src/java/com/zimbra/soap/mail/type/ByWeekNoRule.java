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
import com.zimbra.soap.base.ByWeekNoRuleInterface;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_BY_WEEK_NO_RULE, description="By-week-no rule")
public class ByWeekNoRule implements ByWeekNoRuleInterface {

    /**
     * @zm-api-field-tag byweekno-wklist
     * @zm-api-field-description BYWEEKNO Week list.  Format : <b>[[+]|-]num[,...]</b> where num is between 1 and 53
     * <br />
     * e.g. <b>&lt;byweekno wklist="1,+2,-1"/></b> means first week, 2nd week, and last week of the year.
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_BYWEEKNO_WKLIST /* wklist */, required=true)
    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.LIST, description="BYWEEKNO Week list.  Format : [[+]|-]num[,...] where num is between 1 and 53")
    private final String list;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ByWeekNoRule() {
        this((String) null);
    }

    public ByWeekNoRule(@GraphQLNonNull @GraphQLInputField String list) {
        this.list = list;
    }

    @Override
    public ByWeekNoRuleInterface create(String list) {
        return new ByWeekNoRule(list);
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
