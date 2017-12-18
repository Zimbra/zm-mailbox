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
package com.zimbra.cs.index;

import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;

import com.google.common.base.MoreObjects;
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
        return MoreObjects.toStringHelper(this).add("documentID", documentID).add("score", score).toString();
    }
}
