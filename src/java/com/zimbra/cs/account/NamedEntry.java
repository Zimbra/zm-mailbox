/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
    
    public interface CheckRight {
        boolean allow(NamedEntry entry) throws ServiceException;
    }

    protected NamedEntry(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults, Provisioning prov) {
        super(attrs, defaults, prov);
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
