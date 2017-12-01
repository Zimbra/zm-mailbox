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

import org.apache.lucene.document.Document;
import org.apache.solr.common.SolrInputDocument;

import com.google.common.base.Strings;

/**
 * Helper for Lucene {@link Document}.
 *
 * @author ysasaki
 */
public final class IndexDocument {

    public static final String SEARCH_HISTORY_TYPE = "sh";

    private final SolrInputDocument document;

    public IndexDocument() {
        document = new SolrInputDocument();
    }

    public IndexDocument(SolrInputDocument doc) {
        document = doc;
    }

    public SolrInputDocument toInputDocument() {
        return document;
    }

    public void addMimeType(String value) {
        document.addField(LuceneFields.L_MIMETYPE, value);
    }

    public void addPartName(String value) {
        document.addField(LuceneFields.L_PARTNAME, value);
    }

    public void addFilename(String value) {
        document.addField(LuceneFields.L_FILENAME, value);
    }

    public void addSortSize(long value) {
        document.addField(LuceneFields.L_SORT_SIZE, String.valueOf(value));
    }

    public void removeSortSize() {
        document.removeField(LuceneFields.L_SORT_SIZE);
    }

    public void addSortAttachment(boolean value) {
        document.addField(LuceneFields.L_SORT_ATTACH, LuceneFields.valueForBooleanField(value));
    }

    public void removeSortAttachment() {
        document.removeField(LuceneFields.L_SORT_ATTACH);
    }

    public void addSortFlag(boolean value) {
        document.addField(LuceneFields.L_SORT_FLAG, LuceneFields.valueForBooleanField(value));
    }

    public void removeSortFlag() {
        document.removeField(LuceneFields.L_SORT_FLAG);
    }

    public void addSortPriority(int value) {
        document.addField(LuceneFields.L_SORT_PRIORITY, LuceneFields.valueForPriority(value));
    }

    public void removeSortPriority() {
        document.removeField(LuceneFields.L_SORT_PRIORITY);
    }

    public void addFrom(String from) {
        document.addField(LuceneFields.L_H_FROM, from);
    }

    public void removeFrom() {
        document.removeField(LuceneFields.L_H_FROM);
    }

    public void addTo(String to) {
        document.addField(LuceneFields.L_H_TO, to);
    }

    public void removeTo() {
        document.removeField(LuceneFields.L_H_TO);
    }

    public void addCc(String cc) {
        document.addField(LuceneFields.L_H_CC, cc);
    }

    public void removeCc() {
        document.removeField(LuceneFields.L_H_CC);
    }

    public void addEnvFrom(String envFrom) {
        document.addField(LuceneFields.L_H_X_ENV_FROM, envFrom);
    }

    public void addEnvTo(String envTo) {
        document.addField(LuceneFields.L_H_X_ENV_TO, envTo);
    }

    public void addMessageId(String value) {
        document.addField(LuceneFields.L_H_MESSAGE_ID, value);
    }

    public void addField(String value) {
        document.addField(LuceneFields.L_FIELD, value);
    }

    public void addSortName(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return;
        }
        document.addField(LuceneFields.L_SORT_NAME, value.toLowerCase());
    }

    public void removeSortName() {
        document.removeField(LuceneFields.L_SORT_NAME);
    }

    public void addSubject(String value) {
        document.addField(LuceneFields.L_H_SUBJECT, value);
    }

    public void removeSubject() {
        document.removeField(LuceneFields.L_H_SUBJECT);
    }

    public void addSortSubject(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return;
        }
        document.addField(LuceneFields.L_SORT_SUBJECT, value.toUpperCase());
    }

    public void removeSortSubject() {
        document.removeField(LuceneFields.L_SORT_SUBJECT);
    }

    public void addContent(String value) {
        document.addField(LuceneFields.L_CONTENT, value);
    }

    public void addAttachments(String value) {
        document.addField(LuceneFields.L_ATTACHMENTS, value);
    }

    public void addMailboxBlobId(int value) {
        document.addField(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(value));
    }

    public void removeMailboxBlobId() {
        document.removeField(LuceneFields.L_MAILBOX_BLOB_ID);
    }

    public void addSortDate(long value) {
        document.addField(LuceneFields.L_SORT_DATE, String.valueOf(value));
    }

    public void removeSortDate() {
        document.removeField(LuceneFields.L_SORT_DATE);
    }

    public void addContactData(String value) {
        document.addField(LuceneFields.L_CONTACT_DATA, value);
    }

    public void addObjects(String value) {
        document.addField(LuceneFields.L_OBJECTS, value);
    }

    public void addVersion(int value) {
        document.addField(LuceneFields.L_VERSION, String.valueOf(value));
    }

    public String get(String fieldName) {
        return (String) document.getFieldValue(fieldName);
    }

    public static IndexDocument fromSearchString(int id, String searchString) {
        IndexDocument doc = new IndexDocument();
        doc.document.addField(LuceneFields.L_SEARCH_EXACT, searchString); //will be copied to sh_terms by solr
        doc.document.addField(LuceneFields.L_SEARCH_ID, String.valueOf(id));
        doc.document.addField(LuceneFields.L_ITEM_TYPE, SEARCH_HISTORY_TYPE);
        return doc;
    }

}
