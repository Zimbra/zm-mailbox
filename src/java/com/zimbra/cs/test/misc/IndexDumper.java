/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.test.misc;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermEnum;

import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.ZimbraFSDirectory;


/**
 * @since  May 3, 2004
 * @author schemers
 */
public class IndexDumper {

    public static void main(String[] args) throws IOException {
        IndexReader reader = IndexReader.open(
                new ZimbraFSDirectory(new File("/tmp/zimbra/index")));
        TermEnum terms = reader.terms();
        while (terms.next()) {
            if (terms.term().field().equals(LuceneFields.L_MAILBOX_BLOB_ID)) {
                System.out.println(terms.term());
            }
            //String field = terms.term().field();

        }
    }
}
