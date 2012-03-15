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

package com.zimbra.soap.admin.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.SyncGalAccountSpec;

/**
 * @zm-api-command-description Sync GalAccount
 * <br />
 * Notes:
 * <ul>
 * <li> If fullSync is set to false (or unset) the default behavior is trickle sync which will pull in any new
 *      contacts or modified contacts since last sync.
 * <li> If fullSync is set to true, then the server will go through all the contacts that appear in GAL, and resolve
 *      deleted contacts in addition to new or modified ones.
 * <li> If reset attribute is set, then all the contacts will be populated again, regardless of the status since last
 *      sync.  Reset needs to be done when there is a significant change in the configuration, such as filter,
 *      attribute map, or search base.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_SYNC_GAL_ACCOUNT_REQUEST)
public class SyncGalAccountRequest {

    /**
     * @zm-api-field-description Sync GalAccount specification
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT /* account */, required=false)
    private List<SyncGalAccountSpec> accounts = Lists.newArrayList();

    public SyncGalAccountRequest() {
    }

    public void setAccounts(Iterable <SyncGalAccountSpec> accounts) {
        this.accounts.clear();
        if (accounts != null) {
            Iterables.addAll(this.accounts,accounts);
        }
    }

    public void addAccount(SyncGalAccountSpec account) {
        this.accounts.add(account);
    }

    public List<SyncGalAccountSpec> getAccounts() {
        return accounts;
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("accounts", accounts);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
