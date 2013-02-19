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
package com.zimbra.cs.index;

import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;

/**
 * Modeled on a subset of {@link org.apache.lucene.index.IndexReader}
 */
public interface ZimbraIndexReader extends Closeable, Cloneable {
    /**
     * Returns the number of documents in this index.
     */
    public int numDocs();

    /**
     * Number of documents marked for deletion but not yet fully removed from the index
     * @return number of deleted documents for this index
     */
    public int numDeletedDocs();

    /**
     * Returns an enumeration of all terms starting at a given term. If the given term does not exist, the enumeration
     * is positioned at the first term greater than the supplied term. The enumeration is ordered by Term.compareTo().
     * Each term is greater than all that precede it in the enumeration.
     */
    public TermEnum terms(Term t) throws IOException;
}
