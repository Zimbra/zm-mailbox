/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.voice.type;

import com.google.common.base.Objects;
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

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("messageId", messageId)
            .add("flags", flags)
            .add("callParty", callParty)
            .add("voiceContent", voiceContent);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
