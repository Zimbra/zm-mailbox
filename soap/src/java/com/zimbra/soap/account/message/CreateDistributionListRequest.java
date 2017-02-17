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

package com.zimbra.soap.account.message;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.AccountKeyValuePairs;
import com.zimbra.soap.type.KeyValuePair;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Create a Distribution List
 * <p>
 * Note:<br />
 *  authed account must have the privilege to create dist lists in the domain
 * </p>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_CREATE_DISTRIBUTION_LIST_REQUEST)
public class CreateDistributionListRequest extends AccountKeyValuePairs {

    /**
     * @zm-api-field-description Name for the new Distribution List
     */
    @XmlAttribute(name=AccountConstants.E_NAME, required=true)
    private String name;

    /**
     * @zm-api-field-description Flag type of distribution list to create
     * <table>
     * <tr> <td> <b>set to 1 (true)</b> [default] </td> <td> create a dynamic distribution list </td> </tr>
     * <tr> <td> <b>set to 0 (false)</b> </td> <td> create a static distribution list </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AccountConstants.A_DYNAMIC, required=false)
    private ZmBoolean dynamic;

    public CreateDistributionListRequest() {
        this((String)null);
    }

    public CreateDistributionListRequest(String name) {
        this(name, (Collection<KeyValuePair>) null, false);
    }

    public CreateDistributionListRequest(String name, Collection<KeyValuePair> attrs, Boolean dynamic) {
        super(attrs);
        this.name = name;
        this.dynamic = ZmBoolean.fromBool(dynamic);
    }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }
    public Boolean getDynamic() { return ZmBoolean.toBool(dynamic); }
}
