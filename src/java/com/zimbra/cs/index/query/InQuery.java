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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.index.DBQueryOperation;
import com.zimbra.cs.index.IntersectionQueryOperation;
import com.zimbra.cs.index.NoResultsQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.index.UnionQueryOperation;
import com.zimbra.cs.index.query.parser.QueryParser;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.service.util.ItemId;

/**
 * Query in or under a folder.
 *
 * @author tim
 * @author ysasaki
 */
public final class InQuery extends Query {
    public static final Integer IN_ANY_FOLDER = new Integer(-2);
    public static final Integer IN_LOCAL_FOLDER = new Integer(-3);
    public static final Integer IN_REMOTE_FOLDER = new Integer(-4);
    public static final Integer IN_NO_FOLDER = new Integer(-5);

    private Folder mFolder;
    private ItemId mRemoteId = null;
    private String mSubfolderPath = null;
    private Integer mSpecialTarget = null;
    private boolean mIncludeSubfolders = false;

    public static Query Create(Mailbox mailbox, int mod, Integer folderId,
            boolean includeSubfolders) throws ServiceException {
        if (folderId < 0) {
            return new InQuery(null, null, null, folderId,
                    includeSubfolders, mod);
        } else if (includeSubfolders &&
                folderId == Mailbox.ID_FOLDER_USER_ROOT) {
            return new InQuery(null, null, null, IN_ANY_FOLDER,
                    includeSubfolders, mod);
        } else {
            Folder folder = mailbox.getFolderById(null, folderId.intValue());
            return new InQuery(folder, null, null, null,
                    includeSubfolders, mod);
        }
    }

    public static Query Create(Mailbox mailbox, int mod, String folderName,
            boolean includeSubfolders) throws ServiceException {
        Pair<Folder, String> result = mailbox.getFolderByPathLongestMatch(
                null, Mailbox.ID_FOLDER_USER_ROOT, folderName);
        return recursiveResolve(mailbox, mod, result.getFirst(),
                result.getSecond(), includeSubfolders);
    }

    public static Query Create(Mailbox mailbox, int mod, ItemId iid,
            String subfolderPath, boolean includeSubfolders) throws ServiceException {

        if (iid.belongsTo(mailbox)) { // local
            if (includeSubfolders &&
                    iid.getId() == Mailbox.ID_FOLDER_USER_ROOT) {
                return new InQuery(null, iid, subfolderPath, IN_ANY_FOLDER,
                        includeSubfolders, mod);
            }
            // find the base folder
            Pair<Folder, String> result;
            if (subfolderPath != null && subfolderPath.length() > 0) {
                result = mailbox.getFolderByPathLongestMatch(null,
                        iid.getId(), subfolderPath);
            } else {
                Folder f = mailbox.getFolderById(null, iid.getId());
                result = new Pair<Folder, String>(f, null);
            }
            return recursiveResolve(mailbox, mod, result.getFirst(),
                    result.getSecond(), includeSubfolders);
        } else { // remote
            return new InQuery(null, iid, subfolderPath, null,
                    includeSubfolders, mod);
        }
    }

    /**
     * Resolve through local mountpoints until we get to the actual folder,
     * or until we get to a remote folder.
     */
    private static Query recursiveResolve(Mailbox mailbox, int mod,
            Folder baseFolder, String subfolderPath,
            boolean includeSubfolders) throws ServiceException {

        if (!(baseFolder instanceof Mountpoint)) {
            if (subfolderPath != null) {
                throw MailServiceException.NO_SUCH_FOLDER(
                        baseFolder.getPath() + "/" + subfolderPath);
            }
            return new InQuery(baseFolder, null, null, null,
                    includeSubfolders, mod);
        } else {
            Mountpoint mpt = (Mountpoint) baseFolder;
            if (mpt.isLocal()) { // local
                if (subfolderPath == null || subfolderPath.length() == 0) {
                    return new InQuery(baseFolder, null, null, null,
                            includeSubfolders, mod);
                } else {
                    Folder newBase = mailbox.getFolderById(null,
                            mpt.getRemoteId());
                    return recursiveResolve(mailbox, mod,
                            newBase, subfolderPath, includeSubfolders);
                }
            } else { // remote
                return new InQuery(null, mpt.getTarget(), subfolderPath,
                        null, includeSubfolders, mod);
            }
        }
    }

