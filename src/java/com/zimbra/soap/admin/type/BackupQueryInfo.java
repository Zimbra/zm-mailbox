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
import javax.xml.bind.annotation.XmlElementWrapper;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class BackupQueryInfo {

    /**
     * @zm-api-field-tag backup-set-label
     * @zm-api-field-description Backup set label
     */
    @XmlAttribute(name=BackupConstants.A_LABEL /* label */, required=false)
    private String label;

    /**
     * @zm-api-field-tag backup-type
     * @zm-api-field-description Backup type - <b>full|incremental</b>
     */
    @XmlAttribute(name=BackupConstants.A_TYPE /* type */, required=false)
    private String type;

    /**
     * @zm-api-field-tag aborted-flag
     * @zm-api-field-description Set if backup was aborted by the abort command
     */
    @XmlAttribute(name=BackupConstants.A_ABORTED /* aborted */, required=false)
    private ZmBoolean aborted;

    /**
     * @zm-api-field-tag start-time-millis
     * @zm-api-field-description Start time in milliseconds
     */
    @XmlAttribute(name=BackupConstants.A_START /* start */, required=false)
    private Long start;

    /**
     * @zm-api-field-tag end-time-millis
     * @zm-api-field-description End time in milliseconds.  Omitted for a live backup
     */
    @XmlAttribute(name=BackupConstants.A_END /* end */, required=false)
    private Long end;

    /**
     * @zm-api-field-tag min-redo-seq
     * @zm-api-field-description Minimum redo sequence in this backup set
     */
    @XmlAttribute(name=BackupConstants.A_MIN_REDO_SEQ /* minRedoSeq */, required=false)
    private Long minRedoSeq;

    /**
     * @zm-api-field-tag max-redo-seq
     * @zm-api-field-description Maximum redo sequence in this backup set
     */
    @XmlAttribute(name=BackupConstants.A_MAX_REDO_SEQ /* maxRedoSeq */, required=false)
    private Long maxRedoSeq;

    /**
     * @zm-api-field-tag live-backup-in-progress
     * @zm-api-field-description "live" means backup is currently in progress
     */
    @XmlAttribute(name=BackupConstants.A_LIVE /* live */, required=false)
    private ZmBoolean live;

    /**
     * @zm-api-field-description Information about current accounts.  <b>&lt;currentAccounts></b> is returned only for
     * a live backup.
     */
    @XmlElement(name=BackupConstants.E_CURRENT_ACCOUNTS /* currentAccounts */, required=false)
    private CurrentAccounts currentAccounts;

    /**
     * @zm-api-field-description Backup information by account
     */
    @XmlElement(name=BackupConstants.E_ACCOUNTS /* accounts */, required=false)
    private BackupQueryAccounts accounts;

    /**
     * @zm-api-field-description Any errors that are not account-specific; account-specific errors are returned in
     * <b>&lt;account></b>.
     */
    @XmlElement(name=BackupConstants.E_ERROR /* error */, required=false)
    private List<BackupQueryError> errors = Lists.newArrayList();

    /**
     * @zm-api-field-description Statistics.  Returned if request specified stats="1" (true)
     */
    @XmlElementWrapper(name=BackupConstants.E_STATS /* stats */, required=false)
    @XmlElement(name=BackupConstants.E_COUNTER /* counter */, required=false)
    private List<BackupQueryCounter> stats = Lists.newArrayList();

    public BackupQueryInfo() {
    }

    public void setLabel(String label) { this.label = label; }
    public void setType(String type) { this.type = type; }
    public void setAborted(Boolean aborted) { this.aborted = ZmBoolean.fromBool(aborted); }
    public void setStart(Long start) { this.start = start; }
    public void setEnd(Long end) { this.end = end; }
    public void setMinRedoSeq(Long minRedoSeq) { this.minRedoSeq = minRedoSeq; }
    public void setMaxRedoSeq(Long maxRedoSeq) { this.maxRedoSeq = maxRedoSeq; }
    public void setLive(Boolean live) { this.live = ZmBoolean.fromBool(live); }
    public void setCurrentAccounts(CurrentAccounts currentAccounts) {
        this.currentAccounts = currentAccounts;
    }
    public void setAccounts(BackupQueryAccounts accounts) {
        this.accounts = accounts;
    }
    public void setErrors(Iterable <BackupQueryError> errors) {
        this.errors.clear();
        if (errors != null) {
            Iterables.addAll(this.errors,errors);
        }
    }

    public void addError(BackupQueryError error) {
        this.errors.add(error);
    }

    public void setStats(Iterable <BackupQueryCounter> stats) {
        this.stats.clear();
        if (stats != null) {
            Iterables.addAll(this.stats,stats);
        }
    }

    public void addStat(BackupQueryCounter stat) {
        this.stats.add(stat);
    }

    public String getLabel() { return label; }
    public String getType() { return type; }
    public Boolean getAborted() { return ZmBoolean.toBool(aborted); }
    public Long getStart() { return start; }
    public Long getEnd() { return end; }
    public Long getMinRedoSeq() { return minRedoSeq; }
    public Long getMaxRedoSeq() { return maxRedoSeq; }
    public Boolean getLive() { return ZmBoolean.toBool(live); }
    public CurrentAccounts getCurrentAccounts() { return currentAccounts; }
    public BackupQueryAccounts getAccounts() { return accounts; }
    public List<BackupQueryError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    public List<BackupQueryCounter> getStats() {
        return Collections.unmodifiableList(stats);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("label", label)
            .add("type", type)
            .add("aborted", aborted)
            .add("start", start)
            .add("end", end)
            .add("minRedoSeq", minRedoSeq)
            .add("maxRedoSeq", maxRedoSeq)
            .add("live", live)
            .add("currentAccounts", currentAccounts)
            .add("accounts", accounts)
            .add("errors", errors)
            .add("stats", stats);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
