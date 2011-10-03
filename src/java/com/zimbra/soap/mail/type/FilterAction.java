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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class FilterAction {

    @XmlAttribute(name=MailConstants.A_INDEX, required=false)
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
    public static final class FlagAction extends FilterAction {

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

        @XmlAttribute(name=MailConstants.A_ADDRESS, required=false)
        private String address;

        @XmlAttribute(name=MailConstants.A_SUBJECT, required=false)
        private String subject;

        @XmlAttribute(name=MailConstants.A_MAX_BODY_SIZE, required=false)
        private Integer maxBodySize;

        @XmlElement(name=MailConstants.E_CONTENT, required=false)
        private String content;

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
