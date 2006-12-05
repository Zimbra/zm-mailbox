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
        return qinfo;
    }
    
    public String toString() {
        return "WILDCARD("+mStr+","+mNumExpanded+","+ (mExpandedAll ? "ALL" : "PARTIAL")+")";
    }
}
