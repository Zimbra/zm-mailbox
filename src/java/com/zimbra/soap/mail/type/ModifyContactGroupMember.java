/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014 Zimbra, Inc.
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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ModifyContactGroupMember {

    /**
     * @zm-api-field-tag member-operation
     * @zm-api-field-description Operation - <b>+|-|reset</b>
     * <br />
     * <b>Required</b> when replace-mode is unset, otherwise, it <b>must not be set </b> - INVALID_REQUEST
     * will be thrown
     * <table>
     * <tr> <td> <b>+</b>     </td> <td> add the member - {member-type} and {member-value} required </td> </tr>
     * <tr> <td> <b>-</b>     </td> <td> remove the member - {member-type} and {member-value} required </td> </tr>
     * <tr> <td> <b>reset</b> </td> <td> delete all pre-existing members </td> </tr>
     * </table>
     */
    @XmlAttribute(name=MailConstants.A_OPERATION /* op */, required=false)
    private ModifyGroupMemberOperation operation;

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

    public ModifyContactGroupMember() {
    }

    public void setOperation(ModifyGroupMemberOperation operation) { this.operation = operation; }
    public void setType(String type) { this.type = type; }
    public void setValue(String value) { this.value = value; }
    public ModifyGroupMemberOperation getOperation() { return operation; }
    public String getType() { return type; }
    public String getValue() { return value; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("operation", operation)
            .add("type", type)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
