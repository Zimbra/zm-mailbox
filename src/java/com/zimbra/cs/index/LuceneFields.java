/*
 * Created on Apr 30, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.liquidsys.coco.index;


/**
 * @author schemers
 *
 * Standard Lucene fields.
 * 
 */
public class LuceneFields {
    /** 
     * unique set of all attachment content types, or "none" if no
     * attachments.
     */
    public static final String L_ATTACHMENTS = "attachment";

    public static final String L_ATTACHMENT_NONE = "none";
    public static final String L_ATTACHMENT_ANY = "any";
    
    /**
     * The "index id" this document -- maps to one or more rows in the DB's mail_item
     * table (index_id column)
     */
    public static final String L_MAILBOX_BLOB_ID = "l.mbox_blob_id";
    
    /** the "content" of the message/attachment/etc  */
    public static final String L_CONTENT = "l.content";
    /** searchable date */ 
    public static final String L_DATE = "l.date";
    /** list of objects */
    public static final String L_OBJECTS = "has";
    /** MIME-type of blob */
    public static final String L_MIMETYPE = "type"; 
    /** size of blob */
    public static final String L_SIZE = "l.size";

    /** dotted-number MIME part name or CONTACT for contact*/
    public static final String L_PARTNAME = "l.partname";
    public static final String L_PARTNAME_CONTACT = "CONTACT";
    public static final String L_PARTNAME_NOTE= "NOTE";
    public static final String L_PARTNAME_NONE= "none";
    public static final String L_PARTNAME_TOP= "top";

    /** MIME name/filename */
    public static final String L_FILENAME = "filename";

    /** thread ID */
//    public static final String L_THREADID = "l.thread_id";
    
    /** "From" RFC2822 header */
    public static final String L_H_FROM = "from";
    /** "To" RFC2822 header */
    public static final String L_H_TO = "to";
    /** "CC" RFC2822 header */
    public static final String L_H_CC = "cc";
    /** "Subject" RFC2822 header */
    public static final String L_H_SUBJECT = "subject";

    /** Subject for sorting purposes (untokenized)  */
    public static final String L_SORT_SUBJECT = "subjSort";
    /** name for sorting purposes */
    public static final String L_SORT_NAME = "nameSort";
    
    
//    /** "Date" RFC2822 header */
//    public static final String L_H_DATE = "date";

    /** "ALL" field, workaround for lucene's inability to do naked not queries */
    public static final String L_ALL = "ALL";
    public static final String L_ALL_VALUE = "yes";
}
