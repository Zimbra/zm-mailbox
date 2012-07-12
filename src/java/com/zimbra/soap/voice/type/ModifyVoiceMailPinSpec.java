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

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.VoiceConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ModifyVoiceMailPinSpec {

    /**
     * @zm-api-field-tag phone-name
     * @zm-api-field-description Phone name
     */
    @XmlAttribute(name=AccountConstants.A_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag old-pin
     * @zm-api-field-description Old PIN
     */
    @XmlAttribute(name=VoiceConstants.A_VMAIL_OLD_PIN /* oldPin */, required=true)
    private String oldVoiceMailPin;

    /**
     * @zm-api-field-tag new-pin
     * @zm-api-field-description New PIN
     */
    @XmlAttribute(name=VoiceConstants.A_VMAIL_PIN /* pin */, required=true)
    private String newVoiceMailPin;

    public ModifyVoiceMailPinSpec() {
    }

    public void setName(String name) { this.name = name; }
    public void setOldVoiceMailPin(String oldVoiceMailPin) { this.oldVoiceMailPin = oldVoiceMailPin; }
    public void setNewVoiceMailPin(String newVoiceMailPin) { this.newVoiceMailPin = newVoiceMailPin; }
    public String getName() { return name; }
    public String getOldVoiceMailPin() { return oldVoiceMailPin; }
    public String getNewVoiceMailPin() { return newVoiceMailPin; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("oldVoiceMailPin", oldVoiceMailPin)
            .add("newVoiceMailPin", newVoiceMailPin);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
