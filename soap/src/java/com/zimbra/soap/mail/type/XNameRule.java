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

import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.XNameRuleInterface;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_XNAME_RULE, description="XName Rule")
public class XNameRule
implements XNameRuleInterface {

    /**
     * @zm-api-field-tag xname-name
     * @zm-api-field-description XNAME Name
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_XNAME_NAME /* name */, required=false)
    @GraphQLQuery(name=GqlConstants.NAME, description="XNAME Name")
    private final String name;

    /**
     * @zm-api-field-tag xname-value
     * @zm-api-field-description XNAME Value
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_XNAME_VALUE /* value */, required=false)
    @GraphQLQuery(name=GqlConstants.VALUE, description="XNAME value")
    private final String value;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private XNameRule() {
        this((String) null, (String) null);
    }

    public XNameRule(@GraphQLInputField String name, @GraphQLInputField String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public XNameRuleInterface createFromNameAndValue(String name, String value) {
        return new XNameRule(name, value);
    }

    @Override
    public String getName() { return name; }
    @Override
    public String getValue() { return value; }

    public static Iterable <XNameRule> fromInterfaces(
                    Iterable <XNameRuleInterface> ifs) {
        if (ifs == null)
            return null;
        List <XNameRule> newList = Lists.newArrayList();
        for (XNameRuleInterface listEnt : ifs) {
            newList.add((XNameRule) listEnt);
        }
        return newList;
    }

    public static List <XNameRuleInterface> toInterfaces(
                    Iterable <XNameRule> params) {
        if (params == null)
            return null;
        List <XNameRuleInterface> newList = Lists.newArrayList();
        Iterables.addAll(newList, params);
        return newList;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
