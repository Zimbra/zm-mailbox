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

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.SortField;

import com.google.common.base.MoreObjects;

/**
 * Used for results of Zimbra Index searches.
 */
public class ZimbraTopFieldDocs extends ZimbraTopDocs {
    private final List<SortField> sortFields;

    /**
     * @param totalHits Total number of hits for the query.
     * @param scoreDocs The top hits for the query.
     * @param fields The sort criteria used to find the top hits.
     * @param maxScore The maximum score encountered.
     */
    protected ZimbraTopFieldDocs(int totalHits, List<ZimbraScoreDoc> scoreDocs, float maxScore,
            List<SortField> sortFields) {
        super(totalHits, scoreDocs, maxScore);
        this.sortFields = sortFields;
    }

    /**
     * Create a ZimbraTopFielDocs object for search results where scores are not tracked.
     */
    public static ZimbraTopFieldDocs create(int totalHits, List<ZimbraScoreDoc> scoreDocs, List<SortField> sortFields) {
        return new ZimbraTopFieldDocs(totalHits, scoreDocs, Float.NaN, sortFields);
    }

    /**
     * Create equivalent ZimbraTopFieldDocs object to a Lucene TopFieldDocs object
     */
    public static ZimbraTopFieldDocs create(TopFieldDocs luceneTopFieldDocs) {
        return new ZimbraTopFieldDocs((int) luceneTopFieldDocs.totalHits,
                ZimbraScoreDoc.listFromLuceneScoreDocs(luceneTopFieldDocs.scoreDocs),
                luceneTopFieldDocs.getMaxScore(), Arrays.asList(luceneTopFieldDocs.fields));
    }

    /**
     * Returns the sort criteria used to find the top hits.
     */
    public List<SortField> getSortFields() {
        return sortFields;
    }

    /**
     * Returns SortField at index {@code index}
     */
    public SortField getSortField(int index) {
        return sortFields.get(index);
    }

    @Override
    public String toString() {
        return super.toString() + MoreObjects.toStringHelper(this).add("sortFields", sortFields).toString();
    }
}
