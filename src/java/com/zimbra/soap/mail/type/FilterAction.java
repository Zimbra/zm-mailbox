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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.ContentSerializer;

@XmlAccessorType(XmlAccessType.NONE)
public class FilterAction {

    /**
     * @zm-api-field-tag index
     * @zm-api-field-description Index - specifies a guaranteed order for the action elements
     */
    @XmlAttribute(name=MailConstants.A_INDEX /* index */, required=false)
    private int index = 0;

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("index", index).toString();
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class DiscardAction extends FilterAction {
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class KeepAction extends FilterAction {
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class StopAction extends FilterAction {
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class FileIntoAction extends FilterAction {

        /**
         * @zm-api-field-tag folder-path
         * @zm-api-field-description Folder path
         */
        @XmlAttribute(name=MailConstants.A_FOLDER_PATH, required=false)
        private final String folder;

        @SuppressWarnings("unused")
        private FileIntoAction() {
            this(null);
        }

        public FileIntoAction(String folder) {
            this.folder = folder;
        }

        public String getFolder() {
            return folder;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("folder", folder).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    @JsonPropertyOrder({ "flagName", "index" })
    public static final class FlagAction extends FilterAction {

        /**
         * @zm-api-field-tag flag-name-flagged|read|priority
         * @zm-api-field-description Flag name - <b>flagged|read|priority</b>
         */
        @XmlAttribute(name=MailConstants.A_FLAG_NAME, required=false)
        private final String flag;

        @SuppressWarnings("unused")
        private FlagAction() {
            this(null);
        }

        public FlagAction(String flag) {
            this.flag = flag;
        }

        public String getFlag() {
            return flag;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("flag", flag).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class RedirectAction extends FilterAction {

        /**
         * @zm-api-field-tag email-address
         * @zm-api-field-description Email address
         */
        @XmlAttribute(name=MailConstants.A_ADDRESS, required=false)
        private final String address;

        @SuppressWarnings("unused")
        private RedirectAction() {
            this(null);
        }

        public RedirectAction(String addr) {
            address = addr;
        }

        public String getAddress() {
            return address;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("address", address).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class NotifyAction extends FilterAction {

        /**
         * @zm-api-field-tag email-address
         * @zm-api-field-description Email address
         */
        @XmlAttribute(name=MailConstants.A_ADDRESS, required=false)
        private String address;

        /**
         * @zm-api-field-tag subject-template
         * @zm-api-field-description Subject template
         * <br />
         * Can contain variables such as ${SUBJECT}, ${TO}, ${CC}, etc
         * (basically ${any-header-name}; case not important), plus ${BODY} (text body of the message).
         */
        @XmlAttribute(name=MailConstants.A_SUBJECT, required=false)
        private String subject;

        /**
         * @zm-api-field-tag max-body-size-bytes
         * @zm-api-field-description Maximum body size in bytes
         */
        @XmlAttribute(name=MailConstants.A_MAX_BODY_SIZE, required=false)
        private Integer maxBodySize;

        /**
         * @zm-api-field-tag body-template
         * @zm-api-field-description Body template
         * <br />
         * Can contain variables such as ${SUBJECT}, ${TO}, ${CC}, etc
         * (basically ${any-header-name}; case not important), plus ${BODY} (text body of the message).
         */
        @JsonSerialize(using=ContentSerializer.class)
        @XmlElement(name=MailConstants.E_CONTENT, required=false)
        private String content;

        /**
         * @zm-api-field-tag comma-sep-header-names|*
         * @zm-api-field-description Optional - Either "*" or a comma-separated list of header names.
         */
        @XmlAttribute(name=MailConstants.A_ORIG_HEADERS, required=false)
        private String origHeaders;

        public void setAddress(String address) {
            this.address = address;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public void setMaxBodySize(Integer maxBodySize) {
            this.maxBodySize = maxBodySize;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public void setOrigHeaders(String origHeaders) {
            this.origHeaders = origHeaders;
        }

        public String getAddress() {
            return address;
        }

        public String getSubject() {
            return subject;
        }

        public int getMaxBodySize() {
            return maxBodySize != null ? maxBodySize : -1;
        }

        public String getContent() {
            return content;
        }

        public String getOrigHeaders() {
            return origHeaders;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                .add("address", address)
                .add("subject", subject)
                .add("maxBodySize", maxBodySize)
                .add("content", content)
                .add("origHeaders", origHeaders)
                .toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class TagAction extends FilterAction {

        /**
         * @zm-api-field-tag tag-name
         * @zm-api-field-description Tag name
         */
        @XmlAttribute(name=MailConstants.A_TAG_NAME, required=true)
        private final String tag;

        @SuppressWarnings("unused")
        private TagAction() {
            this(null);
        }

        public TagAction(String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("tag", tag).toString();
        }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static final class ReplyAction extends FilterAction {

        /**
         * @zm-api-field-tag body-template
         * @zm-api-field-description Body template
         * <br />
         * Can contain variables such as ${SUBJECT}, ${TO}, ${CC}, etc
         * (basically ${any-header-name}; case not important), plus ${BODY} (text body of the message).
         */
        @JsonSerialize(using=ContentSerializer.class)
        @XmlElement(name=MailConstants.E_CONTENT, required=false)
        private final String content;

        @SuppressWarnings("unused")
        private ReplyAction() {
            this(null);
        }

        public ReplyAction(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("content", content).toString();
        }
    }

}
