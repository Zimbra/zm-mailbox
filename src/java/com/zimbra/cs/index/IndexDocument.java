/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;

public class IndexDocument {
    private Object mWrappedDocument;

    public IndexDocument(Object wrapped) {
        mWrappedDocument = wrapped;
    }

    public Object getWrappedDocument() {
        return mWrappedDocument;
    }

    public void dump(StringBuilder sb) {
        Object wd = getWrappedDocument();
        if (wd instanceof Document) {
            Document luceneDoc = (Document) wd;
            sb.append("Lucene doc \n");

            for (Fieldable field : luceneDoc.getFields()) {
                String name = field.name();
                String value = field.stringValue();
                sb.append("    Field " + name + ": " + value + "\n");
            }
        }
    }
}
