/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.gal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.datasource.MailItemImport;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapDateUtil;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedContact;

public class GalImport extends MailItemImport {

    public GalImport(DataSource ds) throws ServiceException {
        super(ds);
    }

    @Override
    public void importData(List<Integer> folderIds, boolean fullSync)
            throws ServiceException {
        if (folderIds == null)
            importGal(dataSource.getFolderId(), fullSync, false);
        else
            for (int fid : folderIds)
                importGal(fid, fullSync, false);
    }

    @Override
    public void test() throws ServiceException {
        searchGal(null, SearchGalResult.newSearchGalResult(null), false);
    }

    private static final String TYPE = "t";
    private static final String FOLDER = "f";
    public static final String SYNCTOKEN = "st";

    private void setStatus(boolean success) throws ServiceException {
        Date now = new Date();
        DataSource ds = getDataSource();
        Map<String,Object> attrs = new HashMap<String,Object>();
        String attr = success ?
                Provisioning.A_zimbraGalLastSuccessfulSyncTimestamp :
                Provisioning.A_zimbraGalLastFailedSyncTimestamp;
        attrs.put(attr, LdapDateUtil.toGeneralizedTime(now));
        Provisioning.getInstance().modifyDataSource(ds.getAccount(), ds.getId(), attrs);
    }

    public void importGal(int fid, boolean fullSync, boolean force) throws ServiceException {
        mbox.beginTrackingSync();
        DataSource ds = getDataSource();
        DataSourceItem folderMapping = DbDataSource.getMapping(ds, fid);
        if (folderMapping.md == null) {
            folderMapping.itemId = fid;
            folderMapping.md = new Metadata();
            folderMapping.md.put(TYPE, FOLDER);
            DbDataSource.addMapping(ds, folderMapping);
        }
        String syncToken = fullSync ? "" : folderMapping.md.get(SYNCTOKEN, "");
        HashMap<String,DataSourceItem> allMappings = new HashMap<String,DataSourceItem>();
        if (fullSync || force)
            for (DataSourceItem dsItem : DbDataSource.getAllMappings(ds))
                if (dsItem.md == null || dsItem.md.get(TYPE, null) == null)  // non-folder items
                    allMappings.put(dsItem.remoteId, dsItem);
        OperationContext octxt = new OperationContext(mbox);
        SearchGalResult result = SearchGalResult.newSearchGalResult(new GalSearchVisitor(mbox, allMappings, fid, force));
        try {
            searchGal(syncToken, result, true);
        } catch (Exception e) {
            setStatus(false);
            ZimbraLog.gal.error("Error executing gal search", e);
            return;
        }

        folderMapping.md.put(SYNCTOKEN, result.getToken());
        DbDataSource.updateMapping(ds, folderMapping);
        if (allMappings.size() == 0 || !fullSync) {
            setStatus(true);
            return;
        }

        ArrayList<Integer> deleted = new ArrayList<Integer>();
        int[] deletedIds = new int[allMappings.size()];
        int i = 0;
        for (DataSourceItem dsItem : allMappings.values()) {
            deleted.add(dsItem.itemId);
            deletedIds[i++] = dsItem.itemId;
        }
        try {
            mbox.delete(octxt, deletedIds, MailItem.Type.CONTACT, null);
        } catch (ServiceException e) {
            ZimbraLog.gal.warn("Ignoring error deleting gal contacts", e);
        }
        DbDataSource.deleteMappings(getDataSource(), deleted);
        mbox.index.optimize();
        setStatus(true);
    }

    private void searchGal(String syncToken, SearchGalResult result, boolean fetchGroupMembers) throws ServiceException {
        ZimbraLog.gal.debug("searchGal: "+syncToken);
        DataSource ds = getDataSource();
        GalSearchParams params = new GalSearchParams(ds);
        params.setGalResult(result);
        params.setToken(syncToken);
        params.setQuery("*");
        for (String attr : ZIMBRA_ATTRS)
            params.getConfig().getRules().add(attr+"="+attr);
        params.getConfig().getRules().setFetchGroupMembers(fetchGroupMembers);
        params.getConfig().getRules().setNeedSMIMECerts(true);
        Provisioning.getInstance().searchGal(params);
    }
    private static String[] ZIMBRA_ATTRS = {
        "zimbraNotes",
        "zimbraAccountCalendarUserType",
        "zimbraCalResType",
        "zimbraCalResLocationDisplayName",
        "zimbraCalResCapacity",
        "zimbraCalResContactEmail"
    };

    private class GalSearchVisitor implements GalContact.Visitor {
        Mailbox mbox;
        OperationContext octxt;
        Map<String,DataSourceItem> mappings;
        int fid;
        boolean force;