    private InQuery(Folder folder, ItemId remoteId,
            String subfolderPath, Integer specialTarget,
            boolean includeSubfolders, int mod) {
        super(mod, QueryParser.IN);
        mFolder = folder;
        mRemoteId = remoteId;
        mSubfolderPath = subfolderPath;
        mSpecialTarget = specialTarget;
        mIncludeSubfolders = includeSubfolders;
    }

    @Override
    public QueryOperation getQueryOperation(boolean truth) {
        if (mSpecialTarget != null) {
            if (mSpecialTarget == IN_NO_FOLDER) {
                return new NoResultsQueryOperation();
            } else if (mSpecialTarget == IN_ANY_FOLDER) {
                DBQueryOperation dbOp = new DBQueryOperation();
                dbOp.addAnyFolderClause(calcTruth(truth));
                return dbOp;
            } else {
                if (calcTruth(truth)) {
                    if (mSpecialTarget == IN_REMOTE_FOLDER) {
                        DBQueryOperation dbop = new DBQueryOperation();
                        dbop.addIsRemoteClause();
                        return dbop;
                    } else {
                        assert(mSpecialTarget == IN_LOCAL_FOLDER);
                        DBQueryOperation dbop = new DBQueryOperation();
                        dbop.addIsLocalClause();
                        return dbop;
                    }
                } else {
                    if (mSpecialTarget == IN_REMOTE_FOLDER) {
                        DBQueryOperation dbop = new DBQueryOperation();
                        dbop.addIsLocalClause();
                        return dbop;
                    } else {
                        assert(mSpecialTarget == IN_LOCAL_FOLDER);
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

                if (calcTruth(truth)) {
                    // (A or B or C)
                    UnionQueryOperation union = new UnionQueryOperation();

                    for (Folder f : subFolders) {
                        DBQueryOperation dbop = new DBQueryOperation();
                        union.add(dbop);
                        if (f instanceof Mountpoint) {
                            Mountpoint mpt = (Mountpoint)f;
                            if (!mpt.isLocal()) {
                                dbop.addInRemoteFolderClause(mpt.getTarget(), "", mIncludeSubfolders, calcTruth(truth));
                            } else {
                                // TODO FIXME handle local mountpoints. Don't forget to check for infinite recursion!
                            }

                        } else {
                            dbop.addInClause(f, calcTruth(truth));
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
                                dbop.addInRemoteFolderClause(mpt.getTarget(), "", mIncludeSubfolders, calcTruth(truth));
                            } else {
                                // TODO FIXME handle local mountpoints.  Don't forget to check for infinite recursion!
                            }

                        } else {
                            dbop.addInClause(f, calcTruth(truth));
                        }
                    }
                    return iop;
                }
            } else {
                dbOp.addInClause(mFolder, calcTruth(truth));
            }
        } else if (mRemoteId != null) {
            dbOp.addInRemoteFolderClause(mRemoteId, mSubfolderPath, mIncludeSubfolders, calcTruth(truth));
        } else {
            assert(false);
        }

        return dbOp;
    }

    @Override
    public StringBuilder dump(StringBuilder out) {
        super.dump(out);
        if (mSpecialTarget != null) {
            if (!mIncludeSubfolders) {
                out.append(",IN:");
            } else {
                out.append(",UNDER:");
            }

            if (mSpecialTarget == IN_ANY_FOLDER) {
                out.append("ANY_FOLDER");
            } else if (mSpecialTarget == IN_LOCAL_FOLDER) {
                out.append("LOCAL");
            } else if (mSpecialTarget == IN_REMOTE_FOLDER) {
                out.append("REMOTE");
            }
        } else {
            out.append(',');
            out.append(mIncludeSubfolders ? "UNDER" : "IN");
            out.append(':');
            out.append(mRemoteId != null ? mRemoteId.toString() :
                    (mFolder != null ? mFolder.getName() : "ANY_FOLDER"));
            if (mSubfolderPath != null) {
                out.append('/');
                out.append(mSubfolderPath);
            }
        }
        return out.append(')');
    }

}
