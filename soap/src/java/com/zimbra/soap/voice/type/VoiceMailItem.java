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

package com.zimbra.soap.voice.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class VoiceMailItem extends VoiceCallItem {

    /**
     * @zm-api-field-tag message-id
     * @zm-api-field-description Message ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private String messageId;

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Flags = 'u'(nread), '!'(high priority), 'p'(rivate, i.e. message is not forwardable)
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    /**
     * @zm-api-field-description Calling Party involved in the call or voice mail
     */
    @XmlElement(name=VoiceConstants.E_CALLPARTY /* cp */, required=false)
    private VoiceMailCallParty callParty;

    /**
     * @zm-api-field-description Content
     */
    @XmlElement(name=MailConstants.E_CONTENT /* content */, required=false)
    private VoiceMailContent voiceContent;

    public VoiceMailItem() {
    }

    public void setMessageId(String messageId) { this.messageId = messageId; }
    public void setFlags(String flags) { this.flags = flags; }
    public void setCallParty(VoiceMailCallParty callParty) { this.callParty = callParty; }
    public void setVoiceContent(VoiceMailContent voiceContent) { this.voiceContent = voiceContent; }
    public String getMessageId() { return messageId; }
    public String getFlags() { return flags; }
    public VoiceMailCallParty getCallParty() { return callParty; }
    public VoiceMailContent getVoiceContent() { return voiceContent; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("messageId", messageId)
            .add("flags", flags)
            .add("callParty", callParty)
            .add("voiceContent", voiceContent);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
