/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class MsgToSend
extends Msg {

    /**
     * @zm-api-field-tag saved-draft-id
     * @zm-api-field-description Saved draft ID
     */
    @XmlAttribute(name=MailConstants.A_DRAFT_ID /* did */, required=false)
    private String draftId;
    
    /**
     * @zm-api-field-tag send-from-draft
     * @zm-api-field-description If set, message gets constructed based on the "did" (id of the draft).
     */
    @XmlAttribute(name=MailConstants.A_SEND_FROM_DRAFT /* sfd */, required=false)
    private ZmBoolean sendFromDraft;

    /**
     * @zm-api-field-tag datasource-id
     * @zm-api-field-description Id of the data source in case SMTP settings of that data source must be used for
     * sending the message.
     */
    @XmlAttribute(name=MailConstants.A_DATASOURCE_ID /* dsId */, required=false)
    private String dataSourceId;

    public MsgToSend() {
    }

    public void setDraftId(String draftId) { this.draftId = draftId; }
    public String getDraftId() { return draftId; }
    
    public void setSendFromDraft(Boolean sendFromDraft) {
        this.sendFromDraft = ZmBoolean.fromBool(sendFromDraft);
    }
    public Boolean getSendFromDraft() { return ZmBoolean.toBool(sendFromDraft); }

    public void setDataSourceId(String dsId) { this.dataSourceId = dsId; }
    public String getDataSourceId() { return dataSourceId; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("draftId", draftId)
            .add("sendFromDraft", sendFromDraft)
            .add("dataSourceId", dataSourceId);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
