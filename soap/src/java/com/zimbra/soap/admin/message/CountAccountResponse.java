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

package com.zimbra.soap.admin.message;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.Lists;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CosCountInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_COUNT_ACCOUNT_RESPONSE)
@XmlType(propOrder = {})
public class CountAccountResponse {

    /**
     * @zm-api-field-description Account count information by Class Of Service (COS)
     */
    @XmlElement(name=AdminConstants.E_COS, required=false)
    private List <CosCountInfo> cos = Lists.newArrayList();

    public CountAccountResponse() {
    }

    public CountAccountResponse setCos(Collection<CosCountInfo> cos) {
        this.cos.clear();
        if (cos != null) {
            this.cos.addAll(cos);
        }
        return this;
    }

    public CountAccountResponse addCos(CosCountInfo cos) {
        this.cos.add(cos);
        return this;
    }

    public List <CosCountInfo> getCos() {
        return Collections.unmodifiableList(cos);
    }
}
