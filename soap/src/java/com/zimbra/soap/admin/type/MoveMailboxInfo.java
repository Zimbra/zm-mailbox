/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.type.ZmBoolean;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public class MoveMailboxInfo {

    /**
     * @zm-api-field-tag account-email-address
     * @zm-api-field-description Account email address
     */
    @XmlAttribute(name=BackupConstants.A_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-tag target-svr-hostname
     * @zm-api-field-description Hostname of target server
     */
    @XmlAttribute(name=BackupConstants.A_TARGET /* dest */, required=true)
    private String target;

    /**
     * @zm-api-field-tag source-svr-hostname
     * @zm-api-field-description Hostname of source server
     */
    @XmlAttribute(name=BackupConstants.A_SOURCE /* src */, required=true)
    private String source;

    /**
     * @zm-api-field-tag include|exclude|config
     * @zm-api-field-description Option to include/exclude blobs in a move - <b>include|exclude|config</b>
     * <br />
     * Default value is "config", to use the configured value.  "include" or "exclude" overrides the configuration
     */
    @XmlAttribute(name=BackupConstants.A_BLOBS /* blobs */, required=false)
    private String blobs;

    /**
     * @zm-api-field-tag include|exclude|config
     * @zm-api-field-description Option to include/exclude secondary blobs in a move - <b>include|exclude|config</b>
     * <br />
     * Default value is "config", to use the configured value.  "include" or "exclude" overrides the configuration
     * <br />
     * Meaningful only when blobs isn't excluded.
     */
    @XmlAttribute(name=BackupConstants.A_SECONDARY_BLOBS /* secondaryBlobs */, required=false)
    private String secondaryBlobs;

    /**
     * @zm-api-field-tag include|exclude|config
     * @zm-api-field-description Option to include/exclude searchIndex in a move - <b>include|exclude|config</b>
     * <br />
     * Default value is "config", to use the configured value.  "include" or "exclude" overrides the configuration
     */
    @XmlAttribute(name=BackupConstants.A_SEARCH_INDEX /* searchIndex */, required=false)
    private String searchIndex;

    /**
     * @zm-api-field-tag max-syncs
     * @zm-api-field-description Maximum number of syncs.  Default is 10
     */
    @XmlAttribute(name=BackupConstants.A_MAX_SYNCS /* maxSyncs */, required=false)
    private Integer maxSyncs;

    /**
     * @zm-api-field-tag sync-finish-threshold-millisecs
     * @zm-api-field-description Sync finish threshold.  Default is 30000 (30 seconds)
     */
    @XmlAttribute(name=BackupConstants.A_SYNC_FINISH_THRESHOLD /* syncFinishThreshold */, required=false)
    private Long syncFinishThreshold;

    /**
     * @zm-api-field-tag sync
     * @zm-api-field-description If set, run synchronously; command doesn't return until move is finished
     */
    @XmlAttribute(name=BackupConstants.A_SYNC /* sync */, required=false)
    private ZmBoolean sync;

    /**
     * @zm-api-field-tag ngMigration
     * @zm-api-field-description If set, run synchronously; command doesn't return until move is finished
     */
    @XmlAttribute(name=BackupConstants.A_NG_MIGRATION /* sync */, required=false)
    private ZmBoolean ngMigration;

    /**
     * @zm-api-field-tag skipRemoteLockout
     * @zm-api-field-description If set, do not lockout the remote mailbox. Used for backward compatibility when moving from legacy server versions
     */
    @XmlAttribute(name=BackupConstants.A_SKIP_REMOTE_LOCKOUT, required=false)
    private ZmBoolean skipRemoteLockout;

    /**
     * @zm-api-field-tag skipMemcachePurge
     * @zm-api-field-description If set, do not automatically purge route info from memcache. Provided for special cases where memcache/proxy is handled outside the move process
     */
    @XmlAttribute(name=BackupConstants.A_SKIP_MEMCACHE_PURGE, required=false)
    private ZmBoolean skipMemcachePurge;

    private MoveMailboxInfo() {
    }

    private MoveMailboxInfo(String name, String target, String source) {
        setName(name);
        setTarget(target);
        setSource(source);
    }

    public static MoveMailboxInfo createForNameTargetAndSource(String name, String target, String source) {
        return new MoveMailboxInfo(name, target, source);
    }

    public void setName(String name) { this.name = name; }
    public void setTarget(String target) { this.target = target; }
    public void setSource(String source) { this.source = source; }
    public void setBlobs(String blobs) { this.blobs = blobs; }
    public void setSecondaryBlobs(String secondaryBlobs) { this.secondaryBlobs = secondaryBlobs; }
    public void setSearchIndex(String searchIndex) { this.searchIndex = searchIndex; }
    public void setMaxSyncs(Integer maxSyncs) { this.maxSyncs = maxSyncs; }
    public void setSyncFinishThreshold(Long syncFinishThreshold) { this.syncFinishThreshold = syncFinishThreshold; }
    public void setSync(Boolean sync) { this.sync = ZmBoolean.fromBool(sync); }
    public void setNgMigration(Boolean ngMigration) { this.ngMigration = ZmBoolean.fromBool(ngMigration); }
    public void setSkipRemoteLockout(Boolean skipRemoteLockout) { this.skipRemoteLockout = ZmBoolean.fromBool(skipRemoteLockout); }
    public void setSkipMemcachePurge(Boolean skipMemcachePurge) { this.skipMemcachePurge = ZmBoolean.fromBool(skipMemcachePurge); }
    public String getName() { return name; }
    public String getTarget() { return target; }
    public String getSource() { return source; }
    public String getBlobs() { return blobs; }
    public String getSecondaryBlobs() { return secondaryBlobs; }
    public String getSearchIndex() { return searchIndex; }
    public Integer getMaxSyncs() { return maxSyncs; }
    public Long getSyncFinishThreshold() { return syncFinishThreshold; }
    public Boolean getSync() { return ZmBoolean.toBool(sync); }
    public Boolean getNgMigration() { return ZmBoolean.toBool(ngMigration); }

    public Boolean getSkipRemoteLockout() { return ZmBoolean.toBool(skipRemoteLockout); }
    public Boolean getSkipMemcachePurge() { return ZmBoolean.toBool(skipMemcachePurge); }


    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("target", target)
            .add("source", source)
            .add("blobs", blobs)
            .add("secondaryBlobs", secondaryBlobs)
            .add("searchIndex", searchIndex)
            .add("maxSyncs", maxSyncs)
            .add("syncFinishThreshold", syncFinishThreshold)
            .add("sync", sync)
            .add("ngMigration", ngMigration)
            .add("skipRemoteLockout", skipRemoteLockout)
            .add("skipMemcachePurge", skipMemcachePurge);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
