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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.AddMsgSpec;
import com.zimbra.soap.type.ZmBoolean;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Add a message
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_ADD_MSG_REQUEST)
public class AddMsgRequest {

    /**
     * @zm-api-field-tag filter-sent
     * @zm-api-field-description If set, then do outgoing message filtering if the msg is being added to the Sent
     * folder and has been flagged as sent. Default is unset.
     */
    @XmlAttribute(name=MailConstants.A_FILTER_SENT /* filterSent */, required=false)
    private ZmBoolean filterSent;

    /**
     * @zm-api-field-description Specification of the message to add
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_MSG /* m */, required=true)
    private final AddMsgSpec msg;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AddMsgRequest() {
        this((AddMsgSpec) null);
    }

    public AddMsgRequest(AddMsgSpec msg) {
        this.msg = msg;
    }

    public void setFilterSent(Boolean filterSent) {
        this.filterSent = ZmBoolean.fromBool(filterSent);
    }
    public Boolean getFilterSent() { return ZmBoolean.toBool(filterSent); }
    public AddMsgSpec getMsg() { return msg; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("filterSent", filterSent)
            .add("msg", msg);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
