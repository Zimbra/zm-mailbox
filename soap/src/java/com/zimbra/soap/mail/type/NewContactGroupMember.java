/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.MoreObjects;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_NEW_CONTACT_GROUP_MEMBER, description="Input for creating a new contact group member")
public class NewContactGroupMember {

    /**
     * @zm-api-field-tag member-type
     * @zm-api-field-description Member type
     * <table>
     * <tr> <td> <b>C</b> </td> <td> reference to another contact </td> </tr>
     * <tr> <td> <b>G</b> </td> <td> reference to a GAL entry </td> </tr>
     * <tr> <td> <b>I</b> </td>
     *      <td> inlined member (member name and email address is embeded in the contact group)</td> </tr>
     * </table>
     */
    @XmlAttribute(name=MailConstants.A_CONTACT_GROUP_MEMBER_TYPE /* type */, required=true)
    private String type;

    /**
     * @zm-api-field-tag member-value
     * @zm-api-field-description Member value
     * <table>
     * <tr> <td> <b>type="C"</b> </td>
     *      <td> Item ID of another contact.  If the referenced contact is in a shared folder, the item ID must be
     *           qualified by zimbraId of the owner.  e.g. {zimbraId}:{itemId} </td> </tr>
     * <tr> <td> <b>type="G"</b> </td> <td> GAL entry reference (returned in SearchGalResponse) </td> </tr>
     * <tr> <td> <b>type="I"</b> </td>
     *      <td> name and email address in the form of: <b>"{name}" &lt;{email}></b> </td> </tr>
     * </table>
     */
    @XmlAttribute(name=MailConstants.A_CONTACT_GROUP_MEMBER_VALUE /* value */, required=true)
    private String value;

    public NewContactGroupMember() {
    }

    protected NewContactGroupMember(String type, String value) {
        setType(type);
        setValue(value);
    }

    public static NewContactGroupMember createForTypeAndValue(String type, String value) {
        return new NewContactGroupMember(type, value);
    }

    @GraphQLInputField(name=GqlConstants.TYPE, description="Member type. C|G|I")
    public void setType(@GraphQLNonNull String type) { this.type = type; }
    @GraphQLInputField(name=GqlConstants.VALUE, description="Member value")
    public void setValue(@GraphQLNonNull String value) { this.value = value; }
    public String getType() { return type; }
    public String getValue() { return value; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("type", type)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