        private GalSearchVisitor(Mailbox mbox, Map<String,DataSourceItem> mappings, int fid, boolean force) throws ServiceException {
            this.mbox = mbox;
            this.octxt = new OperationContext(mbox);
            this.mappings = mappings;
            this.fid = fid;
            this.force = force;
        }

        private final String[] FILE_AS_STR_KEYS = {
            ContactConstants.A_fullName,
            ContactConstants.A_email,
            ContactConstants.A_email2,
            ContactConstants.A_email3
        };

        private void addFileAsStr(Map<String,Object> attrs) {
            for (String key : FILE_AS_STR_KEYS) {
                Object fileAsStr = attrs.get(key);
                if (fileAsStr != null && fileAsStr instanceof String) {
                    attrs.put(ContactConstants.A_fileAs, ContactConstants.FA_EXPLICIT+":"+(String)fileAsStr);
                    return;
                }
            }
        }

        private void addFileAsFromUid(Map<String,Object> attrs) {
            String dnString = attrs.get(LdapConstants.ATTR_dn).toString();
            for (String dnAttr : dnString.split(",")) {
                if (dnAttr.startsWith(LdapConstants.ATTR_uid)) {
                    String[] uidArr = dnAttr.split("=");
                    if (uidArr.length == 2
                            && !StringUtil.isNullOrEmpty(uidArr[1])) {
                        attrs.put(ContactConstants.A_fileAs, ContactConstants.FA_EXPLICIT+":"+uidArr[1]);
                    }
                }
            }
        }

        @Override
        public void visit(GalContact contact) throws ServiceException {
            Map<String,Object> attrs = contact.getAttrs();
            String id = contact.getId();
            mappings.remove(id);
            attrs.put(ContactConstants.A_dn, id);
            ZimbraLog.gal.debug("processing gal contact "+id);
            DataSourceItem dsItem = DbDataSource.getReverseMapping(getDataSource(), id);
            if (attrs.get(LdapConstants.ATTR_objectClass) != null
                    && attrs.get(LdapConstants.ATTR_objectClass).equals(AttributeClass.OC_zimbraAddressList)) {
                addFileAsFromUid(attrs);
            } else {
                addFileAsStr(attrs);
            }
            if (dsItem.itemId == 0) {
                ZimbraLog.gal.debug("creating new contact "+id);
                dsItem.remoteId = id;
                ParsedContact pc = new ParsedContact(attrs);
                dsItem.itemId = mbox.createContact(octxt, pc, fid, null).getId();
                DbDataSource.addMapping(getDataSource(), dsItem);
            } else {
                Contact mboxContact = mbox.getContactById(octxt, dsItem.itemId);

                // check for update conditions
                String syncDate = mboxContact.get(MODIFY_TIMESTAMP);
                String modifiedDate = (String) contact.getAttrs().get(MODIFY_TIMESTAMP);
                if (!force && syncDate != null && syncDate.equals(modifiedDate)) {
                    ZimbraLog.gal.debug("gal contact %s has not been modified", id);
                    return;
                }
                if (!force && allFieldsMatch(attrs, mboxContact.getAllFields())) {
                    ZimbraLog.gal.debug("no field has changed in gal contact %s", id);
                    return;
                }

                ZimbraLog.gal.debug("modifying contact "+id);
                ParsedContact pc = new ParsedContact(attrs);
                mbox.modifyContact(octxt, dsItem.itemId, pc);
            }
        }

        private static final String MODIFY_TIMESTAMP = "modifyTimeStamp";

        private boolean allFieldsMatch(Map<String,Object> ldapContact, Map<String,String> contact) {
            if (ldapContact.size() != contact.size())
                return false;
            HashSet<String> ignoredKeys = new HashSet<String>();
            // always ignore the modified timestamp when comparing attributes.
            ignoredKeys.add(MODIFY_TIMESTAMP);
            Collections.addAll(ignoredKeys, dataSource.getMultiAttr(Provisioning.A_zimbraGalSyncIgnoredAttributes));
            for (Map.Entry<String,Object> entry : ldapContact.entrySet()) {
                String key = entry.getKey();
                if (ignoredKeys.contains(key))
                    continue;
                Object ldapValue = entry.getValue();
                if (ldapValue instanceof String) {
                    String contactValue = contact.get(key);
                    if (!((String)ldapValue).equals(contactValue))
                        return false;
                } else if (ldapValue instanceof String[]) {
                    try {
                        String encodedValue = Contact.encodeMultiValueAttr(((String[])ldapValue));
                        if (!encodedValue.equals(contact.get(key)))
                            return false;
                    } catch (JSONException e) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
