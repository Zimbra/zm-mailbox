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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import com.zimbra.soap.type.ZmBoolean;

import com.zimbra.common.soap.BackupConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class MoveMailboxInfo {

    @XmlAttribute(name=BackupConstants.A_NAME /* name */, required=true)
    private String name;

    @XmlAttribute(name=BackupConstants.A_TARGET /* dest */, required=true)
    private String target;

    @XmlAttribute(name=BackupConstants.A_SOURCE /* src */, required=true)
    private String source;

    @XmlAttribute(name=BackupConstants.A_BLOBS /* blobs */, required=false)
    private String blobs;

    @XmlAttribute(name=BackupConstants.A_SECONDARY_BLOBS /* secondaryBlobs */, required=false)
    private String secondaryBlobs;

    @XmlAttribute(name=BackupConstants.A_SEARCH_INDEX /* searchIndex */, required=false)
    private String searchIndex;

    @XmlAttribute(name=BackupConstants.A_MAX_SYNCS /* maxSyncs */, required=false)
    private Integer maxSyncs;

    @XmlAttribute(name=BackupConstants.A_SYNC_FINISH_THRESHOLD /* syncFinishThreshold */, required=false)
    private Long syncFinishThreshold;

    @XmlAttribute(name=BackupConstants.A_SYNC /* sync */, required=false)
    private ZmBoolean sync;

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
    public String getName() { return name; }
    public String getTarget() { return target; }
    public String getSource() { return source; }
    public String getBlobs() { return blobs; }
    public String getSecondaryBlobs() { return secondaryBlobs; }
    public String getSearchIndex() { return searchIndex; }
    public Integer getMaxSyncs() { return maxSyncs; }
    public Long getSyncFinishThreshold() { return syncFinishThreshold; }
    public Boolean getSync() { return ZmBoolean.toBool(sync); }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("target", target)
            .add("source", source)
            .add("blobs", blobs)
            .add("secondaryBlobs", secondaryBlobs)
            .add("searchIndex", searchIndex)
            .add("maxSyncs", maxSyncs)
            .add("syncFinishThreshold", syncFinishThreshold)
            .add("sync", sync);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
