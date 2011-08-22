/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.mailbox.acl;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ShareInfoData;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;

/**
 */
public class AclPushSerializer {

    private AclPushSerializer() {
    }

    public static String serialize(ShareInfoData shareInfoData) {
        return serialize(
                shareInfoData.getFolderId(),
                shareInfoData.getFolderPath(),
                shareInfoData.getFolderDefaultViewCode(),
                shareInfoData.getGranteeId(),
                shareInfoData.getGranteeName(),
                shareInfoData.getGranteeTypeCode(),
                shareInfoData.getRightsCode());
    }

    public static String serialize(Folder folder, ACL.Grant grant) {
        return serialize(
                folder.getId(),
                folder.getPath(),
                folder.getDefaultView(),
                grant.getGranteeId(),
                grant.getGranteeName(),
                grant.getGranteeType(),
                grant.getGrantedRights());
    }

    public static String serialize(int folderId, String folderPath, MailItem.Type folderDefaultView, String granteeId,
                                   String granteeName, byte granteeType, short rights) {
        // Mailbox ACLs typically persist grantee id but not grantee name
        if (granteeName == null && granteeId != null) {
            try {
                switch (granteeType) {
                    case ACL.GRANTEE_USER:
                        granteeName = Provisioning.getInstance().get(Key.AccountBy.id, granteeId).getName();
                        break;
                    case ACL.GRANTEE_GROUP:
                        granteeName = Provisioning.getInstance().get(Key.DistributionListBy.id, granteeId).getName();
                        break;
                    case ACL.GRANTEE_DOMAIN:
                        granteeName = Provisioning.getInstance().get(Key.DomainBy.id, granteeId).getName();
                        break;
                    case ACL.GRANTEE_COS:
                        granteeName = Provisioning.getInstance().get(Key.CosBy.id, granteeId).getName();
                        break;
                    default:
                }
            } catch (ServiceException e) {
                ZimbraLog.misc.info("Error in getting grantee name for grantee id %s", granteeId, e);
            }
        }
        return new StringBuilder().
                append("granteeId:").append(granteeId).
                append(";granteeName:").append(granteeName).
                append(";granteeType:").append(ACL.typeToString(granteeType)).
                append(";folderId:").append(folderId).
                append(";folderPath:").append(folderPath).
                append(";folderDefaultView:").append(folderDefaultView).
                append(";rights:").append(ACL.rightsToString(rights)).
                toString();
    }

    public static ShareInfoData deserialize(String sharedItemInfo) throws ServiceException {
        String[] parts = sharedItemInfo.split(";");
        ShareInfoData obj = new ShareInfoData();
        // granteeId and granteeName could be "null", e.g. for public/all shares
        String granteeId = parts[0].substring("granteeId:".length());
        obj.setGranteeId("null".equals(granteeId) ? null : granteeId);
        String granteeName = parts[1].substring("granteeName:".length());
        obj.setGranteeName("null".equals(granteeName) ? null : granteeName);
        obj.setGranteeType(ACL.stringToType(parts[2].substring("granteeType:".length())));
        obj.setFolderId(Integer.valueOf(parts[3].substring("folderId:".length())));
        obj.setFolderPath(parts[4].substring("folderPath:".length()));
        obj.setFolderDefaultView(MailItem.Type.of(parts[5].substring("folderDefaultView:".length())));
        obj.setRights(ACL.stringToRights(parts[6].substring("rights:".length())));
        return obj;
    }
}
