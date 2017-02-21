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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ZimletStatusCos {

    /**
     * @zm-api-field-tag cos-name
     * @zm-api-field-description Class Of Service (COS) name
     */
    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;

    /**
     * @zm-api-field-description Information on zimlet status
     */
    @XmlElement(name=AdminConstants.E_ZIMLET, required=false)
    private List<ZimletStatus> zimlets = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ZimletStatusCos() {
        this((String) null);
    }

    public ZimletStatusCos(String name) {
        this.name = name;
    }

    public void setZimlets(Iterable <ZimletStatus> zimlets) {
        this.zimlets.clear();
        if (zimlets != null) {
            Iterables.addAll(this.zimlets,zimlets);
        }
    }

    public ZimletStatusCos addZimlet(ZimletStatus zimlet) {
        this.zimlets.add(zimlet);
        return this;
    }

    public String getName() { return name; }
    public List<ZimletStatus> getZimlets() {
        return Collections.unmodifiableList(zimlets);
    }
}
