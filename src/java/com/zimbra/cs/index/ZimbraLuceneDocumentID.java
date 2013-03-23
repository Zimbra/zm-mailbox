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

import com.google.common.base.Objects;

public final class ZimbraLuceneDocumentID implements ZimbraIndexDocumentID {

    private final int luceneDocID;

    public ZimbraLuceneDocumentID(int luceneDocID) {
        this.luceneDocID = luceneDocID;
    }

    public int getLuceneDocID() {
        return luceneDocID;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("docID", luceneDocID).toString();
    }
}
