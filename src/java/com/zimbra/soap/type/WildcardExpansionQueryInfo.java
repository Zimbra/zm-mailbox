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

package com.zimbra.soap.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.soap.type.BaseQueryInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class WildcardExpansionQueryInfo implements BaseQueryInfo {

    /**
     * @zm-api-field-tag wildcard-str
     * @zm-api-field-description Wildcard expansion string
     */
    @XmlAttribute(name="str", required=true)
    private final String str;

    /**
     * @zm-api-field-tag wildcard-expanded
     * @zm-api-field-description If value is <b>1 (true)</b>, then the wildcard was expanded and the
     * matches are included in the search.  If value is <b>0 (false)</b> then the wildcard was not specific enough and
     * therefore no wildcard matches are included (exact-match *is* included in results).
     */
    @XmlAttribute(name="expanded", required=true)
    private final ZmBoolean expanded;

    /**
     * @zm-api-field-tag wildcard-num-expanded
     * @zm-api-field-description Number expanded
     */
    @XmlAttribute(name="numExpanded", required=true)
    private final int numExpanded;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private WildcardExpansionQueryInfo() {
        this((String) null, false, -1);
    }

    public WildcardExpansionQueryInfo(String str, boolean expanded,
                            int numExpanded) {
        this.str = str;
        this.expanded = ZmBoolean.fromBool(expanded);
        this.numExpanded = numExpanded;
    }

    public String getStr() { return str; }
    public boolean getExpanded() { return ZmBoolean.toBool(expanded); }
    public int getNumExpanded() { return numExpanded; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("str", str)
            .add("expanded", expanded)
            .add("numExpanded", numExpanded);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
