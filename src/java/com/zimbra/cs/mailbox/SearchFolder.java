/*
 * Created on Aug 23, 2004
 */
package com.zimbra.cs.mailbox;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.StringUtil;

/**
 * @author dkarp
 */
public class SearchFolder extends Folder {
    private String mQuery;
    private String mTypes;
    private String mSort;

    public SearchFolder(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_SEARCHFOLDER)
            throw new IllegalArgumentException();
    }

    public String getQuery() {
        return (mQuery == null ? "" : mQuery);
    }

    public String getReturnTypes() {
        return (mTypes == null ? "" : mTypes);
    }

    public String getSortField() {
        return (mSort == null ? "" : mSort);
    }

    public boolean isImapVisible() {
        try {
            return mMailbox.getAccount().getBooleanAttr(Provisioning.A_zimbraPrefImapSearchFoldersEnabled, true);
        } catch (ServiceException e) {
            return true;
        }
    }


    boolean canContain(byte type) {
        return (type == TYPE_SEARCHFOLDER);
    }


    static SearchFolder create(int id, Folder parent, String name, String query, String types, String sort) throws ServiceException {
        if (parent == null || !parent.canContain(TYPE_SEARCHFOLDER))
            throw MailServiceException.CANNOT_CONTAIN();
        name = validateFolderName(name);
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
        data.subject     = name;
        data.metadata    = encodeMetadata(query, types, sort);
        data.modMetadata = mbox.getOperationChangeID();
        data.modContent  = mbox.getOperationChangeID();
        DbMailItem.create(mbox, data);
        
        SearchFolder search = new SearchFolder(mbox, data);
        search.finishCreation(parent);
        return search;
    }

    void changeQuery(String query, String types, String sort) throws ServiceException {
        if (!isMutable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        query = validateQuery(query);

        if (query.equals(mQuery) && getReturnTypes().equals(types) && getSortField().equals(sort))
            return;
        markItemModified(Change.MODIFIED_QUERY);
        mQuery = query;
        mTypes = types;
        mSort  = sort;
        saveMetadata();
    }

    protected static String validateQuery(String query) throws ServiceException {
        if (query != null)
            query = StringUtil.stripControlCharacters(query).trim();
        if (query == null || query.equals(""))
            throw ServiceException.INVALID_REQUEST("search query must not be empty", null);
        return query;
    }


    Metadata decodeMetadata(String metadata) throws ServiceException {
        Metadata meta = new Metadata(metadata, this);
        mQuery = meta.get(Metadata.FN_QUERY);
        mTypes = meta.get(Metadata.FN_TYPES, null);
        mSort = meta.get(Metadata.FN_SORT, null);
        return meta;
    }

    String encodeMetadata() {
        return encodeMetadata(mQuery, mTypes, mSort);
    }
    static String encodeMetadata(String query, String types, String sort) {
        Metadata meta = new Metadata();
        meta.put(Metadata.FN_QUERY, query);
        meta.put(Metadata.FN_TYPES, types);
        meta.put(Metadata.FN_SORT,  sort);
        return meta.toString();
    }


    private static final String CN_NAME  = "name";
    private static final String CN_QUERY = "query";

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("search: {");
        appendCommonMembers(sb).append(", ");
        sb.append(CN_NAME).append(": ").append(getName()).append(", ");
        sb.append(CN_QUERY).append(": ").append(getQuery());
        sb.append("}");
        return sb.toString();
    }
}
