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

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.BackupConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"fileCopier", "accounts"})
public class RestoreSpec {

    // Valid values ca/ra/mb
    @XmlAttribute(name=BackupConstants.A_METHOD /* method */, required=false)
    private String method;

    // Valid values are include/exclude (case insensitive)
    @XmlAttribute(name=BackupConstants.A_SEARCH_INDEX
                    /* searchIndex */, required=false)
    private String searchIndex;

    // Valid values are include/exclude (case insensitive)
    @XmlAttribute(name=BackupConstants.A_BLOBS /* blobs */, required=false)
    private String blobs;

    // Valid values are include/exclude (case insensitive)
    @XmlAttribute(name=BackupConstants.A_SECONDARY_BLOBS
                    /* secondaryBlobs */, required=false)
    private String secondaryBlobs;

    @XmlAttribute(name=BackupConstants.A_BACKUP_TARGET
                    /* target */, required=false)
    private String target;

    @XmlAttribute(name=BackupConstants.A_LABEL /* label */, required=false)
    private String label;

    @XmlAttribute(name=BackupConstants.A_SYSDATA /* sysData */, required=false)
    private Boolean sysData;

    @XmlAttribute(name=BackupConstants.A_INCLUDEINCREMENTALS
                    /* includeIncrementals */, required=false)
    private Boolean includeIncrementals;

    @XmlAttribute(name=BackupConstants.A_REPLAY_CURRENT_REDOLOGS
                    /* replayRedo */, required=false)
    private Boolean replayCurrentRedoLogs;

    @XmlAttribute(name=BackupConstants.A_CONTINUE
                    /* continue */, required=false)
    private Boolean continueOnError;

    @XmlAttribute(name=BackupConstants.A_PREFIX /* prefix */, required=false)
    private String prefix;

    @XmlAttribute(name=BackupConstants.A_RESTORE_TO_TIME
                    /* restoreToTime */, required=false)
    private Long restoreToTime;

    @XmlAttribute(name=BackupConstants.A_RESTORE_TO_REDO_SEQ
                    /* restoreToRedoSeq */, required=false)
    private Long restoreToRedoSeq;

    @XmlAttribute(name=BackupConstants.A_RESTORE_TO_INCR_LABEL
                    /* restoreToIncrLabel */, required=false)
    private String restoreToIncrLabel;

    @XmlAttribute(name=BackupConstants.A_IGNORE_REDO_ERRORS
                    /* ignoreRedoErrors */, required=false)
    private Boolean ignoreRedoErrors;

    @XmlAttribute(name=BackupConstants.A_SKIP_DELETE_OPS
                    /* skipDeleteOps */, required=false)
    private Boolean skipDeleteOps;

    @XmlAttribute(name=BackupConstants.A_SKIP_DELETED_ACCT
                    /* skipDeletedAccounts */, required=false)
    private Boolean skipDeletedAccounts;

    @XmlElement(name=BackupConstants.E_FILE_COPIER
                    /* fileCopier */, required=false)
    private FileCopierSpec fileCopier;

    @XmlElement(name=BackupConstants.E_ACCOUNT
                    /* account */, required=false)
    private List<Name> accounts = Lists.newArrayList();

    public RestoreSpec() {
    }

    public void setMethod(String method) { this.method = method; }
    public void setSearchIndex(String searchIndex) {
        this.searchIndex = searchIndex;
    }
    public void setBlobs(String blobs) { this.blobs = blobs; }
    public void setSecondaryBlobs(String secondaryBlobs) {
        this.secondaryBlobs = secondaryBlobs;
    }
    public void setTarget(String target) { this.target = target; }
    public void setLabel(String label) { this.label = label; }
    public void setSysData(Boolean sysData) { this.sysData = sysData; }
    public void setIncludeIncrementals(Boolean includeIncrementals) {
        this.includeIncrementals = includeIncrementals;
    }
    public void setReplayCurrentRedoLogs(Boolean replayCurrentRedoLogs) {
        this.replayCurrentRedoLogs = replayCurrentRedoLogs;
    }
    public void setContinueOnError(Boolean continueOnError) {
        this.continueOnError = continueOnError;
    }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public void setRestoreToTime(Long restoreToTime) {
        this.restoreToTime = restoreToTime;
    }
    public void setRestoreToRedoSeq(Long restoreToRedoSeq) {
        this.restoreToRedoSeq = restoreToRedoSeq;
    }
    public void setRestoreToIncrLabel(String restoreToIncrLabel) {
        this.restoreToIncrLabel = restoreToIncrLabel;
    }
    public void setIgnoreRedoErrors(Boolean ignoreRedoErrors) {
        this.ignoreRedoErrors = ignoreRedoErrors;
    }
    public void setSkipDeleteOps(Boolean skipDeleteOps) {
        this.skipDeleteOps = skipDeleteOps;
    }
    public void setSkipDeletedAccounts(Boolean skipDeletedAccounts) {
        this.skipDeletedAccounts = skipDeletedAccounts;
    }
    public void setFileCopier(FileCopierSpec fileCopier) {
        this.fileCopier = fileCopier;
    }
    public void setAccounts(Iterable <Name> accounts) {
        this.accounts.clear();
        if (accounts != null) {
            Iterables.addAll(this.accounts,accounts);
        }
    }

    public void addAccount(Name account) {
        this.accounts.add(account);
    }

    public String getMethod() { return method; }
    public String getSearchIndex() { return searchIndex; }
    public String getBlobs() { return blobs; }
    public String getSecondaryBlobs() { return secondaryBlobs; }
    public String getTarget() { return target; }
    public String getLabel() { return label; }
    public Boolean getSysData() { return sysData; }
    public Boolean getIncludeIncrementals() { return includeIncrementals; }
    public Boolean getReplayCurrentRedoLogs() { return replayCurrentRedoLogs; }
    public Boolean getContinueOnError() { return continueOnError; }
    public String getPrefix() { return prefix; }
    public Long getRestoreToTime() { return restoreToTime; }
    public Long getRestoreToRedoSeq() { return restoreToRedoSeq; }
    public String getRestoreToIncrLabel() { return restoreToIncrLabel; }
    public Boolean getIgnoreRedoErrors() { return ignoreRedoErrors; }
    public Boolean getSkipDeleteOps() { return skipDeleteOps; }
    public Boolean getSkipDeletedAccounts() { return skipDeletedAccounts; }
    public FileCopierSpec getFileCopier() { return fileCopier; }
    public List<Name> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("method", method)
            .add("searchIndex", searchIndex)
            .add("blobs", blobs)
            .add("secondaryBlobs", secondaryBlobs)
            .add("target", target)
            .add("label", label)
            .add("sysData", sysData)
            .add("includeIncrementals", includeIncrementals)
            .add("replayCurrentRedoLogs", replayCurrentRedoLogs)
            .add("continueOnError", continueOnError)
            .add("prefix", prefix)
            .add("restoreToTime", restoreToTime)
            .add("restoreToRedoSeq", restoreToRedoSeq)
            .add("restoreToIncrLabel", restoreToIncrLabel)
            .add("ignoreRedoErrors", ignoreRedoErrors)
            .add("skipDeleteOps", skipDeleteOps)
            .add("skipDeletedAccounts", skipDeletedAccounts)
            .add("fileCopier", fileCopier)
            .add("accounts", accounts);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
