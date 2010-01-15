/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;

// for CalDAV
// AccountCtags is a convenience class that aggregates all ctag-related info for an account.
// It combines data from CalListCache and CtagInfoCache.
public class AccountCtags {
    private CalList mCalList;
    private Map<Integer /* cal folder id */, CtagInfo> mCtagMap;

    AccountCtags(CalList calList, Collection<CtagInfo> ctags) {
        mCalList = calList;
        mCtagMap = new HashMap<Integer, CtagInfo>(ctags.size());
        for (CtagInfo ctag : ctags) {
            mCtagMap.put(ctag.getId(), ctag);
        }
    }

    /**
     * Returns a version tag that changes on any calendar-related update in the account.
     * @return
     */
    public String getVersion() { return mCalList.getVersion(); }

    /**
     * Returns the ctag for calendar folder with given id.
     * @param calFolderId
     * @return
     */
    public CtagInfo getById(int calFolderId) {
        return mCtagMap.get(calFolderId);
    }

    /**
     * Returns the ctag for calendar folder with given path.
     * @param path
     * @return
     */
    public CtagInfo getByPath(String path) {
        for (CtagInfo calInfo : mCtagMap.values()) {
            if (path.equalsIgnoreCase(calInfo.getPath()))
                return calInfo;
        }
        return null;
    }

    /**
     * Returns ctags for calendar folder under the given parent folder.
     * @param parentFolderId
     * @return
     */
    public Collection<CtagInfo> getChildren(int parentFolderId) {
        Collection<CtagInfo> allFolders = mCtagMap.values();
        if (parentFolderId == Mailbox.ID_FOLDER_USER_ROOT)
            return allFolders;
        List<CtagInfo> children = new ArrayList<CtagInfo>();
        for (CtagInfo calInfo : allFolders) {
            if (calInfo.getFolderId() == parentFolderId)
                children.add(calInfo);
        }
        return children;
    }
}
