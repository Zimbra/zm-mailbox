/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SingleInstanceLockFactory;

public class RawIndexEditor {

    private FSDirectory mIdxDirectory = null;

    RawIndexEditor(String idxPath) throws IOException {
        mIdxDirectory = new Z23FSDirectory(new File(idxPath));
        if (mIdxDirectory.getLockFactory() == null ||
                !(mIdxDirectory.getLockFactory() instanceof SingleInstanceLockFactory)) {
            mIdxDirectory.setLockFactory(new SingleInstanceLockFactory());
        }
    }

    public static String Format(String s, int len) {
        StringBuffer toRet = new StringBuffer(len+1);
        int curOff = 0;
        if (s.length() < len) {
            for (curOff = 0; curOff < (len-s.length()); curOff++) {
                toRet.append(" ");
            }
        }

        int sOff = 0;
        for (; curOff < len; curOff++) {
            toRet.append(s.charAt(sOff));
            sOff++;
        }
        toRet.append("  ");
        return toRet.toString();
    }

    public boolean dumpDocument(Document d, boolean isDeleted) {
        if (isDeleted) {
            System.out.print("DELETED ");
        }
        String subj, blobId;
        Field f;
        f = d.getField(LuceneFields.L_H_SUBJECT);
        if (f != null) {
            subj = f.stringValue();
        } else {
            subj = "MISSING_SUBJECT";
        }
        f = d.getField(LuceneFields.L_MAILBOX_BLOB_ID);
        if (f != null) {
            blobId = f.stringValue();
        } else {
            blobId = "MISSING";
        }
        String part = d.get(LuceneFields.L_PARTNAME);
        if (part == null) {
            part = "NULL_PART";
        }

        String dateStr = d.get(LuceneFields.L_SORT_DATE);
        if (dateStr == null) {
            dateStr = "";
        } else {
            try {
                dateStr = DateTools.stringToDate(dateStr).toString();
            } catch (ParseException e) {
                assert false;
            }
        }
        String sizeStr = d.get(LuceneFields.L_SORT_SIZE);
        if (sizeStr == null) {
            sizeStr = "";
        }

        System.out.println(Format(blobId, 10) + Format(dateStr, 30) +
                Format(part, 10) + Format(sizeStr, 10) + "\"" + subj + "\"");

        return true;
    }


    void dumpAll() throws IOException {
        IndexReader reader = IndexReader.open(mIdxDirectory);
        try {
            int maxDoc = reader.maxDoc();
            System.out.println("There are "+maxDoc+" documents in this index.");

            for (int i = 0; i < maxDoc; i++) {
                dumpDocument(reader.document(i), reader.isDeleted(i));
            }
        } finally {
            reader.close();
        }
    }

    void run() throws IOException {
        dumpAll();
    }

    public static void main(String[] args) {
        String idxParentDir = args[0];

        try {
            RawIndexEditor editor = new RawIndexEditor(idxParentDir);
            editor.run();
        } catch (IOException e) {
            System.err.println("Caught IOException "+e);
        }

    }

}
