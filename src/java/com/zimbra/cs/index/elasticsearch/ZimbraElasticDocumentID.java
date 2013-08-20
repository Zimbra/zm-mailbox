/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index.elasticsearch;

import com.google.common.base.Objects;
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
        return Objects.toStringHelper(this).add("docID", docID).toString();
    }

    @Override
    public int compareTo(ZimbraElasticDocumentID o) {
        return docID.compareTo(o.getDocID());
    }
}
