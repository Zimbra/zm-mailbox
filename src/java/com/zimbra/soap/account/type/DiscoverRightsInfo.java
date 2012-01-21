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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class DiscoverRightsInfo {

    /**
     * @zm-api-field-tag targets-right
     * @zm-api-field-description Right the targets relate to
     */
    @XmlAttribute(name=AccountConstants.A_RIGHT /* right */, required=true)
    private String right;

    /**
     * @zm-api-field-description Targets
     */
    @XmlElement(name=AccountConstants.E_TARGET /* target */, required=true)
    private List<DiscoverRightsTarget> targets = Lists.newArrayList();

    public DiscoverRightsInfo() {
        this(null);
    }

    public DiscoverRightsInfo(String right) {
        this(right, null);
    }

    public DiscoverRightsInfo(String right, Iterable<DiscoverRightsTarget> targets) {
        setRight(right);
        if (targets != null) {
            setTargets(targets);
        }
    }

    public void setRight(String right) {
        this.right = right;
    }

    public void setTargets(Iterable<DiscoverRightsTarget> targets) {
        this.targets = Lists.newArrayList(targets);
    }

    public void addTarget(DiscoverRightsTarget target) {
        targets.add(target);
    }

    public String getRight() {
        return right;
    }

    public List<DiscoverRightsTarget> getTargets() {
        return targets;
    }


}
