/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.cs.index.analysis.RFC822AddressTokenStream;

/**
 * Helper for Lucene {@link Document}.
 *
 * @author ysasaki
 */
public final class IndexDocument {
    private final Document document;

    public IndexDocument() {
        document = new Document();
    }

    public IndexDocument(Document doc) {
        document = doc;
    }

    public Document toDocument() {
        return document;
    }

    public void addMimeType(String value) {
        document.add(new Field(LuceneFields.L_MIMETYPE, value,
                Field.Store.YES, Field.Index.ANALYZED));
    }

    public void addPartName(String value) {
        document.add(new Field(LuceneFields.L_PARTNAME, value,
                Field.Store.YES, Field.Index.NOT_ANALYZED));
    }

    public void addFilename(String value) {
        document.add(new Field(LuceneFields.L_FILENAME, value,
                    Field.Store.YES, Field.Index.ANALYZED));
    }

    public void addSortSize(long value) {
        document.add(new Field(LuceneFields.L_SORT_SIZE, String.valueOf(value),
                Field.Store.YES, Field.Index.NO));
    }

    public void removeSortSize() {
        document.removeFields(LuceneFields.L_SORT_SIZE);
    }

    public void addFrom(RFC822AddressTokenStream stream) {
        document.add(new Field(LuceneFields.L_H_FROM, stream));
    }

    public void removeFrom() {
        document.removeFields(LuceneFields.L_H_FROM);
    }

    public void addTo(RFC822AddressTokenStream stream) {
        document.add(new Field(LuceneFields.L_H_TO, stream));
    }

    public void removeTo() {
        document.removeFields(LuceneFields.L_H_TO);
    }

    public void addCc(RFC822AddressTokenStream stream) {
        document.add(new Field(LuceneFields.L_H_CC, stream));
    }

    public void removeCc() {
        document.removeFields(LuceneFields.L_H_CC);
    }

    public void addEnvFrom(RFC822AddressTokenStream stream) {
        document.add(new Field(LuceneFields.L_H_X_ENV_FROM, stream));
    }

    public void addEnvTo(RFC822AddressTokenStream stream) {
        document.add(new Field(LuceneFields.L_H_X_ENV_TO, stream));
    }

    public void addMessageId(String value) {
        document.add(new Field(LuceneFields.L_H_MESSAGE_ID, value,
            Field.Store.NO, Field.Index.NOT_ANALYZED));
    }

    public void addField(String value) {
        document.add(new Field(LuceneFields.L_FIELD, value,
            Field.Store.NO, Field.Index.ANALYZED));
    }

    public void addSortName(String value) {
        document.add(new Field(LuceneFields.L_SORT_NAME, value,
                Field.Store.NO, Field.Index.NOT_ANALYZED));
    }

    public void removeSortName() {
        document.removeFields(LuceneFields.L_SORT_NAME);
    }

    public void addSubject(String value) {
        document.add(new Field(LuceneFields.L_H_SUBJECT, value,
                Field.Store.NO, Field.Index.ANALYZED));
    }

    public void removeSubject() {
        document.removeFields(LuceneFields.L_H_SUBJECT);
    }

    public void addSortSubject(String value) {
        document.add(new Field(LuceneFields.L_SORT_SUBJECT, value,
                Field.Store.NO, Field.Index.NOT_ANALYZED));
    }

    public void removeSortSubject() {
        document.removeFields(LuceneFields.L_SORT_SUBJECT);
    }

    public void addContent(String value) {
        document.add(new Field(LuceneFields.L_CONTENT, value,
                Field.Store.NO, Field.Index.ANALYZED));
    }

    public void addAttachments(String value) {
        document.add(new Field(LuceneFields.L_ATTACHMENTS, value,
                Field.Store.NO, Field.Index.ANALYZED));
    }

    public void addMailboxBlobId(String value) {
        document.add(new Field(LuceneFields.L_MAILBOX_BLOB_ID, value,
                Field.Store.YES, Field.Index.NOT_ANALYZED));
    }

    public void removeMailboxBlobId() {
        document.removeFields(LuceneFields.L_MAILBOX_BLOB_ID);
    }

    public void addSortDate(long value) {
        document.add(new Field(LuceneFields.L_SORT_DATE,
                DateTools.timeToString(value, DateTools.Resolution.MILLISECOND),
                Field.Store.YES, Field.Index.NOT_ANALYZED));
    }

    public void removeSortDate() {
        document.removeFields(LuceneFields.L_SORT_DATE);
    }

    public void addContactData(String value) {
        document.add(new Field(LuceneFields.L_CONTACT_DATA, value,
            Field.Store.NO, Field.Index.ANALYZED));
    }

    public void addObjects(String value) {
        document.add(new Field(LuceneFields.L_OBJECTS, value,
            Field.Store.NO, Field.Index.ANALYZED));
    }

    public void addAll() {
        if (document.get(LuceneFields.L_ALL) == null) {
            document.add(new Field(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE,
                    Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS,
                    Field.TermVector.NO));
        }
    }

    public void addVersion(int value) {
        document.add(new Field(LuceneFields.L_VERSION, String.valueOf(value),
                Field.Store.YES, Field.Index.NOT_ANALYZED));
    }

}
