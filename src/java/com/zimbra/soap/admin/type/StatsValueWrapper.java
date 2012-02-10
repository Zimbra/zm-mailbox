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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.NamedElement;

// XmlRootElement is needed for classes referenced via @XmlElementRef
@XmlRootElement(name=AdminConstants.E_VALUES)
@XmlAccessorType(XmlAccessType.NONE)
public class StatsValueWrapper {

    /**
     * @zm-api-field-description Stats specification
     */
    @XmlElement(name=AdminConstants.E_STAT /* stat */, required=false)
    private List<NamedElement> stats = Lists.newArrayList();

    public StatsValueWrapper() {
    }

    public void setStats(Iterable <NamedElement> stats) {
        this.stats.clear();
        if (stats != null) {
            Iterables.addAll(this.stats, stats);
        }
    }

    public StatsValueWrapper addStat(NamedElement stat) {
        this.stats.add(stat);
        return this;
    }

    public List<NamedElement> getStats() {
        return Collections.unmodifiableList(stats);
    }
}
