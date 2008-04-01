/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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
 * Created on Jun 19, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.index.MailboxIndex.BrowseTerm;

/**
 * @author schemers
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BrowseResult {
    private List<BrowseTerm> mResult;

    public BrowseResult() {
        mResult = new ArrayList<BrowseTerm>();
    }
    
    public List<BrowseTerm> getResult() {
        return mResult;
    }
    
    public static class DomainItem extends BrowseTerm {
        static final int F_FROM = 0x1;
        static final int F_TO = 0x2;
        static final int F_CC = 0x4;
        private static final String[] sHeaderFlags = {"", "f", "t", "ft", "c", "fc", "tc", "ftc"}; 

        int mFlags;
        
        DomainItem(BrowseTerm domain) {
            super(domain.term, domain.freq);
        }
        
        public String getDomain() {
            return term;
        }
        
        void addFlag(int flag) {
            mFlags |= flag;
        }
        
        public String getHeaderFlags() {
            return sHeaderFlags[mFlags];
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
