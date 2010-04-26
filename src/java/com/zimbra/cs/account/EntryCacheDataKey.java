package com.zimbra.cs.account;

// TODO: move all cache data keys here.
//       currently they are scaterred everywhere
//       also, change to signature of set/getCacheDat to enum and remove the getKeyName call


public enum EntryCacheDataKey {
    
    /*
     * account
     */
    ACCOUNT_COS,
    ACCOUNT_IS_GAL_SYNC_ACCOUNT,
    ACCOUNT_EMAIL_FIELDS,
    
    /*
     * MailTarget
     */
    MAILTARGET_DOMAIN_ID;
    
    
    // all access of the key name must be through this, 
    // not calling name() or toString() directly
    public String getKeyName() {
        return name();
    }

}
