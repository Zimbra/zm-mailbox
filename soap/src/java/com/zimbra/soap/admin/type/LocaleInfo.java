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

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.base.LocaleInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class LocaleInfo
implements LocaleInterface {

    /**
     * @zm-api-field-tag locale-id
     * @zm-api-field-description Locale ID.  e.g. "en_US"
     */
    @XmlAttribute(name=AccountConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag locale-name
     * @zm-api-field-description Locale name - the name in the locale itself.  e.g. "English (United States)"
     */
    @XmlAttribute(name=AccountConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag locale-local-name
     * @zm-api-field-description Locale name in the user's locale.  e.g. "English (United States)"
     */
    @XmlAttribute(name=AccountConstants.A_LOCAL_NAME /* localName */, required=false)
    private final String localName;

    /**
     * no-argument constructor wanted by JAXB
     */
    private LocaleInfo() {
        this((String) null, (String) null, (String) null);
    }

    private LocaleInfo(String id, String name, String localName) {
        this.id = id;
        this.name = name;
        this.localName = localName;
    }

    public static LocaleInfo createForIdNameAndLocalName(String id, String name, String localName) {
        return new LocaleInfo(id, name, localName);
    }

    @Override
    public String getId() { return id; }
    @Override
    public String getName() { return name; }
    @Override
    public String getLocalName() { return localName; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("name", name)
            .add("localName", localName);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
