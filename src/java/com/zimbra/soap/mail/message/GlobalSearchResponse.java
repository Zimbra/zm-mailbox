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

    @XmlElement(name = MailConstants.E_DOC)
    private List<Document> documents;

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> list) {
        documents = list;
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class Document {
        @XmlAttribute(name = MailConstants.A_ID)
        private String id;

        @XmlAttribute(name = MailConstants.A_NAME)
        private String name;

        @XmlAttribute(name = MailConstants.A_DATE)
        private long date;

        @XmlAttribute(name = MailConstants.A_SIZE)
        private long size;

        @XmlAttribute(name = MailConstants.A_CONTENT_TYPE)
        private String contentType;

        @XmlElement(name = MailConstants.E_FRAG)
        private String fragment;

        public String getID() {
            return id;
        }

        public void setID(String value) {
            id = value;
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
