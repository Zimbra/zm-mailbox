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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;

public class DistributionListRightInfo {

    /**
     * @zm-api-field-description Right
     */
    @XmlAttribute(name=AccountConstants.A_RIGHT, required=true)
    private final String right;

    /**
     * @zm-api-field-description Grantees
     */
    @XmlElement(name=AccountConstants.E_GRANTEE, required=false)
    protected List<DistributionListGranteeInfo> grantees;

    public DistributionListRightInfo() {
        this(null);
    }

    public DistributionListRightInfo(String right) {
        this.right = right;
    }

    public String getRight() {
        return right;
    }

    public void addGrantee(DistributionListGranteeInfo grantee) {
        if (grantees == null) {
            grantees = Lists.newArrayList();
        }
        grantees.add(grantee);
    }

    public void setGrantees(List<DistributionListGranteeInfo> grantees) {
        this.grantees = null;
        if (grantees != null) {
            this.grantees = Lists.newArrayList();
            Iterables.addAll(this.grantees, grantees);
        }
    }

    public List<DistributionListGranteeInfo> getGrantees() {
        return grantees;
    }
}
