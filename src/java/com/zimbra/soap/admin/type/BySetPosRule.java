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
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.BySetPosRuleInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class BySetPosRule implements BySetPosRuleInterface {

    /**
     * @zm-api-field-tag bysetpos-list
     * @zm-api-field-description Format <b>[[+]|-]num[,...]</b> where num is from 1 to 366
     * <br />
     * <b>&lt;bysetpos></b> MUST only be used in conjunction with another <b>&lt;byXXX></b> element.
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_BYSETPOS_POSLIST /* poslist */, required=true)
    private final String list;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BySetPosRule() {
        this((String) null);
    }

    public BySetPosRule(String list) {
        this.list = list;
    }

    @Override
    public BySetPosRuleInterface create(String list) {
        return new BySetPosRule(list);
    }

    @Override
    public String getList() { return list; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("list", list)
            .toString();
    }
}
