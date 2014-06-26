/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class WaitSetAddSpec {

    /**
     * @zm-api-field-tag waitset-name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag waitset-id
     * @zm-api-field-description
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag last-known-sync-token
     * @zm-api-field-description Last known sync token
     */
    @XmlAttribute(name=MailConstants.A_TOKEN /* token */, required=false)
    private String token;

    /**
     * @zm-api-field-tag waitset-types
     * @zm-api-field-description interest types: comma-separated list.  Currently:
     * <table>
     * <tr> <td> <b>f</b> </td> <td> folders </td> </tr>
     * <tr> <td> <b>m</b> </td> <td> messages </td> </tr>
     * <tr> <td> <b>c</b> </td> <td> contacts </td> </tr>
     * <tr> <td> <b>a</b> </td> <td> appointments </td> </tr>
     * <tr> <td> <b>t</b> </td> <td> tasks </td> </tr>
     * <tr> <td> <b>d</b> </td> <td> documents </td> </tr>
     * <tr> <td> <b>all</b> </td> <td> all types (equiv to "f,m,c,a,t,d") </td> </tr>
     * </table>
     */
    @XmlAttribute(name=MailConstants.A_TYPES /* types */, required=false)
    private String interests;

    public WaitSetAddSpec() {
    }

    public void setName(String name) { this.name = name; }
    public void setId(String id) { this.id = id; }
    public void setToken(String token) { this.token = token; }
    public void setInterests(String interests) { this.interests = interests; }
    public String getName() { return name; }
    public String getId() { return id; }
    public String getToken() { return token; }
    public String getInterests() { return interests; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("id", id)
            .add("token", token)
            .add("interests", interests);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
