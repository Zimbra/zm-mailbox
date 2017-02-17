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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.base.DistributionListGranteeInfoInterface;
import com.zimbra.soap.type.GranteeType;

@XmlAccessorType(XmlAccessType.NONE)
public class DistributionListGranteeInfo implements DistributionListGranteeInfoInterface {

    /**
     * @zm-api-field-tag dl-grantee-type
     * @zm-api-field-description Grantee Type.
     * <br />
     * <table>
     * <tr><td> <b>usr</b> </td> <td> a Zimbra internal user </td> </tr>
     * <tr><td> <b>grp</b> </td> <td> a Zimbra internal group </td> </tr>
     * <tr><td> <b>egp</b> </td> <td> an external AD group </td> </tr>
     * <tr><td> <b>all</b> </td> <td> all Zimbra users (id and name will not be present) </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AccountConstants.A_TYPE, required=true)
    private final GranteeType type;

    /**
     * @zm-api-field-tag dl-owner-or-grantee-id
     * @zm-api-field-description ID of owner of grantee
     */
    @XmlAttribute(name=AccountConstants.A_ID, required=true)
    private final String id;

    /**
     * @zm-api-field-tag dl-owner-or-grantee-name
     * @zm-api-field-description Name of owner of grantee
     */
    @XmlAttribute(name=AccountConstants.A_NAME, required=true)
    private final String name;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DistributionListGranteeInfo() {
        this(null, null, null);
    }

    public DistributionListGranteeInfo(GranteeType type, String id, String name) {
        this.type = type;
        this.id = id;
        this.name = name;
    }

    @Override
    public GranteeType getType() { return type; }
    @Override
    public String getId() { return id; }
    @Override
    public String getName() { return name; }
}

