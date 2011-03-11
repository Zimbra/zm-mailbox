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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class StatsValues {

    @XmlAttribute(name=AdminConstants.A_T, required=true)
    private final String t;

    @XmlElement(name=AdminConstants.E_STAT, required=false)
    private List<NameAndValue> stats = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private StatsValues() {
        this((String) null);
    }

    public StatsValues(String t) {
        this.t = t;
    }

    public void setStats(Iterable <NameAndValue> stats) {
        this.stats.clear();
        if (stats != null) {
            Iterables.addAll(this.stats,stats);
        }
    }

    public StatsValues addStat(NameAndValue stat) {
        this.stats.add(stat);
        return this;
    }

    public String getT() { return t; }
    public List<NameAndValue> getStats() {
        return Collections.unmodifiableList(stats);
    }
}
