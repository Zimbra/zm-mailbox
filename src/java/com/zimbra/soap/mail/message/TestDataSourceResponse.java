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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_TEST_DATA_SOURCE_RESPONSE)
public class TestDataSourceResponse {

    /**
     * @zm-api-field-tag success
     * @zm-api-field-description Flags whether the test was successful
     */
    @XmlAttribute(name=MailConstants.A_DS_SUCCESS /* success */, required=true)
    private final ZmBoolean success;

    /**
     * @zm-api-field-tag error-message
     * @zm-api-field-description Error message
     */
    @XmlAttribute(name=MailConstants.A_DS_ERROR /* error */, required=false)
    private String error;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private TestDataSourceResponse() {
        this(false);
    }

    public TestDataSourceResponse(boolean success) {
        this.success = ZmBoolean.fromBool(success);
    }

    public void setError(String error) { this.error = error; }
    public boolean getSuccess() { return ZmBoolean.toBool(success); }
    public String getError() { return error; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("success", success)
            .add("error", error);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
