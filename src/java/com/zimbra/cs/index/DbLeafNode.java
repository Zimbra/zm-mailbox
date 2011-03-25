/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.index;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbSearchConstraints;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.util.ItemId;

class DbLeafNode extends DbSearchConstraints implements IConstraints {

    /**
     * Set by the forceHasSpamTrashSetting() API
     *
     * True if we have a SETTING pertaining to Spam/Trash.  This doesn't
     * necessarily mean we actually have "in trash" or something, it just
     * means that we've got something set which means we shouldn't add
     * the default "not in:trash and not in:junk" thing.
     */
    protected boolean mHasSpamTrashSetting = false;

    @Override
    public Object clone() throws CloneNotSupportedException {
        DbLeafNode toRet = (DbLeafNode)super.clone();

        // make sure we cloned folders instead of just copying them!
        assert(toRet.folders != folders);

        return toRet;
    }

    @Override
    public void ensureSpamTrashSetting(Mailbox mbox, List<Folder> trashSpamFolders) {
        if (!mHasSpamTrashSetting) {
            for (Folder f : trashSpamFolders) {
                excludeFolders.add(f);
            }
            mHasSpamTrashSetting = true;
        }
    }

    @Override
    public IConstraints andIConstraints(IConstraints other) {
        switch(other.getNodeType()) {
            case AND:
                return other.andIConstraints(this);
            case OR:
                return other.andIConstraints(this);
            case LEAF:
                if (other.hasSpamTrashSetting())
                    forceHasSpamTrashSetting();

                if (other.hasNoResults()) {
                    noResults = true;
                }
                andConstraints((DbLeafNode)other);
                return this;
        }
        assert(false);
        return null;
    }

    @Override
    public IConstraints orIConstraints(IConstraints other) {
        if (other.getNodeType() == NodeType.OR) {
            return other.orIConstraints(this);
        } else {
            IConstraints top = new DbOrNode();
            top = top.orIConstraints(this);
            top = top.orIConstraints(other);
            return top;
        }
    }

    @Override
    public boolean hasSpamTrashSetting() {
        return mHasSpamTrashSetting;
    }

    @Override
    public void forceHasSpamTrashSetting() {
        mHasSpamTrashSetting = true;
    }

    @Override
    public boolean hasNoResults() {
        return noResults;
    }

    @Override
    public boolean tryDbFirst(Mailbox mbox) throws ServiceException {
        return (convId != 0 || (tags != null && tags.contains(mbox.getFlagById(Flag.ID_UNREAD))));
    }

    @Override
    public void setTypes(Set<MailItem.Type> types) {
        this.types = types;
        if (types.isEmpty()) {
            noResults = true;
        }
    }

    @Override
    public String toQueryString() {
        if (noResults)
            return "-is:anywhere";

        return toString();
    }

    void addItemIdClause(Integer itemId, boolean truth) {
        if (truth) {
            if (!itemIds.contains(itemId)) {
                //
                // {1} AND {-1} AND {1} == no-results
                //  ... NOT {1} (which could happen if you removed from both arrays on combining!)
                if (prohibitedItemIds== null || !prohibitedItemIds.contains(itemId)) {
                    itemIds.add(itemId);
                }
            }
        } else {
            if (!prohibitedItemIds.contains(itemId)) {
                //
                // {1} AND {-1} AND {1} == no-results
                //  ... NOT {1} (which could happen if you removed from both arrays on combining!)
                if (itemIds != null && itemIds.contains(itemId)) {
                    itemIds.remove(itemId);
                }
                prohibitedItemIds.add(itemId);
            }
        }
    }

