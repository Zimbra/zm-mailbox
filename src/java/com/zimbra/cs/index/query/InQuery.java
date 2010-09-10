/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.index.query;

import java.util.List;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.index.DBQueryOperation;
import com.zimbra.cs.index.IntersectionQueryOperation;
import com.zimbra.cs.index.NoResultsQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.index.UnionQueryOperation;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.service.util.ItemId;

/**
 * Query IN or UNDER a folder.
 *
 * @author tim
 * @author ysasaki
 */
public final class InQuery extends Query {

    public enum In {
        ANY, LOCAL, REMOTE, NONE
    }

    private Folder mFolder;
    private ItemId mRemoteId = null;
    private String mSubfolderPath = null;
    private In mSpecialTarget = null;
    private boolean mIncludeSubfolders = false;

    /**
     * Creates a new {@link InQuery} by a folder ID.
     *
     * @param mbox mailbox
     * @param fid folder ID
     * @param under true to include sub folders, otherwise false
     * @return query
     * @throws ServiceException if an error occurred during mailbox access
     */
    public static InQuery create(Mailbox mbox, int fid, boolean under)
        throws ServiceException {

        if (under && fid == Mailbox.ID_FOLDER_USER_ROOT) {
            return new InQuery(null, null, null, In.ANY, under);
        } else {
            Folder folder = mbox.getFolderById(null, fid);
            return new InQuery(folder, null, null, null, under);
        }
    }

    /**
     * Creates a new {@link InQuery} by a folder name.
     *
     * @param mailbox mailbox
     * @param folder folder name
     * @param under true to include sub folders, otherwise false
     * @return query
     * @throws ServiceException if an error occurred during mailbox access
     */
    public static InQuery create(Mailbox mailbox, String folder, boolean under)
        throws ServiceException {

        Pair<Folder, String> result = mailbox.getFolderByPathLongestMatch(
                null, Mailbox.ID_FOLDER_USER_ROOT, folder);
        return recursiveResolve(mailbox, result.getFirst(),
                result.getSecond(), under);
    }

    /**
     * Creates a new {@link InQuery} with a special folder.
     *
     * @param in special folder
     * @param under true to include sub folders, otherwise false
     * @return query
     */
    public static InQuery create(In in, boolean under) {
        return new InQuery(null, null, null, in, under);
    }

    /**
     * Creates a new {@link InQuery} with item ID.
     *
     * @param mailbox mailbox
     * @param iid item ID
     * @param path sub folder path
     * @param under true to include sub folders, otherwise false
     * @return query
     * @throws ServiceException if an error occurred during mailbox access
     */
    public static InQuery create(Mailbox mailbox, ItemId iid,
            String path, boolean under) throws ServiceException {

        if (iid.belongsTo(mailbox)) { // local
            if (under && iid.getId() == Mailbox.ID_FOLDER_USER_ROOT) {
                return new InQuery(null, iid, path, In.ANY, under);
            }
            // find the base folder
            Pair<Folder, String> result;
            if (!Strings.isNullOrEmpty(path)) {
                result = mailbox.getFolderByPathLongestMatch(null, iid.getId(), path);
            } else {
                Folder folder = mailbox.getFolderById(null, iid.getId());
                result = new Pair<Folder, String>(folder, null);
            }
            return recursiveResolve(mailbox, result.getFirst(),
                    result.getSecond(), under);
        } else { // remote
            return new InQuery(null, iid, path, null, under);
        }
    }

    /**
     * Resolve through local mountpoints until we get to the actual folder,
     * or until we get to a remote folder.
     */
    private static InQuery recursiveResolve(Mailbox mailbox, Folder folder,
            String path, boolean under) throws ServiceException {

        if (!(folder instanceof Mountpoint)) {
            if (path != null) {
                throw MailServiceException.NO_SUCH_FOLDER(
                        folder.getPath() + "/" + path);
            }
            return new InQuery(folder, null, null, null, under);
        } else {
            Mountpoint mpt = (Mountpoint) folder;
            if (mpt.isLocal()) { // local
                if (Strings.isNullOrEmpty(path)) {
                    return new InQuery(folder, null, null, null, under);
                } else {
                    return recursiveResolve(mailbox,
                            mailbox.getFolderById(null, mpt.getRemoteId()),
                            path, under);
                }
            } else { // remote
                return new InQuery(null, mpt.getTarget(), path, null, under);
            }
        }
    }

