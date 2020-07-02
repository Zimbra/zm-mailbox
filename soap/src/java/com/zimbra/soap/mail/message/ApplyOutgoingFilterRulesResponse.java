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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.IdsAttr;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_APPLY_OUTGOING_FILTER_RULES_RESPONSE)
public class ApplyOutgoingFilterRulesResponse {

    /**
     * @zm-api-field-tag comma-sep-msg-ids
     * @zm-api-field-description Comma-separated list of message IDs that were affected
     */
    @XmlElement(name=MailConstants.E_MSG, required=false)
    private IdsAttr msgIds;

    public ApplyOutgoingFilterRulesResponse() {
    }

    public void setMsgIds(IdsAttr msgIds) { this.msgIds = msgIds; }
    public IdsAttr getMsgIds() { return msgIds; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("msgIds", msgIds)
            .toString();
    }
}
