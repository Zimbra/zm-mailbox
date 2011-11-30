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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.TargetSpec;


/*
 * Delete this class in bug 66989
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_CHECK_PERMISSION_REQUEST)
public class CheckPermissionRequest {

    @XmlElement(name=MailConstants.E_TARGET /* target */, required=false)
    private List<TargetSpec> targets = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_RIGHT /* right */, required=false)
    private List<String> rights = Lists.newArrayList();

    public CheckPermissionRequest() {
    }

    public void setTargets(Iterable <TargetSpec> targets) {
        this.targets.clear();
        if (targets != null) {
            Iterables.addAll(this.targets,targets);
        }
    }

    public CheckPermissionRequest addTarget(TargetSpec target) {
        this.targets.add(target);
        return this;
    }

    public void setRights(Iterable <String> rights) {
        this.rights.clear();
        if (rights != null) {
            Iterables.addAll(this.rights,rights);
        }
    }

    public CheckPermissionRequest addRight(String right) {
        this.rights.add(right);
        return this;
    }

    public List<TargetSpec> getTargets() {
        return Collections.unmodifiableList(targets);
    }
    public List<String> getRights() {
        return Collections.unmodifiableList(rights);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("targets", targets)
            .add("rights", rights);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
