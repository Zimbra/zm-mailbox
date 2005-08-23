/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on Jun 19, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.mailbox;

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
