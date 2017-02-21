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

package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_COS)
@XmlType(propOrder = {})
public class DLInfo extends ObjectInfo {

    /**
     * @zm-api-field-tag dl-ldap-dn
     * @zm-api-field-description ldap dn of the DL.
     */
    @XmlAttribute(name=AccountConstants.A_REF, required=true)
    private final String ref;

    /**
     * @zm-api-field-tag dl-display-name
     * @zm-api-field-description Display name of group
     */
    @XmlAttribute(name=AccountConstants.A_DISPLAY /* d */, required=false)
    private final String displayName;

    /**
     * @zm-api-field-tag dl-is-dynamic
     * @zm-api-field-description Flags whether the group is dynamic or not
     */
    @XmlAttribute(name=AccountConstants.A_DYNAMIC /* dynamic */, required=false)
    ZmBoolean dynamic;

    /**
     * @zm-api-field-tag via-dl-name
     * @zm-api-field-description <b>{via-dl-name}</b> = is present if the account is a member of the returned list
     * because they are either a direct or indirect member of another list that is a member of the returned list.
     * For example, if a user is a member of engineering@domain.com, and engineering@domain.com is a member of
     * all@domain.com, then the following would be returned:
     * <pre>
     *     &lt;dl name="all@domain.com" ... via="engineering@domain.com"/>
     * </pre>
     */
    @XmlAttribute(name=AccountConstants.A_VIA /* via */, required=false)
    private final String via;

    /**
     * @zm-api-field-tag isOwner
     * @zm-api-field-description Flags whether user is the owner of the group.
     * <br />
     * Only returned if <b>ownerOf</b> on the request is <b>1 (true)</b>
     */
    @XmlAttribute(name=AccountConstants.A_IS_OWNER /* isOwner */, required=false)
    ZmBoolean isOwner;

    /**
     * @zm-api-field-tag isMember
     * @zm-api-field-description Flags whether user is a member of the group.
     * <br />
     * Only returned if <b>memberOf</b> on the request is <b>1 (true)</b>
     */
    @XmlAttribute(name=AccountConstants.A_IS_MEMBER /* isMember */, required=false)
    ZmBoolean isMember;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DLInfo() {
        this(null, null, null, null, null, null, null, null);
    }

    public DLInfo(String id, String ref, String name, String displayName, Boolean dynamic, String via,
            Boolean isOwner, Boolean isMember) {
        super(id, name, null);
        this.ref = ref;
        this.displayName = displayName;
        this.dynamic = ZmBoolean.fromBool(dynamic);
        this.via = via;
        this.isOwner = ZmBoolean.fromBool(isOwner);
        this.isMember = ZmBoolean.fromBool(isMember);
    }

    public String getRef() {
        return ref;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getVia() {
        return via;
    }

    public Boolean isDynamic() {
        return ZmBoolean.toBool(dynamic, false);
    }

    public Boolean isOwner() {
        return ZmBoolean.toBool(isOwner);
    }

    public Boolean isMember() {
        return ZmBoolean.toBool(isMember);
    }
}
