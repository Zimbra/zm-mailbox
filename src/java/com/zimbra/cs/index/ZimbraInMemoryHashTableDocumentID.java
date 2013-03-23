/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 VMware, Inc.
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
package com.zimbra.cs.index;

import java.util.UUID;

import com.google.common.base.Objects;

public final class ZimbraInMemoryHashTableDocumentID
implements Comparable <ZimbraInMemoryHashTableDocumentID>, ZimbraIndexDocumentID {

    private final UUID docID;

    public ZimbraInMemoryHashTableDocumentID(UUID docID) {
        this.docID = docID;
    }

    public UUID getDocID() {
        return docID;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("docID", docID).toString();
    }

    @Override
    public int compareTo(ZimbraInMemoryHashTableDocumentID o) {
        return docID.compareTo(o.getDocID());
    }
}
