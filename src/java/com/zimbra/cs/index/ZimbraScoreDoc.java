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
package com.zimbra.cs.index;

import java.util.List;

import org.apache.lucene.search.ScoreDoc;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * Used for results of Zimbra Index searches.
 */
public final class ZimbraScoreDoc {
    private final ZimbraIndexDocumentID documentID;
    private final float score;

    private ZimbraScoreDoc(ZimbraIndexDocumentID documentID, float score) {
        this.documentID = documentID;
        this.score = score;
    }

    public static ZimbraScoreDoc create(ZimbraIndexDocumentID documentID, float score) {
        return new ZimbraScoreDoc(documentID, score);
    }

    /**
     * Used when scores are not being used
     */
    public static ZimbraScoreDoc create(ZimbraIndexDocumentID documentID) {
        return new ZimbraScoreDoc(documentID, Float.NaN);
    }

    /**
     * Create equivalent ZimbraScoreDoc object to a Lucene ScoreDoc object
     */
    public static ZimbraScoreDoc create(ScoreDoc luceneScoreDoc) {
        return new ZimbraScoreDoc(new ZimbraLuceneDocumentID(luceneScoreDoc.doc), luceneScoreDoc.score);
    }

    /**
     * Returns the index store's ID for this document
     */
    public ZimbraIndexDocumentID getDocumentID() {
        return documentID;
    }

    /**
     * Returns the score of this document for the query
     */
    public float getScore() {
        return score;
    }

    public static List<ZimbraScoreDoc> listFromLuceneScoreDocs(ScoreDoc[] luceneScoreDocs) {
        if (luceneScoreDocs == null) {
            return Lists.newArrayListWithCapacity(0);
        }
        List<ZimbraScoreDoc> docs = Lists.newArrayListWithCapacity(luceneScoreDocs.length);
        for (ScoreDoc luceneDoc : luceneScoreDocs) {
            docs.add(ZimbraScoreDoc.create(luceneDoc));
        }
        return docs;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("documentID", documentID).add("score", score).toString();
    }
}
