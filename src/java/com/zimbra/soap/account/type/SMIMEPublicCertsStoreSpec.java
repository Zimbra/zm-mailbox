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

package com.zimbra.soap.account.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.SourceLookupOpt;
import com.zimbra.soap.type.StoreLookupOpt;

@XmlAccessorType(XmlAccessType.FIELD)
public class SMIMEPublicCertsStoreSpec {

    @XmlAttribute(name=AccountConstants.A_SMIME_STORE_LOOKUP_OPT
                            /* storeLookupOpt */, required=false)
    private StoreLookupOpt storeLookupOpt;

    @XmlAttribute(name=AccountConstants.A_SMIME_SOURCE_LOOKUP_OPT
                            /* sourceLookupOpt */, required=false)
    private SourceLookupOpt sourceLookupOpt;

    // Comma separated list - valid values CONTACT GAL and LDAP
    @XmlValue
    private SourceLookupOpt storeTypes;

    public SMIMEPublicCertsStoreSpec() {
    }

    public void setStoreLookupOpt(StoreLookupOpt storeLookupOpt) {
        this.storeLookupOpt = storeLookupOpt;
    }
    public void setSourceLookupOpt(SourceLookupOpt sourceLookupOpt) {
        this.sourceLookupOpt = sourceLookupOpt;
    }
    public void setStoreTypes(SourceLookupOpt storeTypes) {
        this.storeTypes = storeTypes;
    }
    public StoreLookupOpt getStoreLookupOpt() { return storeLookupOpt; }
    public SourceLookupOpt getSourceLookupOpt() { return sourceLookupOpt; }
    public SourceLookupOpt getStoreTypes() { return storeTypes; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("storeLookupOpt", storeLookupOpt)
            .add("sourceLookupOpt", sourceLookupOpt)
            .add("storeTypes", storeTypes);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
