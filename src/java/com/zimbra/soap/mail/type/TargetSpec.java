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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.AccountBy;
import com.zimbra.soap.type.TargetType;

@XmlAccessorType(XmlAccessType.NONE)
public class TargetSpec {

    /**
     * @zm-api-field-tag target-type
     * @zm-api-field-description Target type
     */
    @XmlAttribute(name=MailConstants.A_TARGET_TYPE /* type */, required=true)
    private final TargetType targetType;

    /**
     * @zm-api-field-tag target-selector-by
     * @zm-api-field-description Select the meaning of <b>{target-selector-key}</b>
     */
    @XmlAttribute(name=MailConstants.A_TARGET_BY /* by */, required=true)
    private final AccountBy accountBy;

    /**
     * @zm-api-field-tag target-selector-key
     * @zm-api-field-description The key used to identify the target. Meaning determined by <b>{target-selector-by}</b>
     */
    @XmlValue
    private String value;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private TargetSpec() {
        this((TargetType) null, (AccountBy) null);
    }

    public TargetSpec(TargetType targetType, AccountBy accountBy) {
        this.targetType = targetType;
        this.accountBy = accountBy;
    }

    public void setValue(String value) { this.value = value; }
    public TargetType getTargetType() { return targetType; }
    public AccountBy getAccountBy() { return accountBy; }
    public String getValue() { return value; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("targetType", targetType)
            .add("accountBy", accountBy)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
