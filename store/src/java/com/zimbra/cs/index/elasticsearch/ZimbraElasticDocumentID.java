/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.index.elasticsearch;

import com.google.common.base.MoreObjects;
import com.zimbra.cs.index.ZimbraIndexDocumentID;

public final class ZimbraElasticDocumentID
implements Comparable <ZimbraElasticDocumentID>, ZimbraIndexDocumentID {

    private final String docID;

    public ZimbraElasticDocumentID(String docID) {
        this.docID = docID;
    }

    public String getDocID() {
        return docID;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("docID", docID).toString();
    }

    @Override
    public int compareTo(ZimbraElasticDocumentID o) {
        return docID.compareTo(o.getDocID());
    }
}
