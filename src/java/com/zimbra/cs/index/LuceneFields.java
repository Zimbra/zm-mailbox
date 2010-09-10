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

package com.zimbra.cs.index;

/**
 * Standard Lucene fields.
 *
 * @since Apr 30, 2004
 * @author schemers
 */
public final class LuceneFields {

    private LuceneFields() {
    }

    /*******************************
     *
     * SORTING FIELDS
     *
     * These fields are used for the sorting of lucene results.  They MUST be stored
     * with all documents, and their values MUST match the values in the corresponding
     * MailItem row entry.  They MUST be Indexed and Untokenized.
     *
     ************/
    /** Subject for sorting purposes (untokenized)  */
    public static final String L_SORT_SUBJECT = "subjSort";
    /** name for sorting purposes */
    public static final String L_SORT_NAME = "nameSort";
    /**
     * date for sorting purposes
     * ALSO searchable date-  "date:"  "after:" and "before:"
     **/
    public static final String L_SORT_DATE = "l.date";

    @Deprecated
    public static final String L_DATE = L_SORT_DATE;

    /** size of document "size:" searches, "larger:" and "smaller:"*/
    public static final String L_SORT_SIZE = "l.size";

    @Deprecated
    public static final String L_SIZE = L_SORT_SIZE;


    /*********************************
     *
     * "ALL" field, workaround for lucene's inability to do naked not queries
     *
     * This field will automatically be populated with one term, "all" for all
     * documents.
     *
     ************/
    public static final String L_ALL = "ALL";
    public static final String L_ALL_VALUE = "yes";

    /**
     * The "index id" this document -- maps to one or more rows in the DB's mail_item
     * table (index_id column)
     *
     * This field will automatically be populated with the index ID
     */
    public static final String L_MAILBOX_BLOB_ID = "l.mbox_blob_id";


    /**
     * unique set of all attachment content types, or "none" if no
     * attachments.
     *
     * "attachment:"  searches
     */
    public static final String L_ATTACHMENTS = "attachment";

    public static final String L_ATTACHMENT_NONE = "none";
    public static final String L_ATTACHMENT_ANY = "any";

    /**
     * the "content" of the message/attachment/etc
     *
     * searches with no operator will search this field
     */
    public static final String L_CONTENT = "l.content";

    /**
     * list of objects  "has:" searches
     */
    public static final String L_OBJECTS = "has";


    /**
     * MIME-type of blob  "type:" searches
     **/
    public static final String L_MIMETYPE = "type";

    /** version */
    public static final String L_VERSION = "l.version";

    /** Easily Searchable Contact  Data (bug 11831)   "contact:" searches*/
    public static final String L_CONTACT_DATA= "l.contactData";

    /**
     * Partname identifier for multipart/mime messages
     *
     *       For RFC/822 messages, should be dotted-number MIME part name or "top"
     *       For all other mail_item types, should be "top", or should be specific to the type
     *       note that the partname is used in a weird way when constructing "NOT" queries --
     *       bare 'not's are only checked against toplevel parts....see LuceneQueryOperation.java
     *       AddClause()
     *
     */
    public static final String L_PARTNAME = "l.partname";
    public static final String L_PARTNAME_CONTACT = "CONTACT";
    public static final String L_PARTNAME_NOTE= "NOTE";
    public static final String L_PARTNAME_NONE= "none";
    public static final String L_PARTNAME_TOP= "top";

    /** "filename:" searches */
    public static final String L_FILENAME = "filename";

    /** "from:" searches */
    public static final String L_H_FROM = "from";
    /** "to:" searches */
    public static final String L_H_TO = "to";
    /** "cc:" searches */
    public static final String L_H_CC = "cc";
    /** "subject:" searches*/
    public static final String L_H_SUBJECT = "subject";

    /** x-envelope-from / to, see bug 8703 */
    public static final String L_H_X_ENV_FROM = "env_from";
    public static final String L_H_X_ENV_TO = "env_to";

    /** Message-Id: */
    public static final String L_H_MESSAGE_ID = "msg_id";

    /** field operator: structured data storage */
    public static final String L_FIELD = "l.field";
}
