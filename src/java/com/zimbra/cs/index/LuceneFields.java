/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013 Zimbra Software, LLC.
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

import com.zimbra.cs.mailbox.Flag;
import java.util.Map;

import org.apache.lucene.document.Field;

import com.google.common.collect.ImmutableMap;

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
     * field operator: structured data storage
     */
    public static final String L_FIELD = "l.field";

    public enum IndexField {
        MIMETYPE(L_MIMETYPE, Field.Store.YES, Field.Index.ANALYZED),
        PARTNAME(L_PARTNAME, Field.Store.YES, Field.Index.NOT_ANALYZED),
        FILENAME(L_FILENAME, Field.Store.YES, Field.Index.ANALYZED),
        SORT_SIZE(L_SORT_SIZE, Field.Store.YES, Field.Index.NOT_ANALYZED),
        SORT_ATTACH(L_SORT_ATTACH, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS),
        SORT_FLAG(L_SORT_FLAG, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS),
        SORT_PRIORITY(L_SORT_PRIORITY, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS),
        H_FROM(L_H_FROM, Field.Store.YES, Field.Index.ANALYZED),
        H_TO(L_H_TO, Field.Store.YES, Field.Index.ANALYZED),
        H_CC(L_H_CC, Field.Store.YES, Field.Index.ANALYZED),
        H_X_ENV_FROM(L_H_X_ENV_FROM, Field.Store.YES, Field.Index.ANALYZED),
        H_X_ENV_TO(L_H_X_ENV_TO, Field.Store.YES, Field.Index.ANALYZED),
        H_MESSAGE_ID(L_H_MESSAGE_ID, Field.Store.NO, Field.Index.NOT_ANALYZED),
        FIELD(L_FIELD, Field.Store.YES, Field.Index.ANALYZED),
        SORT_NAME(L_SORT_NAME, Field.Store.NO, Field.Index.NOT_ANALYZED),
        H_SUBJECT(L_H_SUBJECT, Field.Store.NO, Field.Index.ANALYZED),
        SORT_SUBJECT(L_SORT_SUBJECT, Field.Store.NO, Field.Index.NOT_ANALYZED),
        CONTENT(L_CONTENT, Field.Store.NO, Field.Index.ANALYZED),
        ATTACHMENTS(L_ATTACHMENTS, Field.Store.YES, Field.Index.ANALYZED),
        MAILBOX_BLOB_ID(L_MAILBOX_BLOB_ID, Field.Store.YES, Field.Index.NOT_ANALYZED),
        SORT_DATE(L_SORT_DATE, Field.Store.YES, Field.Index.NOT_ANALYZED),
        CONTACT_DATA(L_CONTACT_DATA, Field.Store.NO, Field.Index.ANALYZED),
        OBJECTS(L_OBJECTS, Field.Store.NO, Field.Index.ANALYZED),
        VERSION(L_VERSION, Field.Store.YES, Field.Index.NOT_ANALYZED);

        private String fieldName;
        private Field.Store storeSetting;
        private Field.Index indexSetting;
        IndexField(String fieldName, Field.Store storeSetting, Field.Index indexSetting) {
            this.fieldName = fieldName;
            this.storeSetting = storeSetting;
            this.indexSetting = indexSetting;
        }
        public String getFieldName() {
            return fieldName;
        }
        public Field.Store getStoreSetting() {
            return storeSetting;
        }
        public Field.Index getIndexSetting() {
            return indexSetting;
        }
        public static IndexField fromFieldName(String name) {
            for (IndexField ifield: IndexField.values()) {
                if (ifield.getFieldName().equals(name)) {
                    return ifield;
                }
            }
            throw new IllegalArgumentException("Unrecognised Index field name " + name);
        }
    }

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
