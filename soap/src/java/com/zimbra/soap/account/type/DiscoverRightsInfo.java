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
package com.zimbra.soap.account.type;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AccountConstants;

import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_DISCOVER_RIGHTS_INFO, description="")
public class DiscoverRightsInfo {

    /**
     * @zm-api-field-tag targets-right
     * @zm-api-field-description Right the targets relate to
     */
    @XmlAttribute(name=AccountConstants.A_RIGHT /* right */, required=true)
    private String right;

    /**
     * @zm-api-field-description Targets
     */
    @XmlElement(name=AccountConstants.E_TARGET /* target */, required=true)
    private List<DiscoverRightsTarget> targets = Lists.newArrayList();

    public DiscoverRightsInfo() {
        this(null);
    }

    public DiscoverRightsInfo(String right) {
        this(right, null);
    }

    public DiscoverRightsInfo(String right, Iterable<DiscoverRightsTarget> targets) {
        setRight(right);
        if (targets != null) {
            setTargets(targets);
        }
    }

    public void setRight(String right) {
        this.right = right;
    }

    public void setTargets(Iterable<DiscoverRightsTarget> targets) {
        this.targets = Lists.newArrayList(targets);
    }

    public void addTarget(DiscoverRightsTarget target) {
        targets.add(target);
    }

    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.RIGHT, description="Right the targets relate to")
    public String getRight() {
        return right;
    }

    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.TARGETS, description="Targets")
    public List<DiscoverRightsTarget> getTargets() {
        return targets;
    }


}
