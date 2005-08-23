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

package com.zimbra.cs.client;

public class LmcContactAttr {
    
    private String mAttrName;
    private String mID;
    private String mRef;
    private String mAttrData;

    public LmcContactAttr(String attrName,
                          String id,
                          String ref,
                          String attrData)
    {
        mAttrName = attrName;
        mID = id;
        mRef = ref;
        mAttrData = attrData;
    }
    
    public String getAttrName() { return mAttrName; }
    
    public String getID() { return mID; }
    
    public String getRef() { return mRef; }
    
    public String getAttrData() { return mAttrData; }
}