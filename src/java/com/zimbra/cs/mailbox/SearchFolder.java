/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Aug 23, 2004
 */
package com.zimbra.cs.mailbox;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author dkarp
 */
public class SearchFolder extends Folder {

    /** The search folder's query. */
    private String mQuery;
    /** The comma-separated list of types the search should return. */
    private String mTypes;
    /** The field to sort the search's results on. */
    private String mSort;

    public SearchFolder(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_SEARCHFOLDER)
            throw new IllegalArgumentException();
    }

    /** Returns the query associated with this search folder. */
    public String getQuery() {
        return (mQuery == null ? "" : mQuery);
    }

    /** Returns the set of item types returned by this search, or
     *  <code>""</code> if none were specified. */
    public String getReturnTypes() {
        return (mTypes == null ? "" : mTypes);
    }

    /** Returns the field this search is sorted on, or <code>""</code>
     *  if none was specified. */
    public String getSortField() {
        return (mSort == null ? "" : mSort);
    }

    /** Returns whether this search folder is visible through the IMAP
     *  interface.  At present, this is completely controlled by the COS
     *  attribute <code>zimbraPrefImapSearchFoldersEnabled</code>. */
    public boolean isImapVisible() {
        try {
            return mMailbox.getAccount().getBooleanAttr(Provisioning.A_zimbraPrefImapSearchFoldersEnabled, true);
        } catch (ServiceException e) {
            return true;
        }
    }


    /** Returns whether the folder can contain objects of the given type.
     *  Search folders may only contain other search folders. */
    @Override boolean canContain(byte type) {
        return (type == TYPE_SEARCHFOLDER);
    }


    /** Creates a new SearchFolder and persists it to the database.  A
     *  real nonnegative item ID must be supplied from a previous call to
     *  {@link Mailbox#getNextItemId(int)}.
     * 
     * @param id      The id for the new search folder.
     * @param parent  The parent folder to place the new folder in.
     * @param name    The new folder's name.
     * @param query   The query associated with the search folder.
     * @param types   The (optional) set of item types the search returns.
     * @param sort    The (optional) order the results are returned in.
     * @param color   The new folder's color.
     * @param custom  An optional extra set of client-defined metadata.
     * @perms {@link ACL#RIGHT_INSERT} on the parent folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><code>mail.CANNOT_CONTAIN</code> - if the target folder
     *        can't contain search folders
     *    <li><code>mail.ALREADY_EXISTS</code> - if a folder by that name
     *        already exists in the parent folder
     *    <li><code>mail.INVALID_NAME</code> - if the new folder's name is
     *        invalid
     *    <li><code>mail.INVALID_REQUEST</code> - if the supplied query
     *        string is blank or missing
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul>
     * @see #validateItemName(String)
     * @see #validateQuery(String)
     * @see #canContain(byte) */
    static SearchFolder create(int id, Folder parent, String name, String query, String types, String sort, int flags, Color color, CustomMetadata custom)
    throws ServiceException {
        if (parent == null || !parent.canContain(TYPE_SEARCHFOLDER))
            throw MailServiceException.CANNOT_CONTAIN();
        if (!parent.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on the parent folder");
        name = validateItemName(name);
        query = validateQuery(query);
        if (parent.findSubfolder(name) != null)
            throw MailServiceException.ALREADY_EXISTS(name);
        if (types != null && types.trim().equals(""))
            types = null;
        if (sort != null && sort.trim().equals(""))
            sort = null;

        Mailbox mbox = parent.getMailbox();
        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = TYPE_SEARCHFOLDER;
        data.folderId    = parent.getId();
        data.parentId    = parent.getId();
        data.date        = mbox.getOperationTimestamp();
        data.flags       = flags;
        data.name        = name;
        data.subject     = name;
        data.metadata    = encodeMetadata(color, 1, custom, query, types, sort);
        data.contentChanged(mbox);
        ZimbraLog.mailop.info("Adding SearchFolder %s: id=%d, parentId=%d, parentName=%s.",
            name, data.id, parent.getId(), parent.getName());
        DbMailItem.create(mbox, data, null);
        
        SearchFolder search = new SearchFolder(mbox, data);
        search.finishCreation(parent);
        return search;
    }

    /** Replaces the search folder's query and attributes with a new set.
     *  Persists the updated version to the cache and to the database.
     *  Omitting the query is not permitted; omitting attributes causes the
     *  search to use the default <code>types</code> and <code>sort</code>.
     * 
     * @param query   The new query associated with the search folder.
     * @param types   The new (optional) set of item types the search returns.
     * @param sort    The new (optional) order the results are returned in.
     * @perms {@link ACL#RIGHT_WRITE} on the search folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><code>mail.IMMUTABLE_OBJECT</code> - if the search folder
     *        cannot be modified
     *    <li><code>mail.INVALID_REQUEST</code> - if the supplied query
     *        string is blank or missing
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul> */
    void changeQuery(String query, String types, String sort) throws ServiceException {
        if (!isMutable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        if (!canAccess(ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on the search folder");
        query = validateQuery(query);

        if (query.equals(mQuery) && getReturnTypes().equals(types) && getSortField().equals(sort))
            return;
        markItemModified(Change.MODIFIED_QUERY);
        mQuery = query;
        mTypes = types;
        mSort  = sort;
        saveMetadata();
    }

    /** Cleans up the provided query string and verifies that it's not blank.
     *  Removes all non-XML-safe control characters and trims leading and
     *  trailing whitespace.
     * 
     * @param query  The query string.
     * @return The cleaned-up query string.
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><code>mail.INVALID_REQUEST</code> - if the cleaned-up query
     *        string is blank or missing</ul>
     * @see StringUtil#stripControlCharacters(String) */
    protected static String validateQuery(String query) throws ServiceException {
        if (query != null)
            query = StringUtil.stripControlCharacters(query).trim();
        if (query == null || query.equals(""))
            throw ServiceException.INVALID_REQUEST("search query must not be empty", null);
        return query;
    }


    @Override void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
        mQuery = meta.get(Metadata.FN_QUERY);
        mTypes = meta.get(Metadata.FN_TYPES, null);
        mSort = meta.get(Metadata.FN_SORT, null);
    }

    @Override Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mRGBColor, mVersion, mExtendedData, mQuery, mTypes, mSort);
    }

    private static String encodeMetadata(Color color, int version, CustomMetadata custom, String query, String types, String sort) {
        CustomMetadataList extended = (custom == null ? null : custom.asList());
        return encodeMetadata(new Metadata(), color, version, extended, query, types, sort).toString();
    }

    static Metadata encodeMetadata(Metadata meta, Color color, int version, CustomMetadataList extended, String query, String types, String sort) {
        meta.put(Metadata.FN_QUERY, query);
        meta.put(Metadata.FN_TYPES, types);
        meta.put(Metadata.FN_SORT,  sort);
        return MailItem.encodeMetadata(meta, color, version, extended);
    }


    private static final String CN_NAME  = "name";
    private static final String CN_QUERY = "query";

    @Override public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("search: {");
        appendCommonMembers(sb).append(", ");
        sb.append(CN_NAME).append(": ").append(getName()).append(", ");
        sb.append(CN_QUERY).append(": ").append(getQuery());
        sb.append("}");
        return sb.toString();
    }
}
