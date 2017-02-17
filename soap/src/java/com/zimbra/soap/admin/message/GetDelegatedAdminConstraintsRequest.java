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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.TargetType;
import com.zimbra.soap.type.NamedElement;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get constraints (zimbraConstraint) for delegated admin on global config or a COS
 * <br />
 * none or several attributes can be specified for which constraints are to be returned.
 * <br />
 * If no attribute is specified, all constraints on the global config/cos will be returned.
 * <br />
 * If there is no constraint for a requested attribute, <b>&lt;a></b> element for the attribute will not appear in
 * the response.
 * <br />
 * <br />
 * e.g.
 * <pre>
 *     &lt;GetDelegatedAdminConstraintsRequest type="cos" name="cos1">
 *       &lt;a name="zimbraMailQuota">
 *     &lt;/GetDelegatedAdminConstraintsRequest>
 *
 *     &lt;GetDelegatedAdminConstraintsResponse type="cos" id="e00428a1-0c00-11d9-836a-000d93afea2a" name="cos1">
 *       &lt;a n="zimbraMailQuota">
 *         &lt;constraint>
 *           &lt;max>524288000&lt;/max>
 *           &lt;min>20971520&lt;/min>
 *         &lt;/constraint>
 *       &lt;/a>
 *     &lt;/GetDelegatedAdminConstraintsResponse>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_DELEGATED_ADMIN_CONSTRAINTS_REQUEST)
public class GetDelegatedAdminConstraintsRequest {

    /**
     * @zm-api-field-description Target Type
     */
    @XmlAttribute(name=AdminConstants.A_TYPE, required=true)
    private final TargetType type;

    /**
     * @zm-api-field-description ID
     */
    @XmlAttribute(name=AdminConstants.A_ID, required=false)
    private final String id;

    /**
     * @zm-api-field-description name
     */
    @XmlAttribute(name=AdminConstants.A_NAME, required=false)
    private final String name;

    /**
     * @zm-api-field-description Attrs
     */
    @XmlElement(name=AdminConstants.E_A, required=false)
    private List<NamedElement> attrs = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetDelegatedAdminConstraintsRequest() {
        this((TargetType) null, (String) null, (String) null);
    }

    public GetDelegatedAdminConstraintsRequest(
                TargetType type, String id, String name) {
        this.type = type;
        this.id = id;
        this.name = name;
    }

    public void setAttrs(Iterable <NamedElement> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs,attrs);
        }
    }

    public GetDelegatedAdminConstraintsRequest addAttr(NamedElement attr) {
        this.attrs.add(attr);
        return this;
    }

    public TargetType getType() { return type; }
    public String getId() { return id; }
    public String getName() { return name; }
    public List<NamedElement> getAttrs() {
        return Collections.unmodifiableList(attrs);
    }
}
