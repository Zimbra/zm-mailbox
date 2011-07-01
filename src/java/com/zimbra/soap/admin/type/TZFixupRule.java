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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class TZFixupRule {

    @XmlElement(name=AdminConstants.E_MATCH /* match */, required=true)
    private TZFixupRuleMatch match;

    // Need either "touch" or "replace" but not both

    // Force sync clients to refetch
    @XmlElement(name=AdminConstants.E_TOUCH /* touch */, required=false)
    private SimpleElement touch;

    // replace any matching timezone with this timezone
    @XmlElement(name=AdminConstants.E_REPLACE /* replace */, required=false)
    private TZReplaceInfo replace;

    public TZFixupRule() {
    }

    public void setMatch(TZFixupRuleMatch match) { this.match = match; }
    public void setTouch(SimpleElement touch) { this.touch = touch; }
    public void setReplace(TZReplaceInfo replace) { this.replace = replace; }
    public TZFixupRuleMatch getMatch() { return match; }
    public SimpleElement getTouch() { return touch; }
    public TZReplaceInfo getReplace() { return replace; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("match", match)
            .add("touch", touch)
            .add("replace", replace);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
