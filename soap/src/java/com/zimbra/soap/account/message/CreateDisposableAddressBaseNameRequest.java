/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;
import com.zimbra.common.soap.AccountConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Create base name for the disposable addresses of an account.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_CREATE_DISPOSABLE_ADDRESS_BASE_NAME_REQUEST)
public class CreateDisposableAddressBaseNameRequest {

    /**
     * @zm-api-field-description Details of the signature to be created
     */
    @XmlAttribute(name=AccountConstants.A_BASE_NAME, required=true)
    private final String baseName;

    /**
     * no-argument constructor is required by JAXB
     */
    @SuppressWarnings("unused")
    private CreateDisposableAddressBaseNameRequest() {
        this (null);
    }

    public CreateDisposableAddressBaseNameRequest(String baseName) {
        this.baseName = baseName;
    }

    public String getBaseName() { return baseName; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("baseName", baseName)
            .toString();
    }
}
