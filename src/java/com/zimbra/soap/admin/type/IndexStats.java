/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
