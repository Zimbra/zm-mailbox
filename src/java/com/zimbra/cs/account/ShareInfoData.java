/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.MailItem;

public class ShareInfoData {
    // owner
    private String mOwnerAcctId;
    private String mOwnerAcctEmail;
    private String mOwnerAcctDisplayName;

    // item
    private int mItemId;
    private String mPath;
    private MailItem.Type folderDefaultView;
    private MailItem.Type type;

    // rights
    private short mRights;

    // grantee
    private byte mGranteeType;
    private String mGranteeId;
    private String mGranteeName;
    private String mGranteeDisplayName;

    // URL for accessing the share
    // only used for guest share
    private String mUrl;

    // password (for guest grantee)
    // only used for sending
    private String mGuestPassword;

    // time when share expires
    private long mExpiry = 0;

    // mountpointid
    // Note:
    //    only used by zmprov/SoapProvisioning  to construct a
    //    ShareInfo from SOAP response to pass back just to be displayed
    //
    //    NOT used by the any of the ShareInfo code on the server side
    //    On the server side, the mountpoint id is not part of the ShareInfo,
    //    it is a property of the target account, and is encoded in the
    //    SOAP response by the visitor.
    //
    private String mMountpointId_zmprov_only;
    public void setMountpointId_zmprov_only(String mptId) {
        mMountpointId_zmprov_only = mptId;
    }
    public String getMountpointId_zmprov_only() {
        if (mMountpointId_zmprov_only == null)
            return "";
        else
            return mMountpointId_zmprov_only;
    }
    ////////////////////////////////////



    public void setOwnerAcctId(String ownerAcctId) {
        mOwnerAcctId = ownerAcctId;
    }

    public String getOwnerAcctId() {
        return mOwnerAcctId;
    }

    public void setOwnerAcctEmail(String ownerAcctEmail) {
        mOwnerAcctEmail = ownerAcctEmail;
    }

    public String getOwnerAcctEmail() {
        return mOwnerAcctEmail;
    }

    public void setOwnerAcctDisplayName(String ownerAcctDisplayName) {
        mOwnerAcctDisplayName = ownerAcctDisplayName;
    }

    public String getOwnerAcctDisplayName() {
        return mOwnerAcctDisplayName;
    }

    public void setItemId(int itemId) {
        mItemId = itemId;
    }

