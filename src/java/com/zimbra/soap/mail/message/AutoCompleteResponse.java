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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.AutoCompleteMatch;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_AUTO_COMPLETE_RESPONSE)
@XmlType(propOrder = {})
public class AutoCompleteResponse {

    @XmlAttribute(name=MailConstants.A_CANBECACHED /* canBeCached */, required=false)
    private ZmBoolean canBeCached;

    @XmlElement(name=MailConstants.E_MATCH /* match */, required=false)
    private List<AutoCompleteMatch> matches = Lists.newArrayList();

    public AutoCompleteResponse() {
    }

    public void setCanBeCached(Boolean canBeCached) {
        this.canBeCached = ZmBoolean.fromBool(canBeCached);
    }
    public void setMatches(Iterable <AutoCompleteMatch> matches) {
        this.matches.clear();
        if (matches != null) {
            Iterables.addAll(this.matches,matches);
        }
    }

    public AutoCompleteResponse addMatche(AutoCompleteMatch matche) {
        this.matches.add(matche);
        return this;
    }

    public Boolean getCanBeCached() { return ZmBoolean.toBool(canBeCached); }
    public List<AutoCompleteMatch> getMatches() {
        return Collections.unmodifiableList(matches);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("canBeCached", canBeCached)
            .add("matches", matches);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
