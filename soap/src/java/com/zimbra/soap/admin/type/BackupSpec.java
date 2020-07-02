/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class BackupSpec {

    /**
     * @zm-api-field-tag backup-method
     * @zm-api-field-description Backup method - <b>full|incremental|abort|delete</b>
     */
    @XmlAttribute(name=BackupConstants.A_METHOD /* method */, required=false)
    private String method;

    /**
     * @zm-api-field-tag path-to-backup-target
     * @zm-api-field-description Path to backup target
     */
    @XmlAttribute(name=BackupConstants.A_BACKUP_TARGET /* target */, required=false)
    private String target;

    /**
     * @zm-api-field-tag full-backup-label
     * @zm-api-field-description Full backup label
     */
    @XmlAttribute(name=BackupConstants.A_LABEL /* label */, required=false)
    private String label;

    /**
     * @zm-api-field-tag before
     * @zm-api-field-description Used for selecting backups to delete for method="delete".
     * <br />
     * Value is either a full backup label or a date or a time period.
     * <br />
     * If it is a date, the format is <b>YYYY/MM/DD[-hh:mm:ss]</b>
     * If it is a time period, the format is <b>nn{d|m|y}</b> where d = day, m = month, y = year
     * <br />
     * e.g., before="7d" means delete backups older than 7 days.  before="1y" means delete backups older than a year.
     */
    @XmlAttribute(name=BackupConstants.A_BEFORE /* before */, required=false)
    private String before;

    /**
     * @zm-api-field-tag sync-flag
     * @zm-api-field-description Run synchronously; command doesn't return until backup is finished
     */
    @XmlAttribute(name=BackupConstants.A_SYNC /* sync */, required=false)
    private ZmBoolean sync;

    // Valid values are config/include/exclude (case insensitive)
    /**
     * @zm-api-field-tag include-search-index-setting
     * @zm-api-field-description Option to include/exclude search index in a full backup (not applicable in
     * incremental backup).  Values: <b>include|exclude|config</b>
     * <br />
     * Default value is "config", to use the configured value.  "include" or "exclude" overrides the configuration.
     */
    @XmlAttribute(name=BackupConstants.A_SEARCH_INDEX /* searchIndex */, required=false)
    private String searchIndex;

    // Valid values are config/include/exclude (case insensitive)
    /**
     * @zm-api-field-tag include-blobs-setting
     * @zm-api-field-description Option to include/exclude blobs in a full backup (not applicable in
     * incremental backup).  Values: <b>include|exclude|config</b>
     * <br />
     * Default value is "config", to use the configured value.  "include" or "exclude" overrides the configuration.
     */
    @XmlAttribute(name=BackupConstants.A_BLOBS /* blobs */, required=false)
    private String blobs;

    // Valid values are config/include/exclude (case insensitive)
    /**
     * @zm-api-field-tag include-secondary-blobs-setting
     * @zm-api-field-description Option to include/exclude secondary blobs in a full backup (not applicable in
     * incremental backup).  Values: <b>include|exclude|config</b>
     * <br />
     * Default value is "config", to use the configured value.  "include" or "exclude" overrides the configuration.
     * <br />
     * Meaningful only when blob backup isn't excluded
     */
    @XmlAttribute(name=BackupConstants.A_SECONDARY_BLOBS /* secondaryBlobs */, required=false)
    private String secondaryBlobs;

    /**
     * @zm-api-field-tag backup-blobs-to-zip
     * @zm-api-field-description Backup blobs to zip files.  Defaults to <b>1 (true)</b>
     */
    @XmlAttribute(name=BackupConstants.A_ZIP /* zip */, required=false)
    private ZmBoolean zip;

    /**
     * @zm-api-field-tag zip-store
     * @zm-api-field-description if set, store blobs uncompressed in zip files (used only when
     * <b>{backup-blobs-to-zip}</b> is set.  Defaults to <b>1 (true)</b>
     */
    @XmlAttribute(name=BackupConstants.A_ZIP_STORE /* zipStore */, required=false)
    private ZmBoolean zipStore;

    /**
     * @zm-api-field-description File copier specification
     */
    @XmlElement(name=BackupConstants.E_FILE_COPIER /* fileCopier */, required=false)
    private FileCopierSpec fileCopier;

    /**
     * @zm-api-field-tag account-sel
     * @zm-api-field-description Account selector - either one <b>&lt;account name="all"/></b> or a list of
     * <b>&lt;account name="{account email addr}"/></b>
     */
    @XmlElement(name=BackupConstants.E_ACCOUNT /* account */, required=false)
    private List<Name> accounts = Lists.newArrayList();

    public BackupSpec() {
    }

    public void setMethod(String method) { this.method = method; }
    public void setTarget(String target) { this.target = target; }
    public void setLabel(String label) { this.label = label; }
    public void setBefore(String before) { this.before = before; }
    public void setSync(Boolean sync) { this.sync = ZmBoolean.fromBool(sync); }
    public void setSearchIndex(String searchIndex) { this.searchIndex = searchIndex; }
    public void setBlobs(String blobs) { this.blobs = blobs; }
    public void setSecondaryBlobs(String secondaryBlobs) {
        this.secondaryBlobs = secondaryBlobs;
    }
    public void setZip(Boolean zip) { this.zip = ZmBoolean.fromBool(zip); }
    public void setZipStore(Boolean zipStore) { this.zipStore = ZmBoolean.fromBool(zipStore); }
    public void setFileCopier(FileCopierSpec fileCopier) { this.fileCopier = fileCopier; }
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
    public String getTarget() { return target; }
    public String getLabel() { return label; }
    public String getBefore() { return before; }
    public Boolean getSync() { return ZmBoolean.toBool(sync); }
    public String getSearchIndex() { return searchIndex; }
    public String getBlobs() { return blobs; }
    public String getSecondaryBlobs() { return secondaryBlobs; }
    public Boolean getZip() { return ZmBoolean.toBool(zip); }
    public Boolean getZipStore() { return ZmBoolean.toBool(zipStore); }
    public FileCopierSpec getFileCopier() { return fileCopier; }
    public List<Name> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("method", method)
            .add("target", target)
            .add("label", label)
            .add("before", before)
            .add("sync", sync)
            .add("searchIndex", searchIndex)
            .add("blobs", blobs)
            .add("secondaryBlobs", secondaryBlobs)
            .add("zip", zip)
            .add("zipStore", zipStore)
            .add("fileCopier", fileCopier)
            .add("accounts", accounts);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
