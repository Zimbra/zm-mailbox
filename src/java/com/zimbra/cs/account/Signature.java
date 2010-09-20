/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class Signature extends AccountProperty implements Comparable {

    private static final BiMap<String, String> sAttrTypeMap = HashBiMap.create();

    static {
        sAttrTypeMap.put(Provisioning.A_zimbraPrefMailSignature, "text/plain");
        sAttrTypeMap.put(Provisioning.A_zimbraPrefMailSignatureHTML, "text/html");
    }
    
    public Signature(Account acct, String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(acct, name, id, attrs, null, prov);
    }
    
    /**
     * this should only be used internally by the server. it doesn't modify the real id, just
     * the cached one.
     * @param id
     */
    public void setId(String id) {
        mId = id;
        getRawAttrs().put(Provisioning.A_zimbraSignatureId, id);
    }
    
    public static class SignatureContent {
        private String mMimeType;
        private String mContent;
        
        public SignatureContent(String mimeType, String content) {
            mMimeType = mimeType;
            mContent = content;
        }
        
        public String getMimeType() { return mMimeType; }
        public String getContent() { return mContent; }
    }
    
    public Set<SignatureContent> getContents() {
        Set<SignatureContent> contents = new HashSet<SignatureContent>();
        
        for (Iterator it = sAttrTypeMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            
            String content = getAttr((String)entry.getKey());
            if (content != null)
                contents.add(new SignatureContent((String)entry.getValue(), content));
        }
        
        return contents;
    }
    
    public static String mimeTypeToAttrName(String mimeType) {
        return sAttrTypeMap.inverse().get(mimeType);
    }
    
    public static String attrNameToMimeType(String attrName) {
        return sAttrTypeMap.get(attrName);
    }

}
