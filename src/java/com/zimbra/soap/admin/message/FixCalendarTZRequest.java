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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.TZFixupRule;
import com.zimbra.soap.type.NamedElement;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_FIX_CALENDAR_TZ_REQUEST)
public class FixCalendarTZRequest {

    @XmlAttribute(name=AdminConstants.A_TZFIXUP_SYNC /* sync */, required=false)
    private ZmBoolean sync;

    @XmlAttribute(name=AdminConstants.A_TZFIXUP_AFTER /* after */, required=false)
    private Long after;

    @XmlElement(name=AdminConstants.E_ACCOUNT /* account */, required=false)
    private List<NamedElement> accounts = Lists.newArrayList();

    @XmlElementWrapper(name=AdminConstants.E_TZFIXUP /* tzfixup */, required=false)
    @XmlElement(name=AdminConstants.E_FIXUP_RULE /* fixupRule */, required=false)
    private List<TZFixupRule> fixupRules = Lists.newArrayList();

    public FixCalendarTZRequest() {
    }

    public void setSync(Boolean sync) { this.sync = ZmBoolean.fromBool(sync); }
    public void setAfter(Long after) { this.after = after; }
    public void setAccounts(Iterable <NamedElement> accounts) {
        this.accounts.clear();
        if (accounts != null) {
            Iterables.addAll(this.accounts,accounts);
        }
    }

    public void addAccount(NamedElement account) {
        this.accounts.add(account);
    }

    public void setFixupRules(Iterable <TZFixupRule> fixupRules) {
        this.fixupRules.clear();
        if (fixupRules != null) {
            Iterables.addAll(this.fixupRules,fixupRules);
        }
    }

    public void addFixupRule(TZFixupRule fixupRule) {
        this.fixupRules.add(fixupRule);
    }

    public Boolean getSync() { return ZmBoolean.toBool(sync); }
    public Long getAfter() { return after; }
    public List<NamedElement> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }
    public List<TZFixupRule> getFixupRules() {
        return Collections.unmodifiableList(fixupRules);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("sync", sync)
            .add("after", after)
            .add("accounts", accounts)
            .add("fixupRules", fixupRules);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
