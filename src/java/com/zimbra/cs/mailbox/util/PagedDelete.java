/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.util.TypedIdList.ItemInfo;

/**
 * @author psurana
 * @since 2015
 */
public class PagedDelete {
    private Multimap<Integer, Integer> mSequence2ItemIds;
    private Multimap<Integer, MailItem.Type> mItemIds2Type = null;
    private Multimap<MailItem.Type, Integer> mType2ItemIds = null;
    private boolean typedDeletes;
    private int mCutOffSequence = 0;
    private int mLastItemId = -1;
    private boolean mDeleteOverFlow;

    public PagedDelete(TypedIdList tombstones, boolean typedDeletes) {
        this.mSequence2ItemIds = TreeMultimap.create();
        this.typedDeletes = typedDeletes;
        if (typedDeletes) {
            this.mItemIds2Type = TreeMultimap.create();
        }
        typedIdListToMultiMap(tombstones, typedDeletes);
    }

    public int getCutOffModsequnce() {
        return mCutOffSequence;
    }

    public int getLastItemId() {
        return mLastItemId;
    }

    public boolean isDeleteOverFlow() {
        return mDeleteOverFlow;
    }

     public Collection<Integer> getAllIds () {
        return mSequence2ItemIds.values();
     }

     //Inverting ItemIds2Type to Type2ItemId.   
     public Multimap< MailItem.Type,Integer> getTypedItemIds() {
         Iterator<Entry<Integer, MailItem.Type>> iterator = mItemIds2Type.entries().iterator();
         if(mType2ItemIds == null) {
             mType2ItemIds = TreeMultimap.create();
         }
         while (iterator.hasNext()) {
             Entry<Integer, MailItem.Type> e = iterator.next();
             mType2ItemIds.put(e.getValue(), e.getKey());
             iterator.remove();
         }
         return mType2ItemIds;
      }

     //Moving TypedIdList into MultiMaps. TypedIdList will be empty at the end.
    public void typedIdListToMultiMap(TypedIdList tombstones, boolean typedDeletes) {
        if(tombstones != null && !tombstones.isEmpty()) {
            Iterator<Map.Entry<MailItem.Type, List<ItemInfo>>> iterator = tombstones.iterator();
            while (iterator.hasNext()) {
                Map.Entry<MailItem.Type, List<TypedIdList.ItemInfo>> entry = iterator.next();
                for (TypedIdList.ItemInfo iinfo : entry.getValue()) {
                    this.mSequence2ItemIds.put(iinfo.getModSequence(), iinfo.getId());
                    if (typedDeletes) {
                        this.mItemIds2Type.put(iinfo.getId(), entry.getKey());
                    }
                }
                iterator.remove();
            }
        }
    }

    private void removeSequences(List<Integer> listRemoveSequences) {
        if (!listRemoveSequences.isEmpty()) {
            for (Integer seq : listRemoveSequences) {
                if(typedDeletes) {
                    for (Integer id : mSequence2ItemIds.get(seq)) {
                        mItemIds2Type.removeAll(id);
                    }
                }
                mSequence2ItemIds.removeAll(seq);
            }
        }
    }

    private void removeItemIds (List<Integer> listRemoveItemIds, int modSequence) {
        if (!listRemoveItemIds.isEmpty()) {
            for (Integer itemId : listRemoveItemIds) {
                mSequence2ItemIds.remove(modSequence, itemId);
                if (typedDeletes) {
                    mItemIds2Type.removeAll(itemId);
                }
            }
        }
    }

    public void trimDeletesTillPageLimit(int limit) {
        if (mSequence2ItemIds.size() > limit) {
            this.mDeleteOverFlow = true;
            int size = 0;
            List<Integer> removeSequences = new ArrayList<Integer>();
            List<Integer> removeItemIds = null;
            int lastSequence = 0;
            int lastItemId = 0;
            for (int sequence : mSequence2ItemIds.keySet()) {
                int currentSize = 0;
                if (size <= limit) {
                    currentSize = mSequence2ItemIds.get(sequence).size();
                    if ((size + currentSize) > limit) {
                        List<Integer> ids =  new ArrayList<Integer>(mSequence2ItemIds.get(sequence));
                        Collections.sort(ids);
                        if (removeItemIds == null) {
                            removeItemIds = new ArrayList<>(ids.size());
                        }
                        for (Integer id : ids) {
                            size++;
                            if(size > limit) {
                                if (lastSequence == 0) {
                                    lastSequence = sequence; //cut off sequence.
                                    lastItemId = id; // cut off item id.
                                }
                                removeItemIds.add(id);
                            }
                        }
                    } else {
                        size = size + currentSize;
                    }
                } else {
                    removeSequences.add(sequence);
                }
            }
            removeSequences(removeSequences);
            removeItemIds(removeItemIds, lastSequence);
            this.mCutOffSequence = lastSequence;
            this.mLastItemId = lastItemId;
        } else {
            this.mDeleteOverFlow = false;
        }
    }

    public void removeBeforeCutoff(int itemCutoff, int preSeq) {
        List<Integer> removeSequences = new ArrayList<>();
        List<Integer> removeItemIds = null;
        int lastSequence = 0;
        for (int sequence : this.mSequence2ItemIds.keySet()) {
            if(sequence < preSeq) {
                removeSequences.add(sequence);
            } else {
                List<Integer> ids =  new ArrayList<Integer>(mSequence2ItemIds.get(sequence));
                Collections.sort(ids);
                lastSequence = sequence;
                for (Integer id : ids) {
                    if(id < itemCutoff) {
                        if (removeItemIds == null) {
                            removeItemIds = new ArrayList<Integer>(ids.size());
                        }
                        removeItemIds.add(id);
                    }
                }
                break;
            }
        }
        removeSequences(removeSequences);
        if (removeItemIds != null) {
            removeItemIds(removeItemIds, lastSequence);
        }
    }

    public void removeAfterCutoff(int cutOffSequence) {
        List<Integer> removeSequences = new ArrayList<>();
        for (int sequence : this.mSequence2ItemIds.keySet()) {
            if (sequence > cutOffSequence) {
                removeSequences.add(sequence);
            }
        }
        if (!removeSequences.isEmpty()) {
            mDeleteOverFlow = true;
            this.mCutOffSequence = removeSequences.get(0);
            List<Integer> ids =  new ArrayList<Integer>(mSequence2ItemIds.get(this.mCutOffSequence));
            Collections.sort(ids);
            this.mLastItemId = ids.get(0);
            removeSequences(removeSequences);
            this.mCutOffSequence = cutOffSequence + 1;
        }
    }

}
