/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * for group membership caching
 * 
 * Contains only "upward" memberships.  i.e. all direct and indirect groups this object is a member of
 * 
 * @author pshao
 *
 */
public class Group extends MailTarget {
    
    // ids of all direct and indirect groups this object is a member of
    private Map<String, String> mInGroups;
    
    public Group(String name, String id, List<DistributionList> inGroups) {
        super(name, id, null, null);
        
        if (inGroups != null) {
            mInGroups = new HashMap<String, String>();
            for (DistributionList dl : inGroups)
                mInGroups.put(dl.getId(), dl.getName());
            mInGroups = Collections.unmodifiableMap(mInGroups);
        }
    }
    
    public boolean isMemberOf(String id) {
        if (mInGroups == null)
            return false;
        else
            return (mInGroups.get(id) != null);
    }

}