    public int getItemId() {
        return mItemId;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public String getPath() {
        return mPath;
    }

    // returns the leaf name
    public String getName() {
        String[] fn = mPath.split("/");
        return fn.length > 0 ? fn[fn.length - 1] : mPath;
    }

    public void setFolderDefaultView(MailItem.Type view) {
        folderDefaultView = view;
    }

    public String getFolderDefaultView() {
        return folderDefaultView.toString();
    }

    public MailItem.Type getFolderDefaultViewCode() {
        return folderDefaultView;
    }
    
    public void setType(MailItem.Type type) {
        this.type = type;
    }
    
    public MailItem.Type getType() {
        return (type == null ? MailItem.Type.UNKNOWN : type);
    }

    public void setRights(short rights) {
        mRights = rights;
    }

    public String getRights() {
        return ACL.rightsToString(mRights);
    }

    public short getRightsCode() {
        return mRights;
    }

    public void setGranteeType(byte granteeType) {
        mGranteeType = granteeType;
    }

    public byte getGranteeTypeCode() {
        return mGranteeType;
    }

    public String getGranteeType() {
        return ACL.typeToString(mGranteeType);
    }

    public void setGranteeId(String granteeId) {
        mGranteeId = granteeId;
    }

    public String getGranteeId() {
        return mGranteeId;
    }

    public void setGranteeName(String granteeName) {
        mGranteeName = granteeName;
    }

    public String getGranteeName() {
        return mGranteeName;
    }

    public void setGranteeDisplayName(String granteeDisplayName) {
        mGranteeDisplayName = granteeDisplayName;
    }

    public String getGranteeDisplayName() {
        return mGranteeDisplayName;
    }


    public void setUrl(String url) {
        mUrl = url;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setGuestPassword(String guestPassword) {
        mGuestPassword = guestPassword;
    }

    public String getGuestPassword() {
        return mGuestPassword;
    }

    public void setExpiry(Long expiry) {
        mExpiry = expiry;
    }

    public long getExpiry() {
        return mExpiry;
    }

    public boolean isExpired() {
        return mExpiry != 0 && System.currentTimeMillis() > mExpiry;
    }

    /*
     * name shown in notification message, must have a value
     * return display name if it is set, otherwise return
     * the owner's email
     */
    public String getOwnerNotifName() {
        String notifName = getOwnerAcctDisplayName();
        if (notifName != null)
            return notifName;
        else
            return getOwnerAcctEmail();
    }

    /*
     * name shown in notification message, must have a value
     * return display name if it is set, otherwise return
     * the grantee name
     */
    public String getGranteeNotifName() {
        String notifName = getGranteeDisplayName();
        if (notifName != null)
            return notifName;
        else
            return getGranteeName();
    }


    public static ShareInfoData fromJaxbShareInfo(
            com.zimbra.soap.type.ShareInfo sInfo)
    throws ServiceException {
        ShareInfoData sid = new ShareInfoData();

        sid.setOwnerAcctId(sInfo.getOwnerId());
        sid.setOwnerAcctEmail(sInfo.getOwnerEmail());
        sid.setOwnerAcctDisplayName(sInfo.getOwnerDisplayName());
        sid.setItemId(sInfo.getFolderId());
        sid.setPath(sInfo.getFolderPath());
        sid.setFolderDefaultView(MailItem.Type.of(sInfo.getDefaultView()));
        sid.setRights(ACL.stringToRights(sInfo.getRights()));
        sid.setGranteeType(ACL.stringToType(sInfo.getGranteeType()));
        sid.setGranteeId(sInfo.getGranteeId());
        sid.setGranteeName(sInfo.getGranteeName());
        sid.setGranteeDisplayName(sInfo.getGranteeDisplayName());
        sid.setMountpointId_zmprov_only(sInfo.getMountpointId());

        return sid;
    }

    public static ShareInfoData fromXML(Element eShare) throws ServiceException {
        ShareInfoData sid = new ShareInfoData();

        sid.setOwnerAcctId(eShare.getAttribute(AccountConstants.A_OWNER_ID, null));
        sid.setOwnerAcctEmail(eShare.getAttribute(AccountConstants.A_OWNER_EMAIL, null));
        sid.setOwnerAcctDisplayName(eShare.getAttribute(AccountConstants.A_OWNER_DISPLAY_NAME, null));
        sid.setItemId(Integer.valueOf(eShare.getAttribute(AccountConstants.A_FOLDER_ID)));
        sid.setPath(eShare.getAttribute(AccountConstants.A_FOLDER_PATH, null));
        sid.setFolderDefaultView(MailItem.Type.of(eShare.getAttribute(MailConstants.A_DEFAULT_VIEW, null)));
        sid.setRights(ACL.stringToRights(eShare.getAttribute(AccountConstants.A_RIGHTS)));
        sid.setGranteeType(ACL.stringToType(eShare.getAttribute(AccountConstants.A_GRANTEE_TYPE)));
        sid.setGranteeId(eShare.getAttribute(AccountConstants.A_GRANTEE_ID, null));
        sid.setGranteeName(eShare.getAttribute(AccountConstants.A_GRANTEE_NAME, null));
        sid.setGranteeDisplayName(eShare.getAttribute(AccountConstants.A_GRANTEE_DISPLAY_NAME, null));

        // and this ugly thing
        sid.setMountpointId_zmprov_only(eShare.getAttribute(AccountConstants.A_MOUNTPOINT_ID, null));

        return sid;
    }

    public void toXML(Element eShare, Integer mptId) {
        eShare.addAttribute(AccountConstants.A_OWNER_ID,             getOwnerAcctId());
        eShare.addAttribute(AccountConstants.A_OWNER_EMAIL,          getOwnerAcctEmail());
        eShare.addAttribute(AccountConstants.A_OWNER_DISPLAY_NAME,   getOwnerAcctDisplayName());
        eShare.addAttribute(AccountConstants.A_FOLDER_ID,            getItemId());
        eShare.addAttribute(AccountConstants.A_FOLDER_PATH,          getPath());
        eShare.addAttribute(MailConstants.A_DEFAULT_VIEW,            getFolderDefaultView());
        eShare.addAttribute(AccountConstants.A_RIGHTS,               getRights());
        eShare.addAttribute(AccountConstants.A_GRANTEE_TYPE,         getGranteeType());
        eShare.addAttribute(AccountConstants.A_GRANTEE_ID,           getGranteeId());
        eShare.addAttribute(AccountConstants.A_GRANTEE_NAME,         getGranteeName());
        eShare.addAttribute(AccountConstants.A_GRANTEE_DISPLAY_NAME, getGranteeDisplayName());

        if (mptId != null)
            eShare.addAttribute(AccountConstants.A_MOUNTPOINT_ID, mptId.toString());
    }

    public String toString() {
        String format = "    %15s : %s\n";
        StringBuffer sb = new StringBuffer();
        sb.append(String.format(
                format, "owner id",        getOwnerAcctId()));
        sb.append(String.format(
                format, "owner email",     getOwnerAcctEmail()));
        sb.append(String.format(
                format, "owner display",   getOwnerAcctDisplayName()));
        sb.append(String.format(
                format, "folder id",       String.valueOf(getItemId())));
        sb.append(String.format(
                format, "folder path",     getPath()));
        sb.append(String.format(
                format, "view",            getFolderDefaultView()));
        sb.append(String.format(
                format, "rights",          getRights()));
        sb.append(String.format(
                format, "mountpoint id",   getMountpointId_zmprov_only()));
        sb.append(String.format(
                format, "grantee type",    getGranteeType()));
        sb.append(String.format(
                format, "grantee id",      getGranteeId()));
        sb.append(String.format(
                format, "grantee email",   getGranteeName()));
        sb.append(String.format(
                format, "grantee display", getGranteeDisplayName()));
        return sb.toString();
    }

    public void dump() {
        System.out.println();
        System.out.println(this.toString());
        System.out.println();
    }
}
