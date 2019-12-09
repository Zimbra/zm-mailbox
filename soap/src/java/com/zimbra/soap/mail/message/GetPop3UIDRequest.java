/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.IdsAttr;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Get UID value which is set by SetPop3UIDRequest
 * <br />
 * This request returns the value set by the SetPop3UIDRequest.  Even you see
 * some custom value by this GetPop3UIDRequest, the actual output of POP3 UIDL
 * command depends on the LDAP attribute zimbraFeatureCustomUIDEnabled.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_POP3_UID_REQUEST)
public class GetPop3UIDRequest {

    /**
     * @zm-api-field-description Messages selector
     */
    @XmlElement(name=MailConstants.E_MSG /* m */, required=true)
    private final IdsAttr msgIds;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetPop3UIDRequest() {
        this((IdsAttr) null);
    }

    public GetPop3UIDRequest(IdsAttr msgIds) {
        this.msgIds = msgIds;
    }

    public IdsAttr getMsgIds() { return msgIds; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("msgIds", msgIds);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
