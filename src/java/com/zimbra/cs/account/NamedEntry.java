/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;

import java.util.Map;

public abstract class NamedEntry extends Entry implements Comparable {

    protected String mName;
    protected String mId;

    public interface Visitor  {
        public void visit(NamedEntry entry) throws ServiceException;
    }

    protected NamedEntry(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults) {
        super(attrs, defaults);
        mName = name;
        mId = id;
    }

    public String getLabel() {
        return getName();
    }
    
    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public int compareTo(Object obj) {
        if (!(obj instanceof NamedEntry))
            return 0;
        NamedEntry other = (NamedEntry) obj;
        return getName().compareTo(other.getName());
    }
    
    public synchronized String toString() {
        return String.format("[%s %s]", getClass().getName(), getName());
    }

}
