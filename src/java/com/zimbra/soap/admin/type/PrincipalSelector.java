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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AutoProvPrincipalBy;

@XmlAccessorType(XmlAccessType.NONE)
public class PrincipalSelector {

    /**
     * @zm-api-field-tag principal-selector-by
     * @zm-api-field-description Select the meaning of <b>{principal-selector-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY /* by */, required=true)
    private AutoProvPrincipalBy by;

    /**
     * @zm-api-field-tag principal-selector-key
     * @zm-api-field-description The key used to identify the principal.
     * Meaning determined by <b>{principal-selector-by}</b>
     */
    @XmlValue
    private String key;

    private PrincipalSelector() {
    }

    private PrincipalSelector(AutoProvPrincipalBy by, String key) {
        setBy(by);
        setKey(key);
    }

    public static PrincipalSelector create(AutoProvPrincipalBy by, String key) {
        return new PrincipalSelector(by, key);
    }

    public void setBy(AutoProvPrincipalBy by) { this.by = by; }
    public void setKey(String key) { this.key = key; }
    public AutoProvPrincipalBy getBy() { return by; }
    public String getKey() { return key; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("by", by)
            .add("key", key);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