    private InQuery(Folder folder, ItemId remoteId,
            String path, In special, boolean under) {
        mFolder = folder;
        mRemoteId = remoteId;
        mSubfolderPath = path;
        mSpecialTarget = special;
        mIncludeSubfolders = under;
    }

    @Override
    public QueryOperation getQueryOperation(boolean bool) {
        if (mSpecialTarget != null) {
            if (mSpecialTarget == In.NONE) {
                return new NoResultsQueryOperation();
            } else if (mSpecialTarget == In.ANY) {
                DBQueryOperation dbOp = new DBQueryOperation();
                dbOp.addAnyFolderClause(evalBool(bool));
                return dbOp;
            } else {
                if (evalBool(bool)) {
                    if (mSpecialTarget == In.REMOTE) {
                        DBQueryOperation dbop = new DBQueryOperation();
                        dbop.addIsRemoteClause();
                        return dbop;
                    } else {
                        assert(mSpecialTarget == In.LOCAL);
                        DBQueryOperation dbop = new DBQueryOperation();
                        dbop.addIsLocalClause();
                        return dbop;
                    }
                } else {
                    if (mSpecialTarget == In.REMOTE) {
                        DBQueryOperation dbop = new DBQueryOperation();
                        dbop.addIsLocalClause();
                        return dbop;
                    } else {
                        assert(mSpecialTarget == In.LOCAL);
                        DBQueryOperation dbop = new DBQueryOperation();
                        dbop.addIsRemoteClause();
                        return dbop;
                    }
                }
            }
        }

        DBQueryOperation dbOp = new DBQueryOperation();
        if (mFolder != null) {
            if (mIncludeSubfolders) {
                List<Folder> subFolders = mFolder.getSubfolderHierarchy();

                if (evalBool(bool)) {
                    // (A or B or C)
                    UnionQueryOperation union = new UnionQueryOperation();

                    for (Folder f : subFolders) {
                        DBQueryOperation dbop = new DBQueryOperation();
                        union.add(dbop);
                        if (f instanceof Mountpoint) {
                            Mountpoint mpt = (Mountpoint)f;
                            if (!mpt.isLocal()) {
                                dbop.addInRemoteFolderClause(mpt.getTarget(), "", mIncludeSubfolders, evalBool(bool));
                            } else {
                                // TODO FIXME handle local mountpoints. Don't forget to check for infinite recursion!
                            }
                        } else {
                            dbop.addInClause(f, evalBool(bool));
                        }
                    }
                    return union;
                } else {
                    // -(A or B or C) ==> -A and -B and -C
                    IntersectionQueryOperation iop = new IntersectionQueryOperation();

                    for (Folder f : subFolders) {
                        DBQueryOperation dbop = new DBQueryOperation();
                        iop.addQueryOp(dbop);
                        if (f instanceof Mountpoint) {
                            Mountpoint mpt = (Mountpoint)f;
                            if (!mpt.isLocal()) {
                                dbop.addInRemoteFolderClause(mpt.getTarget(), "", mIncludeSubfolders, evalBool(bool));
                            } else {
                                // TODO FIXME handle local mountpoints.  Don't forget to check for infinite recursion!
                            }

                        } else {
                            dbop.addInClause(f, evalBool(bool));
                        }
                    }
                    return iop;
                }
            } else {
                dbOp.addInClause(mFolder, evalBool(bool));
            }
        } else if (mRemoteId != null) {
            dbOp.addInRemoteFolderClause(mRemoteId, mSubfolderPath, mIncludeSubfolders, evalBool(bool));
        } else {
            assert(false);
        }

        return dbOp;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append(mIncludeSubfolders ? "UNDER," : "IN,");
        if (mSpecialTarget != null) {
            switch (mSpecialTarget) {
                case ANY:
                    out.append("ANY_FOLDER");
                    break;
                case LOCAL:
                    out.append("LOCAL");
                    break;
                case REMOTE:
                    out.append("REMOTE");
                    break;
                case NONE:
                    out.append("NONE");
                    break;
            }
        } else {
            out.append(mRemoteId != null ? mRemoteId.toString() :
                    (mFolder != null ? mFolder.getName() : "ANY_FOLDER"));
            if (mSubfolderPath != null) {
                out.append('/');
                out.append(mSubfolderPath);
            }
        }
    }

}
