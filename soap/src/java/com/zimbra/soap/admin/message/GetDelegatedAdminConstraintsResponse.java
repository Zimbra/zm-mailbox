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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.admin.type.ConstraintAttr;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_DELEGATED_ADMIN_CONSTRAINTS_RESPONSE)
public class GetDelegatedAdminConstraintsResponse {

    /**
     * @zm-api-field-description Constraint attributes
     */
    @XmlElement(name=AdminConstants.E_A, required=false)
    private List<ConstraintAttr> attrs = Lists.newArrayList();

    public GetDelegatedAdminConstraintsResponse() {
    }

    public void setAttrs(Iterable <ConstraintAttr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs,attrs);
        }
    }

    public GetDelegatedAdminConstraintsResponse addAttr(ConstraintAttr attr) {
        this.attrs.add(attr);
        ZimbraLog.mailbox.info("----------- >>>>>>>>>"+  attr.toString());
        return this;
    }

    public List<ConstraintAttr> getAttrs() {
        return Collections.unmodifiableList(attrs);
    }
}
