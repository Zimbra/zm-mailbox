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

import java.util.List;

import org.apache.lucene.search.TopDocs;

import com.google.common.base.Objects;

/**
 * Used for results of Zimbra Index searches.
 */
public class ZimbraTopDocs {
    private final int totalHits;
    private final float maxScore;
    private final List<ZimbraScoreDoc> scoreDocs;

    protected ZimbraTopDocs(int totalHits, List<ZimbraScoreDoc> scoreDocs, float maxScore) {
        this.totalHits = totalHits;
        this.scoreDocs = scoreDocs;
        this.maxScore = maxScore;
    }

    public static ZimbraTopDocs create(int totalHits, List<ZimbraScoreDoc> scoreDocs, float maxScore) {
        return new ZimbraTopDocs(totalHits, scoreDocs, maxScore);
    }

    /**
     * Create a ZimbraTopDocs object for search results where scores are not tracked.
     */
    public static ZimbraTopDocs create(int totalHits, List<ZimbraScoreDoc> scoreDocs) {
        return new ZimbraTopDocs(totalHits, scoreDocs, Float.NaN);
    }

    /**
     * Create equivalent ZimbraTopDocs object to a Lucene TopDocs object
     */
    public static ZimbraTopDocs create(TopDocs luceneTopDocs) {
        return new ZimbraTopDocs(luceneTopDocs.totalHits,
                ZimbraScoreDoc.listFromLuceneScoreDocs(luceneTopDocs.scoreDocs), luceneTopDocs.getMaxScore());
    }

    /**
     * Returns maximum score encountered or {@link Float#NaN} if scores are not tracked.
     */
    public float getMaxScore() {
        return maxScore;
    }

    /**
     * Returns total number of hits for the query.
     */
    public int getTotalHits() {
        return totalHits;
    }

    /**
     * Returns top hits for the query.
     */
    public List<ZimbraScoreDoc> getScoreDocs() {
        return scoreDocs;
    }

    /**
     * Returns ScoreDoc at index {@code index}
     */
    public ZimbraScoreDoc getScoreDoc(int index) {
        return scoreDocs.get(index);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("totalHits", totalHits)
                .add("maxScore", maxScore)
                .add("scoreDocs", scoreDocs).toString();
    }
}
