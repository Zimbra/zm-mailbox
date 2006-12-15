/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import com.zimbra.soap.Element;

public class WildcardExpansionQueryInfo implements  QueryInfo {
    
    private String mStr;
    private int mNumExpanded;
    private boolean mExpandedAll;
    
    WildcardExpansionQueryInfo(String baseStr, int numExpanded, boolean expandedAll) {
        mStr = baseStr;
        mNumExpanded = numExpanded;
        mExpandedAll = expandedAll;
    }
    
    public Element toXml(Element parent) {
        Element qinfo = parent.addElement("wildcard");
        qinfo.addAttribute("str", mStr);
        qinfo.addAttribute("expanded", mExpandedAll);
        qinfo.addAttribute("numExpanded", mNumExpanded);
        return qinfo;
    }
    
    public String toString() {
        return "WILDCARD("+mStr+","+mNumExpanded+","+ (mExpandedAll ? "ALL" : "PARTIAL")+")";
    }
}
