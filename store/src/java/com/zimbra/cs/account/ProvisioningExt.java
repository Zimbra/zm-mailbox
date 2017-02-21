/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