    void addRemoteItemIdClause(ItemId itemId, boolean truth) {
        if (truth) {
            if (!remoteItemIds.contains(itemId)) {
                //
                // {1} AND {-1} AND {1} == no-results
                //  ... NOT {1} (which could happen if you removed from both arrays on combining!)
                if (prohibitedRemoteItemIds== null || !prohibitedRemoteItemIds.contains(itemId)) {
                    remoteItemIds.add(itemId);
                }
            }
        } else {
            if (!prohibitedRemoteItemIds.contains(itemId)) {
                //
                // {1} AND {-1} AND {1} == no-results
                //  ... NOT {1} (which could happen if you removed from both arrays on combining!)
                if (remoteItemIds != null && remoteItemIds.contains(itemId)) {
                    remoteItemIds.remove(itemId);
                }
                prohibitedRemoteItemIds.add(itemId);
            }
        }
    }

    void addDateClause(long lowest, boolean lowestEqual, long highest, boolean highestEqual, boolean truth)  {
        DbSearchConstraints.NumericRange intv = new DbSearchConstraints.NumericRange();
        intv.lowest = lowest;
        intv.lowestEqual = lowestEqual;
        intv.highest = highest;
        intv.highestEqual = highestEqual;
        intv.negated = !truth;

        dates.add(intv);
    }

    void addCalStartDateClause(long lowest, boolean lowestEqual, long highest, boolean highestEqual, boolean truth)  {
        DbSearchConstraints.NumericRange intv = new DbSearchConstraints.NumericRange();
        intv.lowest = lowest;
        intv.lowestEqual = lowestEqual;
        intv.highest = highest;
        intv.highestEqual = highestEqual;
        intv.negated = !truth;

        calStartDates.add(intv);
    }

    void addCalEndDateClause(long lowest, boolean lowestEqual, long highest, boolean highestEqual, boolean truth)  {
        DbSearchConstraints.NumericRange intv = new DbSearchConstraints.NumericRange();
        intv.lowest = lowest;
        intv.lowestEqual = lowestEqual;
        intv.highest = highest;
        intv.highestEqual = highestEqual;
        intv.negated = !truth;

        calEndDates.add(intv);
    }

    void addModSeqClause(long lowest, boolean lowestEqual, long highest, boolean highestEqual, boolean truth)  {
        DbSearchConstraints.NumericRange intv = new DbSearchConstraints.NumericRange();
        intv.lowest = lowest;
        intv.lowestEqual = lowestEqual;
        intv.highest = highest;
        intv.highestEqual = highestEqual;
        intv.negated = !truth;

        this.modified.add(intv);
    }


    void addConvCountClause(long lowest, boolean lowestEqual, long highest, boolean highestEqual, boolean truth)  {
        DbSearchConstraints.NumericRange intv = new DbSearchConstraints.NumericRange();
        intv.lowest = lowest;
        intv.lowestEqual = lowestEqual;
        intv.highest = highest;
        intv.highestEqual = highestEqual;
        intv.negated = !truth;

        convCounts.add(intv);
    }

    void addSizeClause(long lowestSize, long highestSize, boolean truth)  {
        DbSearchConstraints.NumericRange intv = new DbSearchConstraints.NumericRange();
        intv.lowest = lowestSize;
        intv.highest = highestSize;
        intv.negated = !truth;

        sizes.add(intv);
    }

    void addSubjectRelClause(String lowest, boolean lowestEqual, String highest, boolean highestEqual, boolean truth) {
        DbSearchConstraints.StringRange intv = new DbSearchConstraints.StringRange();
        intv.lowest = lowest;
        intv.lowestEqual = lowestEqual;
        intv.highest = highest;
        intv.highestEqual = highestEqual;
        intv.negated = !truth;

        subjectRanges.add(intv);
    }

    void addSenderRelClause(String lowest, boolean lowestEqual, String highest, boolean highestEqual, boolean truth) {
        DbSearchConstraints.StringRange intv = new DbSearchConstraints.StringRange();
        intv.lowest = lowest;
        intv.lowestEqual = lowestEqual;
        intv.highest = highest;
        intv.highestEqual = highestEqual;
        intv.negated = !truth;

        senderRanges.add(intv);
    }

    void setFromContact(boolean bool) {
        fromContact = bool;
    }

