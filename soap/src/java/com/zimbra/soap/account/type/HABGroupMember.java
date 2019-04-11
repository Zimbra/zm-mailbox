/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraKeyValuePairs;
import com.zimbra.soap.type.NamedValue;

@XmlAccessorType(XmlAccessType.NONE)
public class HABGroupMember extends HABMember {

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    public HABGroupMember() {
        this((String) null);
    }

    public HABGroupMember(String name) {
        super(name);
    }

    /**
     * @zm-api-field-description Member attributes. Currently only these attributes are returned:
     * <table>
     * <tr><td> <b>zimbraId</b>       </td><td> the unique UUID of the hab member </td></tr>
     * <tr><td> <b>displayName</b>    </td><td> display name for the member </td></tr>
     * </table>
     */
    @ZimbraKeyValuePairs
    @XmlElement(name=AccountConstants.E_ATTR /* attr */, required=true)
    private List<NamedValue> attrs = Lists.newArrayList();

    public List<NamedValue> getAttrs() {
        return attrs;
    }

    public void setAttrs(Iterable <NamedValue> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs,attrs);
        }
    }

    public HABGroupMember addAttr(NamedValue attr) {
        this.attrs.add(attr);
        return this;
    }

    public MoreObjects.ToStringHelper addToStringInfo(
            MoreObjects.ToStringHelper helper) {
    super.addToStringInfo(helper);
    return helper
        .add("attrs", attrs.toString());
}

}
