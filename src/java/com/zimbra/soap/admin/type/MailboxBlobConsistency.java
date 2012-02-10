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

package com.zimbra.soap.admin.type;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class MailboxBlobConsistency {

    /**
     * @zm-api-field-tag mailbox-id
     * @zm-api-field-description Mailbox ID
     */
    @XmlAttribute(name=AdminConstants.A_ID /* id */, required=true)
    private final Integer id;

    /**
     * @zm-api-field-description Information about missing blobs
     */
    @XmlElementWrapper(name=AdminConstants.E_MISSING_BLOBS /* missingBlobs */, required=true)
    @XmlElement(name=AdminConstants.E_ITEM, required=false)
    private List<MissingBlobInfo> missingBlobs = Lists.newArrayList();

    /**
     * @zm-api-field-description Information about items with incorrect sizes
     */
    @XmlElementWrapper(name=AdminConstants.E_INCORRECT_SIZE /* incorrectSize */, required=true)
    @XmlElement(name=AdminConstants.E_ITEM, required=false)
    private List<IncorrectBlobSizeInfo> incorrectSizes = Lists.newArrayList();

    /**
     * @zm-api-field-description Information about unexpected blobs
     */
    @XmlElementWrapper(name=AdminConstants.E_UNEXPECTED_BLOBS /* unexpectedBlobs */, required=true)
    @XmlElement(name=AdminConstants.E_BLOB, required=false)
    private List<UnexpectedBlobInfo> unexpectedBlobs = Lists.newArrayList();

    /**
     * @zm-api-field-description Information about items with incorrect revisions
     */
    @XmlElementWrapper(name=AdminConstants.E_INCORRECT_REVISION /* incorrectRevision */, required=true)
    @XmlElement(name=AdminConstants.E_ITEM, required=false)
    private List<IncorrectBlobRevisionInfo> incorrectRevisions = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MailboxBlobConsistency() {
        this((Integer) null);
    }

    public MailboxBlobConsistency(Integer id) {
        this.id = id;
    }

    public void setMissingBlobs(Iterable <MissingBlobInfo> missingBlobs) {
        this.missingBlobs.clear();
        if (missingBlobs != null) {
            Iterables.addAll(this.missingBlobs,missingBlobs);
        }
    }

    public MailboxBlobConsistency addMissingBlob(MissingBlobInfo missingBlob) {
        this.missingBlobs.add(missingBlob);
        return this;
    }

    public void setIncorrectSizes(Iterable <IncorrectBlobSizeInfo> incorrectSizes) {
        this.incorrectSizes.clear();
        if (incorrectSizes != null) {
            Iterables.addAll(this.incorrectSizes,incorrectSizes);
        }
    }

    public MailboxBlobConsistency addIncorrectSize(IncorrectBlobSizeInfo incorrectSize) {
        this.incorrectSizes.add(incorrectSize);
        return this;
    }

    public void setUnexpectedBlobs(Iterable <UnexpectedBlobInfo> unexpectedBlobs) {
        this.unexpectedBlobs.clear();
        if (unexpectedBlobs != null) {
            Iterables.addAll(this.unexpectedBlobs,unexpectedBlobs);
        }
    }

    public MailboxBlobConsistency addUnexpectedBlob(UnexpectedBlobInfo unexpectedBlob) {
        this.unexpectedBlobs.add(unexpectedBlob);
        return this;
    }

    public void setIncorrectRevisions(Iterable <IncorrectBlobRevisionInfo> incorrectRevisions) {
        this.incorrectRevisions.clear();
        if (incorrectRevisions != null) {
            Iterables.addAll(this.incorrectRevisions,incorrectRevisions);
        }
    }

    public MailboxBlobConsistency addIncorrectRevision(IncorrectBlobRevisionInfo incorrectRevision) {
        this.incorrectRevisions.add(incorrectRevision);
        return this;
    }

    public Integer getId() { return id; }
    public List<MissingBlobInfo> getMissingBlobs() {
        return Collections.unmodifiableList(missingBlobs);
    }
    public List<IncorrectBlobSizeInfo> getIncorrectSizes() {
        return Collections.unmodifiableList(incorrectSizes);
    }
    public List<UnexpectedBlobInfo> getUnexpectedBlobs() {
        return Collections.unmodifiableList(unexpectedBlobs);
    }
    public List<IncorrectBlobRevisionInfo> getIncorrectRevisions() {
        return Collections.unmodifiableList(incorrectRevisions);
    }
}
