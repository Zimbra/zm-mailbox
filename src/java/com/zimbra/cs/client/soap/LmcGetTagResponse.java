/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.client.soap;

import java.util.*;

import com.zimbra.cs.client.*;

public class LmcGetTagResponse extends LmcSoapResponse {

    // for storing the returned tags
    private ArrayList mTags;

    public LmcTag[] getTags() {
        if (mTags == null || mTags.size() == 0)
        	return null;
        LmcTag tags[] = new LmcTag[mTags.size()];
        return (LmcTag []) mTags.toArray(tags);
    }
    
    public void setTags(ArrayList a) { mTags = a; }
}
