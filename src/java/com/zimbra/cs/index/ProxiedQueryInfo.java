package com.zimbra.cs.index;

import com.zimbra.soap.Element;

public class ProxiedQueryInfo implements QueryInfo {
    
    private Element mElt; 
    
    ProxiedQueryInfo(Element e) {
        mElt = e;
        mElt.detach();
    }

    public Element toXml(Element parent) {
        parent.addElement(mElt);
        return mElt;
    }
    
    public String toString() {
        return mElt.toString();
    }

    
}
