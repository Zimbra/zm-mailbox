/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox.acl;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
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
                shareInfoData.getItemId(),
                shareInfoData.getItemUuid(),
                shareInfoData.getPath(),
                shareInfoData.getFolderDefaultViewCode(),
                shareInfoData.getType(),
                shareInfoData.getGranteeId(),
                shareInfoData.getGranteeName(),
                shareInfoData.getGranteeTypeCode(),
                shareInfoData.getRightsCode(),
                shareInfoData.getExpiry());
    }

    public static String serialize(MailItem item, ACL.Grant grant) {
        return serialize(
                item.getId(), item.getUuid(),
                (item instanceof Folder) ? ((Folder)item).getPath() : item.getName(),
                (item instanceof Folder) ? ((Folder)item).getDefaultView() : item.getType(),
                item.getType(),
                grant.getGranteeId(),
                grant.getGranteeName(),
                grant.getGranteeType(),
                grant.getGrantedRights(),
                grant.getEffectiveExpiry(item.getACL()));
    }

    public static String serialize(int itemId, String itemUuid, String path, MailItem.Type folderDefaultView, MailItem.Type type,
            String granteeId, String granteeName, byte granteeType, short rights, long expiry) {
        // Mailbox ACLs typically persist grantee id but not grantee name
        if (granteeName == null && granteeId != null) {
            try {
                switch (granteeType) {
                    case ACL.GRANTEE_USER:
                        Account granteeAcct = Provisioning.getInstance().get(Key.AccountBy.id, granteeId);
                        if (granteeAcct != null) {
                            granteeName = granteeAcct.getName();
                        }
                        break;
                    case ACL.GRANTEE_GROUP:
                        DistributionList granteeDL = Provisioning.getInstance().get(Key.DistributionListBy.id, granteeId);
                        if (granteeDL != null) {
                            granteeName = granteeDL.getName();
                        }
                        break;
                    case ACL.GRANTEE_DOMAIN:
                        Domain granteeDomain = Provisioning.getInstance().get(Key.DomainBy.id, granteeId);
                        if (granteeDomain != null) {
                            granteeName = granteeDomain.getName();
                        }
                        break;
                    case ACL.GRANTEE_COS:
                        Cos granteeCos = Provisioning.getInstance().get(Key.CosBy.id, granteeId);
                        if (granteeCos != null) {
                            granteeName = granteeCos.getName();
                        }
                        break;
                    default:
                }
            } catch (ServiceException e) {
                ZimbraLog.misc.info("Error in getting grantee name for grantee id %s", granteeId, e);
            }
        }
        if (granteeType == ACL.GRANTEE_GUEST && granteeId != null) {
            granteeId = granteeId.toLowerCase();
        }
        
        if (path.contains(";")) {
        	path = path.replaceAll(";", SEMICOLON_ESCAPE_SEQ);
        }
        StringBuilder sb = new StringBuilder().
                append("granteeId:").append(granteeId).
                append(";granteeName:").append(granteeName).
                append(";granteeType:").append(ACL.typeToString(granteeType)).
                append(";folderId:").append(itemId).
                append(";folderUuid:").append(itemUuid).
                append(";folderPath:").append(path).
                append(";folderDefaultView:").append(folderDefaultView).
                append(";rights:").append(ACL.rightsToString(rights)).
                append(";type:").append(type);
        if (expiry != 0) {
            sb.append(";expiry:").append(expiry);
        }
        return sb.toString();
    }

    public static ShareInfoData deserialize(String sharedItemInfo) throws ServiceException {
        String[] parts = sharedItemInfo.split(";");
        Map<String, String> attrs = new HashMap<String, String>();
        String key = null;
        for (String part : parts) {
            String x[] = part.split(":", 2);
            if (x.length == 2) {
            	attrs.put(x[0], x[1]);
            	key = x[0];
            } else {
            	String value = attrs.get(key);
            	attrs.put(key, value + ";" + x[0]);
            }
        }
        ShareInfoData obj = new ShareInfoData();

        String granteeId = attrs.get("granteeId");
        obj.setGranteeId("null".equals(granteeId) ? null : granteeId);
        String granteeName = attrs.get("granteeName");
        obj.setGranteeName("null".equals(granteeName) ? null : granteeName);
        obj.setGranteeType(ACL.stringToType(attrs.get("granteeType")));
        obj.setItemId(Integer.valueOf(attrs.get("folderId")));
        String uuid = attrs.get("folderUuid");
        obj.setItemUuid("null".equals(uuid) ? null : uuid);
        if (attrs.get("folderPath").contains(SEMICOLON_ESCAPE_SEQ)) {
        	String temp = attrs.get("folderPath").replaceAll("\\*ASCII59\\*", ";");
        	obj.setPath(temp);	
        } else {
        	obj.setPath(attrs.get("folderPath"));
        }
        
        obj.setFolderDefaultView(MailItem.Type.of(attrs.get("folderDefaultView")));
        obj.setRights(ACL.stringToRights(attrs.get("rights")));
        String type = attrs.get("type");
        if (type != null) {
            obj.setType(MailItem.Type.of(type));
        } else {
            obj.setType(MailItem.Type.FOLDER);
        }
        String expiry = attrs.get("expiry");
        if (expiry != null) {
            obj.setExpiry(Long.valueOf(expiry));
        }
        return obj;
    }
    
    public static final String SEMICOLON_ESCAPE_SEQ = "*ASCII59*";
}
