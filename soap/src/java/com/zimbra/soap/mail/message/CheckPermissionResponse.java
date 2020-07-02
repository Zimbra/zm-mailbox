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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

import com.zimbra.soap.mail.type.RightPermission;
import com.zimbra.soap.type.ZmBoolean;

/*
 * Delete this class in bug 66989
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CHECK_PERMISSION_RESPONSE)
public class CheckPermissionResponse {

    /**
     * @zm-api-field-tag has-right-to-all
     * @zm-api-field-description Set if the authed user has ALL the rights for each <b>&lt;right></b> element.
     * <br />
     * i.e.  It is the AND result of each individual result
     */
    @XmlAttribute(name=MailConstants.A_ALLOW /* allow */, required=true)
    private final ZmBoolean allow;

    /**
     * @zm-api-field-description Individual right information
     */
    @XmlElement(name=MailConstants.E_RIGHT /* right */, required=false)
    private List<RightPermission> rights = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CheckPermissionResponse() {
        this(false);
    }

    public CheckPermissionResponse(boolean allow) {
        this.allow = ZmBoolean.fromBool(allow);
    }

    public void setRights(Iterable <RightPermission> rights) {
        this.rights.clear();
        if (rights != null) {
            Iterables.addAll(this.rights,rights);
        }
    }

    public void addRight(RightPermission right) {
        this.rights.add(right);
    }

    public boolean getAllow() { return ZmBoolean.toBool(allow); }
    public List<RightPermission> getRights() {
        return rights;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("allow", allow)
            .add("rights", rights);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
