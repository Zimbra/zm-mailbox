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
