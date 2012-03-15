/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.account.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.ProfileInfo;

/**
 * @zm-api-command-description Update profile
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_UPDATE_PROFILE_REQUEST)
public class UpdateProfileRequest {

    /**
     * @zm-api-field-description New profile information
     */
    @XmlElement(name=AccountConstants.E_PROFILE /* profile */, required=true)
    private final ProfileInfo profile;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private UpdateProfileRequest() {
        this((ProfileInfo) null);
    }

    public UpdateProfileRequest(ProfileInfo profile) {
        this.profile = profile;
    }

    public ProfileInfo getProfile() { return profile; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("profile", profile);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
