/*
 * Created on Mar 28, 2005
 *
 */
package com.zimbra.cs.service.util;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;


/**
 * @author tim
 *
 * A mail_item ID coming off the wire can be of the format:
 * 
 *    /SERVER/MAILBOXID/MAILITEMID[-SUBID] -- different server, different mailbox    
 *    /SERVER/MAILBOXID -- mailbox on different server
 *    MAILBOXID/MAILITEMID[-SUBID]  -- local, but in another account (requires admin auth token)
 *    MAIL_ITEM_ID[-SUBID]  -- local, in this account's mail_item table
 *    
 * "SUBID" is used for composite objects (ie Appointments) which can contain sub-objects   
 * 
 * (do we also need //SERVER/SERVER-SPECIFIC-RESOURCE-DESCRIPTION?)
 * 
 *
 * IMPORTANT----->Server specifier MUST have initial "/" -- so that it is possible to differentiate
 *       SERVER/MAILITEMID from MAILBOXID/MAILITEMID 
 * 
 * Given an "id" field, parse it out into Server
 *  
 */
public class ParsedItemID { 
    
    static public ParsedItemID Parse(String itemID) throws IllegalArgumentException, ServiceException
    {
        return new ParsedItemID(itemID);
    }
    
    static public ParsedItemID Create(int mailItemId, int subId) throws IllegalArgumentException, ServiceException {
        // FIXME eliminate conversion through string here
        return Parse(mailItemId+"-"+subId);
    }
    
    static public ParsedItemID Create(int mailItemId) throws IllegalArgumentException, ServiceException {
        // FIXME eliminate conversion through string here
        return Parse(Integer.toString(mailItemId));
    }
    
    public String getString() { return mInitialString; };
    public String toString() { return getString(); };
    
    public boolean hasServerID() { return mServerId != null; }
    public boolean hasMailboxID() { return mMailboxId != null; }
    public boolean hasSubId() { return mSubId != null; }
    
    public String getServerIDString() { return mServerId; }
    public String getMailboxIDString() { return mMailboxId; }
    public String getItemIDString() { return mItemId; }
    public boolean isLocal() { return mIsLocal; }
    
    public int getMailboxIDInt() {
        if (mMailboxIdInt == -1) {
            if (mMailboxId != null) {
                mMailboxIdInt = Integer.parseInt(mMailboxId);
            }
        }
        return mMailboxIdInt;
    }
    public int getItemIDInt() {
        if (mItemIdInt == -1) {
            if (mItemId != null) {
                mItemIdInt = Integer.parseInt(mItemId);
            }
        }
        return mItemIdInt;
    }
    
    public int getSubIdInt() {
        if (mSubIdInt == -1) {
            if (mSubId != null) {
                mSubIdInt = Integer.parseInt(mSubId);
            }
        }
        return mSubIdInt;
    }
    
    
    private String mInitialString = null;
    
    private String mServerId = null;
    private String mMailboxId = null;
    private String mItemId = null;
    private String mSubId = null;
    
    private int mItemIdInt = -1;
    private int mMailboxIdInt = -1;
    private int mSubIdInt = -1;
    private boolean mIsLocal = true;
    

    /**
     * 
     */
    
    /**
     * MAILITEMID or MAILITEMID-SUBID.  Sets mItemID and mSubID
     *  
     * @param itemIdPart
     * @throws ServiceException
     */
    private void parseItemIdPart(String itemIdPart) throws ServiceException {
        int poundIdx = itemIdPart.indexOf('-');
        if (poundIdx > -1) {
            mItemId = itemIdPart.substring(0, poundIdx);
            mSubId = itemIdPart.substring(poundIdx+1);
        } else {
            mItemId = itemIdPart;
        }
    }
    
    private ParsedItemID(String itemID) throws ServiceException {
        mInitialString = itemID;
        
        String[] substrs = itemID.split("/");
        switch(substrs.length) {
        case 4:
            /* /server/mailboxid/mailitemid */
            /* /server//mailitemid */
            if (substrs[0].length() > 0) {
                throw new IllegalArgumentException("Invalid ItemID Specifier: "+itemID);
            }
            if (substrs[1].length() == 0) {
                throw new IllegalArgumentException("Invalid ItemID Specifier (double initial '/'?): "+itemID);
            }
            mServerId = substrs[1];
            String localhost = Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_zimbraServiceHostname);
            if (!mServerId.equals(localhost)) {
                mIsLocal = false;
            }
            
            if (substrs[2].length() > 0) {
                mMailboxId = substrs[2];
            }
//            mItemId = substrs[3];
            parseItemIdPart(substrs[3]);
            
            break;
        case 3:
            /* ERROR: /server/mailitemid  (no double-/ in middle) */
            throw new IllegalArgumentException("Invalid ItemID Specifier (missing double-'/' before mailitemid?): "+ itemID);
        case 2:
            /* MAILBOXID/MAILITEMID */
            mMailboxId = substrs[0];

//            mItemId = substrs[1];
            parseItemIdPart(substrs[1]);
                        
            break;
        case 1:
            /* MAILITEMID */
//            mItemId = substrs[0];
            parseItemIdPart(substrs[0]);
            
            break;
        default:
            throw new IllegalArgumentException("Invalid ItemID Specifier: "+itemID);
        }
    }
    
}
