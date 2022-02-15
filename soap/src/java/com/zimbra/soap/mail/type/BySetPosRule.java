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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.soap.base.BySetPosRuleInterface;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_BY_SET_POS_RULE, description="By-set-pos rule")
public class BySetPosRule implements BySetPosRuleInterface {

    /**
     * @zm-api-field-tag bysetpos-list
     * @zm-api-field-description Format <b>[[+]|-]num[,...]</b> where num is from 1 to 366
     * <br />
     * <b>&lt;bysetpos></b> MUST only be used in conjunction with another <b>&lt;byXXX></b> element.
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_BYSETPOS_POSLIST /* poslist */, required=true)
    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.LIST, description="Format [[+]|-]num[,...] where num is from 1 to 366")
    private final String list;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BySetPosRule() {
        this((String) null);
    }

    public BySetPosRule(@GraphQLNonNull @GraphQLInputField String list) {
        this.list = list;
    }

    @Override
    public BySetPosRuleInterface create(String list) {
        return new BySetPosRule(list);
    }

    @Override
    public String getList() { return list; }

    public List<Integer> getAsList() {
        if (StringUtil.isNullOrEmpty(list)) {
            return null;
        }
        String[] arr = list.split(",");
        List<Integer> toReturn = new ArrayList<Integer>();
        for (String a : arr) {
            toReturn.add(Integer.valueOf(a));
        }
        return toReturn;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("list", list)
            .toString();
    }
}
