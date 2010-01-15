/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;

public class Right {
    
    // known rights
    public static Right RT_invite;
    public static Right RT_viewFreeBusy;
    public static Right RT_loginAs;
    
    private final String mCode;
    private String mDesc;  // a brief description
    private String mDoc;   // a more detailed description, use cases, examples
    private Boolean mDefault;
    
    static void initKnownRights(RightManager rm) throws ServiceException {
        RT_invite = rm.getRight("invite");
        RT_viewFreeBusy = rm.getRight("viewFreeBusy");
        RT_loginAs = rm.getRight("loginAs");
    }

    Right(String code) {
        mCode = code;
    }
    
    /**
     * - code stored in the ACE.
     * - code appear in XML
     * - code displayed by CLI
     * 
     * @return 
     */
    public String getCode() {
        return mCode;
    }
    
    public String getDesc() {
        return mDesc;
    }
    
    public String getDoc() {
        return mDoc;
    }
    
    public Boolean getDefault() {
        return mDefault;
    }
    
    void setDesc(String desc) {
        mDesc = desc;
    }
    
    void setDoc(String doc) {
        mDoc = doc;
    }

    void setDefault(Boolean defaultValue) {
        mDefault = defaultValue;
    }

}
