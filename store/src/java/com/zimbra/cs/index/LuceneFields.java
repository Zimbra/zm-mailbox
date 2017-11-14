/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.zimbra.cs.mailbox.Flag;

/**
 * Standard Lucene fields.
 *
 * The sort fields are used for the sorting of Lucene results. They MUST be stored with all documents, and their values
 * MUST match the values in the corresponding MailItem row entry. They MUST be Indexed and NOT_ANALYZED.
 * Snippet from Javadoc for Lucene 3.5.0 org.apache.lucene.search.SortField:
 *     Fields must be indexed in order to sort by them.
 *
 * @since Apr 30, 2004
 * @author schemers
 */
public final class LuceneFields {
    /* Map to find Compact key to associate with indexed/tokenized terms */
    public static final Map<String, Character> FIELD2PREFIX = ImmutableMap.<String, Character>builder()
        .put(LuceneFields.L_CONTENT, 'A')
        .put(LuceneFields.L_CONTACT_DATA, 'B')
        .put(LuceneFields.L_MIMETYPE, 'C')
        .put(LuceneFields.L_ATTACHMENTS, 'D')
        .put(LuceneFields.L_FILENAME, 'E')
        .put(LuceneFields.L_OBJECTS, 'F')
        .put(LuceneFields.L_H_FROM, 'G')
        .put(LuceneFields.L_H_TO, 'H')
        .put(LuceneFields.L_H_CC, 'I')
        .put(LuceneFields.L_H_X_ENV_FROM, 'J')
        .put(LuceneFields.L_H_X_ENV_TO, 'K')
        .put(LuceneFields.L_H_MESSAGE_ID, 'L')
        .put(LuceneFields.L_H_SUBJECT, 'M')
        .put(LuceneFields.L_FIELD, 'N')
        .put(LuceneFields.L_SORT_DATE, 'O')
        .put(LuceneFields.L_PARTNAME, 'P')
        .put(LuceneFields.L_MAILBOX_BLOB_ID, 'Q')
        .put(LuceneFields.L_SORT_ATTACH, 'R')
        .put(LuceneFields.L_SORT_FLAG, 'S')
        .put(LuceneFields.L_SORT_PRIORITY, 'T')
        .put(LuceneFields.L_SORT_SIZE, 'U')
        .build();

    public static final String ITEM_ID_PREFIX = "x"; // term prefix for ItemID

    private LuceneFields() {
    }

    /**
     * Subject for sorting.
     */
    public static final String L_SORT_SUBJECT = "subjSort";

    /**
     * Name for sorting.
     */
    public static final String L_SORT_NAME = "nameSort";

    /**
     * Date for sorting. ALSO searchable date-  "date:"  "after:" and "before:".
     */
    public static final String L_SORT_DATE = "l.date";

    /**
     * Size of document "size:" searches, "larger:" and "smaller:" for sorting.
     */
    public static final String L_SORT_SIZE = "l.size";

    /**
     * Values "0" or "1".  "1" if has an attachment
     */
    public static final String L_SORT_ATTACH = "hasAttach";

    /**
     * Values "0" or "1".  "1" if has been flagged
     */
    public static final String L_SORT_FLAG = "hasFlag";

    /**
     * Priority setting
     */
    public static final String L_SORT_PRIORITY = "priority";

    /**
     * The "index id" this document -- maps to one or more rows in the DB's mail_item table (index_id column).
     * <p>
     * This field will automatically be populated with the index ID
     */
    public static final String L_MAILBOX_BLOB_ID = "l.mbox_blob_id";


    /**
     * unique set of all attachment content types, or "none" if no attachments.
     * <p>
     * "attachment:"  searches
     */
    public static final String L_ATTACHMENTS = "attachment";

    public static final String L_ATTACHMENT_NONE = "none";
    public static final String L_ATTACHMENT_ANY = "any";

    /**
     * the "content" of the message/attachment/etc.
     * <p>
     * searches with no operator will search this field
     */
    public static final String L_CONTENT = "l.content";

    /**
     * list of objects  "has:" searches
     */
    public static final String L_OBJECTS = "has";

    /**
     * MIME-type of blob  "type:" searches
     */
    public static final String L_MIMETYPE = "type";

    public static final String L_VERSION = "l.version";

    /**
     * Easily Searchable Contact Data (bug 11831) "contact:" searches.
     */
    public static final String L_CONTACT_DATA= "l.contactData";

    /**
     * Partname identifier for multipart/mime messages.
     *
     * For RFC/822 messages, should be dotted-number MIME part name or "top". For all other mail_item types, should be
     * "top", or should be specific to the type note that the partname is used in a weird way when constructing "NOT"
     * queries -- bare 'not's are only checked against toplevel parts....see LuceneQueryOperation.java AddClause().
     */
    public static final String L_PARTNAME = "l.partname";

    public static final String L_PARTNAME_CONTACT = "CONTACT";
    public static final String L_PARTNAME_NOTE= "NOTE";
    public static final String L_PARTNAME_NONE= "none";
    public static final String L_PARTNAME_TOP= "top";

    /**
     * "filename:" searches.
     */
    public static final String L_FILENAME = "filename";

    /**
     * "from:" searches.
     */
    public static final String L_H_FROM = "from";

    /**
     * "to:" searches.
     */
    public static final String L_H_TO = "to";

    /**
     * "cc:" searches.
     */
    public static final String L_H_CC = "cc";

    /**
     * "subject:" searches.
     */
    public static final String L_H_SUBJECT = "subject";

    /**
     * x-envelope-from / to, see bug 8703.
     */
    public static final String L_H_X_ENV_FROM = "env_from";

    public static final String L_H_X_ENV_TO = "env_to";

    public static final String L_H_MESSAGE_ID = "msg_id";

    /**
     * Exact text of search history entries; used for exact and edge matching
     */
    public static final String L_SEARCH_EXACT = "sh_exact";

    /**
     * word tokens generated from search history entries
     */
    public static final String L_SEARCH_TERMS = "sh_terms";

    /**
     * ID of search history entries
     */
    public static final String L_SEARCH_ID = "search_id";

    /**
     * Item type; used to differentiate search history entries from mail items
     */
    public static final String L_ITEM_TYPE = "doctype";

    /**
     * field operator: structured data storage
     */
    public static final String L_FIELD = "l.field";

    /**
     * account id; used when events are stored in a joint index
     */
    public static final String L_ACCOUNT_ID = "acct_id";

    /**
     * datasource id; used for differentiating events
     */
    public static final String L_DATASOURCE_ID = "datasource_id";


    public static String valueForBooleanField(boolean value) {
        return value ? "1" : "0";
    }

    public static String valueForPriority(int flags) {
        if ((flags & Flag.BITMASK_HIGH_PRIORITY) != 0) {
            return "2";
        } else if ((flags & Flag.BITMASK_LOW_PRIORITY) != 0) {
            return "0";
        }
        return "1";
    }

}
