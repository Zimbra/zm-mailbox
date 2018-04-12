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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("targetType", targetType)
            .add("accountBy", accountBy)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
