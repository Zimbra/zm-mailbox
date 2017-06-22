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

import com.google.common.base.Objects;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ModifyContactGroupMember extends NewContactGroupMember {

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

    public ModifyContactGroupMember() {
    }

    public ModifyContactGroupMember(String type, String value) {
        super(type, value);
    }

    public static ModifyContactGroupMember createForTypeAndValue(String type, String value) {
        return new ModifyContactGroupMember(type, value);
    }

    public void setOperation(ModifyGroupMemberOperation operation) { this.operation = operation; }
    public ModifyGroupMemberOperation getOperation() { return operation; }

    @Override
    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return super.addToStringInfo(helper)
            .add("operation", operation);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
