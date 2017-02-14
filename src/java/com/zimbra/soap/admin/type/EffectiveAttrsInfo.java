/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.type;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.EffectiveAttrInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class EffectiveAttrsInfo {

    /**
     * @zm-api-field-tag all-attrs-accessible
     * @zm-api-field-description Flags whether all attributes on the target entry are accessible.
     * <br />
     * if set, no <b>&lt;a></b> elements will appear under the <b>&lt;setAttrs>/&lt;getAttrs></b>
     */
    @XmlAttribute(name=AdminConstants.A_ALL /* all */, required=false)
    private final ZmBoolean all;

    /**
     * @zm-api-field-description Attributes
     */
    @XmlElement(name=AdminConstants.E_A /* a */, required=false)
    private List <EffectiveAttrInfo> attrs = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private EffectiveAttrsInfo() {
        this((Boolean) null);
    }

    private EffectiveAttrsInfo(Boolean all) {
        this.all = ZmBoolean.fromBool(all);
    }

    public EffectiveAttrsInfo setAttrs(Collection <EffectiveAttrInfo> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            this.attrs.addAll(attrs);
        }
        return this;
    }

    public EffectiveAttrsInfo addAttr(EffectiveAttrInfo attr) {
        attrs.add(attr);
        return this;
    }

    public List <EffectiveAttrInfo> getAttrs() {
        return Collections.unmodifiableList(attrs);
    }

    public Boolean getAll() { return ZmBoolean.toBool(all); }
}
