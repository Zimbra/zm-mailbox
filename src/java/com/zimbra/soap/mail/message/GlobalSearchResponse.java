/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.soap.mail.message;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = MailConstants.E_GLOBAL_SEARCH_RESPONSE)
public final class GlobalSearchResponse {

    /**
     * @zm-api-field-description Search result documents
     */
    @XmlElement(name = MailConstants.E_DOC, required=false)
    private List<Document> documents;

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> list) {
        documents = list;
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class Document {
        /**
         * @zm-api-field-tag id
         * @zm-api-field-description ID
         */
        @XmlAttribute(name = MailConstants.A_ID /* id */, required=false)
        private String id;

        /**
         * @zm-api-field-tag score
         * @zm-api-field-description Score
         */
        @XmlAttribute(name = MailConstants.A_SCORE /* score */, required=false)
        private int score;

        /**
         * @zm-api-field-tag name
         * @zm-api-field-description Name
         */
        @XmlAttribute(name = MailConstants.A_NAME /* name */, required=false)
        private String name;

        /**
         * @zm-api-field-tag date
         * @zm-api-field-description Date
         */
        @XmlAttribute(name = MailConstants.A_DATE /* d */, required=false)
        private long date;

        /**
         * @zm-api-field-tag size
         * @zm-api-field-description Size
         */
        @XmlAttribute(name = MailConstants.A_SIZE /* s */, required=false)
        private long size;

        /**
         * @zm-api-field-tag content-type
         * @zm-api-field-description Content type
         */
        @XmlAttribute(name = MailConstants.A_CONTENT_TYPE /* ct */, required=false)
        private String contentType;

        /**
         * @zm-api-field-tag fragment
         * @zm-api-field-description First few bytes of the message (probably between 40 and 100 bytes)
         */
        @XmlElement(name = MailConstants.E_FRAG /* fr */, required=false)
        private String fragment;

        public String getID() {
            return id;
        }

        public void setID(String value) {
            id = value;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int value) {
            score = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String value) {
            name = value;
        }

        public long getDate() {
            return date;
        }

        public void setDate(long value) {
            date = value;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long value) {
            size = value;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String value) {
            contentType = value;
        }

        public String getFragment() {
            return fragment;
        }

        public void setFragment(String value) {
            fragment = value;
        }
    }

}
