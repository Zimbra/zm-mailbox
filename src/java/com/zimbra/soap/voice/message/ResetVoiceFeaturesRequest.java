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

package com.zimbra.soap.voice.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.soap.voice.type.ResetPhoneVoiceFeaturesSpec;
import com.zimbra.soap.voice.type.StorePrincipalSpec;

/**
 * @zm-api-command-description Reset call features of a phone.
 * <br />
 * If no <b>&lt;{call-feature}></b> are provided, all subscribed call features for the phone are reset to the default
 * values.  If <b>&lt;{call-feature}></b> elements are provided, only those call features are reset.
 * <br />
 * <br />
 * Note: if <b>&lt;{call-feature}></b> should NOT be <b>&lt;voicemailprefs></b>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=VoiceConstants.E_RESET_VOICE_FEATURES_REQUEST)
public class ResetVoiceFeaturesRequest {

    /**
     * @zm-api-field-description Store Principal specification
     */
    @XmlElement(name=VoiceConstants.E_STOREPRINCIPAL /* storeprincipal */, required=false)
    private StorePrincipalSpec storePrincipal;

    /**
     * @zm-api-field-description Features to reset for a phone
     */
    @XmlElement(name=VoiceConstants.E_PHONE /* phone */, required=false)
    private ResetPhoneVoiceFeaturesSpec phone;

    public ResetVoiceFeaturesRequest() {
    }

    public void setStorePrincipal(StorePrincipalSpec storePrincipal) { this.storePrincipal = storePrincipal; }
    public void setPhone(ResetPhoneVoiceFeaturesSpec phone) { this.phone = phone; }
    public StorePrincipalSpec getStorePrincipal() { return storePrincipal; }
    public ResetPhoneVoiceFeaturesSpec getPhone() { return phone; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("storePrincipal", storePrincipal)
            .add("phone", phone);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
