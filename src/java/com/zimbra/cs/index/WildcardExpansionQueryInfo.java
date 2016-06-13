/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.index;

import com.zimbra.common.soap.Element;

public final class WildcardExpansionQueryInfo implements QueryInfo {

    private String mStr;
    private int mNumExpanded;
    private boolean mExpandedAll;

    public WildcardExpansionQueryInfo(String baseStr, int numExpanded, boolean expandedAll) {
        mStr = baseStr;
        mNumExpanded = numExpanded;
        mExpandedAll = expandedAll;
    }

    @Override
    public Element toXml(Element parent) {
        Element qinfo = parent.addElement("wildcard");
        qinfo.addAttribute("str", mStr);
        qinfo.addAttribute("expanded", mExpandedAll);
        qinfo.addAttribute("numExpanded", mNumExpanded);
        return qinfo;
    }

    @Override
    public String toString() {
        return "WILDCARD(" + mStr + "," + mNumExpanded + "," +
            (mExpandedAll ? "ALL" : "PARTIAL") + ")";
    }
}
