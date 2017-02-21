/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class IndexStats {
    /**
     * @zm-api-field-tag max-docs
     * @zm-api-field-description total number of docs in this index
     */
    @XmlAttribute(name=AdminConstants.A_MAX_DOCS /* maxDocs */, required=true)
    private final int maxDocs;

    /**
     * @zm-api-field-tag deleted-docs
     * @zm-api-field-description number of deleted docs for the index
     */
    @XmlAttribute(name=AdminConstants.A_DELETED_DOCS /* totalSize */, required=true)
    private final int numDeletedDocs;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private IndexStats() {
        this(0, 0);
    }

    public IndexStats(int maxDocs, int numDeletedDocs) {
        this.maxDocs = maxDocs;
        this.numDeletedDocs = numDeletedDocs;
    }

    public int getMaxDocs() { return maxDocs; }
    public int getNumDeletedDocs() { return numDeletedDocs; }

}
