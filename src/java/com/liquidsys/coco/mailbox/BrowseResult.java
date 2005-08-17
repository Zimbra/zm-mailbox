/*
 * Created on Jun 19, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.liquidsys.coco.mailbox;

import java.util.ArrayList;

/**
 * @author schemers
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BrowseResult {
    private ArrayList mResult;

	public BrowseResult() {
	    mResult = new ArrayList();
	}
	
    void add(Object item) {
        mResult.add(item);
    }
    
    public ArrayList getResult() {
        return mResult;
    }
    
    public static class DomainItem {
        static final int F_FROM = 0x1;
        static final int F_TO = 0x2;
        static final int F_CC = 0x4;
        private static final String[] sHeaderFlags = {"", "f", "t", "ft", "c", "fc", "tc", "ftc"}; 

        String mDomain;
        int mFlags;
        
        DomainItem(String domain) {
            mDomain = domain;
        }
        
        void setFlag(int flag) {
            mFlags |= flag;
        }
        
        public String getHeaderFlags() {
            return sHeaderFlags[mFlags];
        }
        
        public String getDomain() {
            return mDomain;
        }
        
        public boolean isFrom() {
            return (mFlags & F_FROM) != 0;
        }
        
        public boolean isTo() {
            return (mFlags & F_TO) != 0;
        }
        
        public boolean isCc() {
            return (mFlags & F_CC) != 0;            
        }        
    }
}
