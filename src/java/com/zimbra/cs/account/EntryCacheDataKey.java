package com.zimbra.cs.account;

// TODO: move all cache data keys here.
//       currently they are scattered everywhere
//       also, change to signature of set/getCacheDat to enum and remove the getKeyName call


public enum EntryCacheDataKey {
    
    /*
     * account
     */
    ACCOUNT_COS,
    ACCOUNT_IS_GAL_SYNC_ACCOUNT,
    ACCOUNT_EMAIL_FIELDS,
    ACCOUNT_VALIDITY_VALUE_HIGHEST_RELOAD,
    
    /*
     * MailTarget
     */
    MAILTARGET_DOMAIN_ID,
    
    /*
     * grouped entries(entries that can be in groups): account, cr, dl
     */
    GROUPEDENTRY_DIRECT_GROUPIDS;  
    
    
    // all access of the key name must be through this, 
    // not calling name() or toString() directly
    // TODO: add a signature in Entry to take a EntryCacheDataKey for get/set
    public String getKeyName() {
        return name();
    }

}