    void addConvId(int cid, boolean truth) {

        if (truth) {
            if (prohibitedConvIds.contains(cid)) {
                noResults = true;
            }

            if (convId == 0) {
                convId = cid;
            } else {
                ZimbraLog.search.debug("Query requested two conflicting convIDs, this is now a no-results-query");
                convId = Integer.MAX_VALUE;
                noResults = true;
            }
        } else {
            if (convId == cid) {
                noResults = true;
            }
            prohibitedConvIds.add(cid);
        }
    }

    void addRemoteConvId(ItemId cid, boolean truth) {

        if (truth) {
            if (prohibitedRemoteConvIds.contains(cid)) {
                noResults = true;
            }

            if (remoteConvId == null) {
                remoteConvId = cid;
            } else {
                ZimbraLog.search.debug("Query requested two conflicting Remote convIDs, this is now a no-results-query");
                remoteConvId = new ItemId(cid.getAccountId(), Integer.MAX_VALUE);
                noResults = true;
            }
        } else {
            if (remoteConvId.equals(cid)) {
                noResults = true;
            }
            prohibitedRemoteConvIds.add(cid);
        }
    }


    /**
     * For remote folder
     */
    void addInRemoteFolderClause(ItemId id, String subfolderPath, boolean includeSubfolders, boolean truth)
    {
        if (truth) {
            if ((remoteFolders.size() > 0 && !remoteFolders.contains(id)) || excludeRemoteFolders.contains(id)) {
                ZimbraLog.search.debug("AND of conflicting remote folders, no-results-query");
                noResults = true;
            }
            remoteFolders.clear();
            remoteFolders.add(new DbSearchConstraints.RemoteFolderDescriptor(id, subfolderPath, includeSubfolders));
            forceHasSpamTrashSetting();
        } else {
            if (remoteFolders.contains(id)) {
                remoteFolders.remove(id);
                if (remoteFolders.size() == 0) {
                    ZimbraLog.search.debug("AND of conflicting remote folders, no-results-query");
                    noResults = true;
                }
            }
            excludeRemoteFolders.add(new DbSearchConstraints.RemoteFolderDescriptor(id, subfolderPath, includeSubfolders));
        }
    }

    void addInClause(Folder folder, boolean truth) {
        if (truth) {
            if ((folders.size() > 0 && !folders.contains(folder)) || excludeFolders.contains(folder)) {
                ZimbraLog.search.debug("AND of conflicting folders, no-results-query");
                noResults = true;
            }
            folders.clear();
            folders.add(folder);
            forceHasSpamTrashSetting();
        } else {
            if (folders.contains(folder)) {
                folders.remove(folder);
                if (folders.size() == 0) {
                    ZimbraLog.search.debug("AND of conflicting folders, no-results-query");
                    noResults = true;
                }
            }
            excludeFolders.add(folder);

            int fid = folder.getId();
            if (fid == Mailbox.ID_FOLDER_TRASH || fid == Mailbox.ID_FOLDER_SPAM) {
                forceHasSpamTrashSetting();
            }
        }
    }

    void addAnyFolderClause(boolean truth) {
        // support for "is:anywhere" basically as a way to get around
        // the trash/spam autosetting
        forceHasSpamTrashSetting();

        if (!truth) {
            // if they are weird enough to say "NOT is:anywhere" then we
            // just make it a no-results-query.
            ZimbraLog.search.debug("addAnyFolderClause(FALSE) called -- changing to no-results-query.");
            noResults = true;
        }
    }

    void addTagClause(Tag tag, boolean truth) {
        ZimbraLog.search.debug("AddTagClause(%s,%b)", tag, truth);
        if (truth) {
            if (excludeTags != null && excludeTags.contains(tag)) {
                ZimbraLog.search.debug("TAG and NOT TAG = no results");
                noResults = true;
            }
            if (tags == null)
                tags = new HashSet<Tag>();
            tags.add(tag);
        } else {
            if (tags != null && tags.contains(tag)) {
                ZimbraLog.search.debug("TAG and NOT TAG = no results");
                noResults = true;
            }
            if (excludeTags == null)
                excludeTags = new HashSet<Tag>();
            excludeTags.add(tag);
        }
    }
}
