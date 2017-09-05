/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.google.common.base.Strings;
import com.zimbra.cs.index.analysis.FieldTokenStream;
import com.zimbra.cs.index.analysis.MimeTypeTokenStream;
import com.zimbra.cs.index.analysis.RFC822AddressTokenStream;

/**
 * Helper for Lucene {@link Document}.
 *
 * @author ysasaki
 */
public final class IndexDocument {

    public static final String SEARCH_HISTORY_TYPE = "sh";

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

    public void addMimeType(MimeTypeTokenStream stream) {
        document.add(new Field(LuceneFields.L_MIMETYPE, stream));
    }

    public void addPartName(String value) {
        document.add(new Field(LuceneFields.L_PARTNAME, value, Field.Store.YES, Field.Index.NOT_ANALYZED));
    }

    public void addFilename(String value) {
        document.add(new Field(LuceneFields.L_FILENAME, value, Field.Store.YES, Field.Index.ANALYZED));
    }

    public void addSortSize(long value) {
        document.add(new Field(LuceneFields.L_SORT_SIZE, String.valueOf(value), Field.Store.YES,
                Field.Index.NOT_ANALYZED));
    }

    public void removeSortSize() {
        document.removeFields(LuceneFields.L_SORT_SIZE);
    }

    public void addSortAttachment(boolean value) {
        document.add(new Field(LuceneFields.L_SORT_ATTACH, LuceneFields.valueForBooleanField(value),
                Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
    }

    public void removeSortAttachment() {
        document.removeFields(LuceneFields.L_SORT_ATTACH);
    }

    public void addSortFlag(boolean value) {
        document.add(new Field(LuceneFields.L_SORT_FLAG, LuceneFields.valueForBooleanField(value),
                Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
    }

    public void removeSortFlag() {
        document.removeFields(LuceneFields.L_SORT_FLAG);
    }

    public void addSortPriority(int value) {
        document.add(new Field(LuceneFields.L_SORT_PRIORITY, LuceneFields.valueForPriority(value),
                Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
    }

    public void removeSortPriority() {
        document.removeFields(LuceneFields.L_SORT_PRIORITY);
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
        document.add(new Field(LuceneFields.L_H_MESSAGE_ID, value, Field.Store.NO, Field.Index.NOT_ANALYZED));
    }

    public void addField(FieldTokenStream stream) {
        document.add(new Field(LuceneFields.L_FIELD, stream));
    }

    public void addSortName(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return;
        }
        document.add(new Field(LuceneFields.L_SORT_NAME, value.toLowerCase(),
                Field.Store.NO, Field.Index.NOT_ANALYZED));
    }

    public void removeSortName() {
        document.removeFields(LuceneFields.L_SORT_NAME);
    }

    public void addSubject(String value) {
        document.add(new Field(LuceneFields.L_H_SUBJECT, value, Field.Store.NO, Field.Index.ANALYZED));
    }

    public void removeSubject() {
        document.removeFields(LuceneFields.L_H_SUBJECT);
    }

    public void addSortSubject(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return;
        }
        document.add(new Field(LuceneFields.L_SORT_SUBJECT, value.toUpperCase(),
                Field.Store.NO, Field.Index.NOT_ANALYZED));
    }

    public void removeSortSubject() {
        document.removeFields(LuceneFields.L_SORT_SUBJECT);
    }

    public void addContent(String value) {
        document.add(new Field(LuceneFields.L_CONTENT, value, Field.Store.NO, Field.Index.ANALYZED));
    }

    public void addAttachments(MimeTypeTokenStream stream) {
        document.add(new Field(LuceneFields.L_ATTACHMENTS, stream));
    }

    public void addMailboxBlobId(int value) {
        document.add(new Field(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(value),
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
        document.add(new Field(LuceneFields.L_CONTACT_DATA, value, Field.Store.NO, Field.Index.ANALYZED));
    }

    public void addObjects(String value) {
        document.add(new Field(LuceneFields.L_OBJECTS, value, Field.Store.NO, Field.Index.ANALYZED));
    }

    public void addVersion(int value) {
        document.add(new Field(LuceneFields.L_VERSION, String.valueOf(value),
                Field.Store.YES, Field.Index.NOT_ANALYZED));
    }

    public static IndexDocument fromSearchString(int id, String searchString) {
        IndexDocument doc = new IndexDocument();
        doc.document.add(new Field(LuceneFields.L_SEARCH_EXACT, searchString, Field.Store.NO, Field.Index.ANALYZED));
        doc.document.add(new Field(LuceneFields.L_SEARCH_TERMS, searchString, Field.Store.NO, Field.Index.ANALYZED));
        doc.document.add(new Field(LuceneFields.L_SEARCH_ID, String.valueOf(id), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.document.add(new Field(LuceneFields.L_ITEM_TYPE, SEARCH_HISTORY_TYPE, Field.Store.YES, Field.Index.NOT_ANALYZED));
        return doc;
    }

}
