/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account;

import java.util.ArrayList;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.util.Zimbra;

public abstract class ProvisioningExt {
    
    public static abstract class ProvExt {
        public abstract boolean serverOnly();
        
        public boolean enabled() {
            // skip if the listener needs to run inside the server 
            // and we are not inside the server
            return !(serverOnly() && !Zimbra.started());
        }
    }

    public static abstract class PostCreateAccountListener extends ProvExt {
        public abstract void handle(Account acct) throws ServiceException;
    }
    
    private static final ArrayList<PostCreateAccountListener> 
                postCreateAccountListeners = new ArrayList<PostCreateAccountListener>();
    
    public static void addPostCreateAccountListener(PostCreateAccountListener listener) {
        synchronized (postCreateAccountListeners) {
            postCreateAccountListeners.add(listener);
        }
    }
    
    public static ArrayList<PostCreateAccountListener> getPostCreateAccountListeners() {
        return postCreateAccountListeners;
    }
}
