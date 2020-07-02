package com.zimbra.common.mailbox;

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncStateBase implements Serializable {

    private static final long serialVersionUID = 1L;
    protected String deviceId;
    protected String accountID;
    protected int metadataVersion;
    protected String mailItemTypeAndId; // Email:2
    protected List<String> collectionKeys;
    protected int syncKeyClientConfirmed; // latest syncKey from client
    protected int syncKeyServerLatest; // server's latest syncKey
    protected int modSequenceConfirmed;
    protected int modSequenceNew;
    protected long cutoffConfirmed = -1;
    protected long cutoffNew = -1;
    protected int syncRedoCount = 0; // number of times client sync version
                                     // stuck
    protected int penaltyEndSeq = -1; // the last mod sequence while in penalty
    protected int syncRetryCount = 0; // number of retry for out of sync client
    // all of the sorted collections are in item id descending order
    protected SortedMap<Integer, Integer> onClientItemId2modSeq;
    // tracked by server
    protected int initialSyncItemIdCutoff = Integer.MAX_VALUE; // for initial
                                                               // sync
    protected SortedMap<Integer, Integer> toAddItemId2modSeq;
    protected int onClientItemIdCutoff;
    protected SortedMap<Integer, Integer> toChangeItemId2modSeq;
    protected SortedSet<Integer> toDeleteItemIds;
    protected SortedMap<Integer, String> priorityOnClientItemId2modSeq;
    protected SortedMap<Integer, String> priorityToAddItemId2modSeq;
    protected SortedMap<Integer, String> priorityToChangeItemId2modSeq;
    protected SortedSet<Integer> priorityToDeleteItemIds;
    protected SortedMap<Integer, Integer> exceptionItemId2modSeq;
    protected SortedSet<Integer> revertModItemIds;
    protected SortedSet<Integer> revertAddItemIds;
    // 12.1
    protected Map<Integer, String> bodyPreferences;
    protected boolean hasNoMore; // whether there's more item to sync in this
                                 // collection
    protected boolean dirty; // whether we need to save metadata
    protected boolean hasRealChanges; // whether we need to increment syncKey
    protected boolean forFolder;

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }
    
    public int getMetadataVersion() {
        return metadataVersion;
    }

    public void setMetadataVersion(int metadataVersion) {
        this.metadataVersion = metadataVersion;
    }

    public void setMailItemTypeAndId(String mailItemTypeAndId) {
        this.mailItemTypeAndId = mailItemTypeAndId;
    }

    public void setCollectionKeys(List<String> collectionKeys) {
        this.collectionKeys = collectionKeys;
    }

    public void setSyncKeyClientConfirmed(int syncKeyClientConfirmed) {
        this.syncKeyClientConfirmed = syncKeyClientConfirmed;
    }

    public void setSyncKeyServerLatest(int syncKeyServerLatest) {
        this.syncKeyServerLatest = syncKeyServerLatest;
    }

    public void setModSequenceConfirmed(int modSequenceConfirmed) {
        this.modSequenceConfirmed = modSequenceConfirmed;
    }

    public void setModSequenceNew(int modSequenceNew) {
        this.modSequenceNew = modSequenceNew;
    }

    public void setCutoffConfirmed(long cutoffConfirmed) {
        this.cutoffConfirmed = cutoffConfirmed;
    }

    public void setCutoffNew(long cutoffNew) {
        this.cutoffNew = cutoffNew;
    }

    public void setSyncRedoCount(int syncRedoCount) {
        this.syncRedoCount = syncRedoCount;
    }

    public void setPenaltyEndSeq(int penaltyEndSeq) {
        this.penaltyEndSeq = penaltyEndSeq;
    }

    public void setSyncRetryCount(int syncRetryCount) {
        this.syncRetryCount = syncRetryCount;
    }

    public void setOnClientItemId2modSeq(SortedMap<Integer, Integer> onClientItemId2modSeq) {
        this.onClientItemId2modSeq = onClientItemId2modSeq;
    }

    public int getInitialSyncItemIdCutoff() {
        return initialSyncItemIdCutoff;
    }

    public void setInitialSyncItemIdCutoff(int initialSyncItemIdCutoff) {
        this.initialSyncItemIdCutoff = initialSyncItemIdCutoff;
    }

    public void setToAddItemId2modSeq(SortedMap<Integer, Integer> toAddItemId2modSeq) {
        this.toAddItemId2modSeq = toAddItemId2modSeq;
    }

    public void setOnClientItemIdCutoff(int onClientItemIdCutoff) {
        this.onClientItemIdCutoff = onClientItemIdCutoff;
    }

    public void setToChangeItemId2modSeq(SortedMap<Integer, Integer> toChangeItemId2modSeq) {
        this.toChangeItemId2modSeq = toChangeItemId2modSeq;
    }

    public void setToDeleteItemIds(SortedSet<Integer> toDeleteItemIds) {
        this.toDeleteItemIds = toDeleteItemIds;
    }

    public void
        setPriorityOnClientItemId2modSeq(SortedMap<Integer, String> priorityOnClientItemId2modSeq) {
        this.priorityOnClientItemId2modSeq = priorityOnClientItemId2modSeq;
    }

    public void
        setPriorityToAddItemId2modSeq(SortedMap<Integer, String> priorityToAddItemId2modSeq) {
        this.priorityToAddItemId2modSeq = priorityToAddItemId2modSeq;
    }

    public void
        setPriorityToChangeItemId2modSeq(SortedMap<Integer, String> priorityToChangeItemId2modSeq) {
        this.priorityToChangeItemId2modSeq = priorityToChangeItemId2modSeq;
    }

    public void setPriorityToDeleteItemIds(SortedSet<Integer> priorityToDeleteItemIds) {
        this.priorityToDeleteItemIds = priorityToDeleteItemIds;
    }

    public void setExceptionItemId2modSeq(SortedMap<Integer, Integer> exceptionItemId2modSeq) {
        this.exceptionItemId2modSeq = exceptionItemId2modSeq;
    }

    public void setRevertModItemIds(SortedSet<Integer> revertModItemIds) {
        this.revertModItemIds = revertModItemIds;
    }

    public void setRevertAddItemIds(SortedSet<Integer> revertAddItemIds) {
        this.revertAddItemIds = revertAddItemIds;
    }

    public void setBodyPreferences(Map<Integer, String> bodyPreferences) {
        this.bodyPreferences = bodyPreferences;
    }

    public void setHasNoMore(boolean hasNoMore) {
        this.hasNoMore = hasNoMore;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void setHasRealChanges(boolean hasRealChanges) {
        this.hasRealChanges = hasRealChanges;
    }

    public void setForFolder(boolean forFolder) {
        this.forFolder = forFolder;
    }

    public String getAccountID() {
        return accountID;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getMailItemTypeAndId() {
        return mailItemTypeAndId;
    }

    public List<String> getCollectionKeys() {
        return collectionKeys;
    }

    public int getSyncKeyClientConfirmed() {
        return syncKeyClientConfirmed;
    }

    public int getSyncKeyServerLatest() {
        return syncKeyServerLatest;
    }

    public int getModSequenceConfirmed() {
        return modSequenceConfirmed;
    }

    public int getModSequenceNew() {
        return modSequenceNew;
    }

    public long getCutoffConfirmed() {
        return cutoffConfirmed;
    }

    public long getCutoffNew() {
        return cutoffNew;
    }

    public int getSyncRedoCount() {
        return syncRedoCount;
    }

    public int getPenaltyEndSeq() {
        return penaltyEndSeq;
    }

    public int getSyncRetryCount() {
        return syncRetryCount;
    }

    public SortedMap<Integer, Integer> getOnClientItemId2modSeq() {
        return onClientItemId2modSeq;
    }

    public int getOnClientItemIdCutoff() {
        return onClientItemIdCutoff;
    }

    public SortedMap<Integer, Integer> getToAddItemId2modSeq() {
        return toAddItemId2modSeq;
    }

    public SortedMap<Integer, Integer> getToChangeItemId2modSeq() {
        return toChangeItemId2modSeq;
    }

    public SortedSet<Integer> getToDeleteItemIds() {
        return toDeleteItemIds;
    }

    public SortedMap<Integer, String> getPriorityOnClientItemId2modSeq() {
        return priorityOnClientItemId2modSeq;
    }

    public SortedMap<Integer, String> getPriorityToAddItemId2modSeq() {
        return priorityToAddItemId2modSeq;
    }

    public SortedMap<Integer, String> getPriorityToChangeItemId2modSeq() {
        return priorityToChangeItemId2modSeq;
    }

    public SortedSet<Integer> getPriorityToDeleteItemIds() {
        return priorityToDeleteItemIds;
    }

    public SortedMap<Integer, Integer> getExceptionItemId2modSeq() {
        return exceptionItemId2modSeq;
    }

    public SortedSet<Integer> getRevertModItemIds() {
        return revertModItemIds;
    }

    public SortedSet<Integer> getRevertAddItemIds() {
        return revertAddItemIds;
    }

    public boolean isHasNoMore() {
        return hasNoMore;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isHasRealChanges() {
        return hasRealChanges;
    }

    public boolean isForFolder() {
        return forFolder;
    }

    public Map<Integer, String> getBodyPreferences() {
        return bodyPreferences;
    }
}
