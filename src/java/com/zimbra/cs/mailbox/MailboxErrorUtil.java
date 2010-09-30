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

/*
 * Created on August 9, 2010
 */
package com.zimbra.cs.mailbox;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.Db;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;

class MailboxErrorUtil {
    /**
     * Attempt to delete a list of ids whih previously failed due to foreign key constraint violation. Intended to find which items had the FK issue, and provide a bit more detail about them.
     * Throws the original exception argument, or wraps it with additional details if individual deletes fail 
     */
    static void handleCascadeFailure(Mailbox mbox, List<Integer> cascadeIds, ServiceException e) throws ServiceException {
        if (causeMatchesFKFailure(e)) {
            ZimbraLog.mailbox.error("deleting cascadeIds failed due to foreign key constraint failed; attempting to delete individually and find failure");
            LinkedList<Integer> failures = new LinkedList<Integer>();
            for (Integer id: cascadeIds) {
              try {
                  List<Integer> singleItemList = Collections.singletonList(id);
                  ZimbraLog.mailbox.debug("attempting to delete id ["+id+"]");
                  DbMailItem.delete(mbox, singleItemList, false);   
                  ZimbraLog.mailbox.debug("deleted ["+id+"] OK");
              } catch (ServiceException se) {
                  ZimbraLog.mailbox.error("deleted FAILED for ["+id+"] due to exception",se);
                  failures.add(id);
              }
            }
            if (!failures.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                //find the id,type,subject for each entry. this should help us figure out what's not being removed correctly...
                for (Integer id: failures) {
                    MailItem item = mbox.getItemById(id, MailItem.TYPE_UNKNOWN);
                    String logMsg = "failure item id["+id+"] type["+item.getType()+":"+
                        item.getClass().getSimpleName()+"] subject["+item.getSubject()+"] size["+
                        item.getSize()+"] folder["+item.getFolderId()+"] parent["+
                        item.getParentId()+"]"; 
                    sb.append(logMsg).append("\r\n");
                    ZimbraLog.mailbox.error(logMsg);
                    if (item instanceof Conversation) {
                        Conversation conv = (Conversation) item;
                        List<Message> children = conv.getMessages();
                        if (children != null && children.size() > 0) {
                            ZimbraLog.mailbox.error("converstaion["+conv.getId()+"] still has "+children.size()+" children.");
                            for (Message msg: children) {
                                logMsg="child["+msg+"] type["+msg.getType()+"] subject["+msg.getSubject()+"] in folder["+
                                        msg.getFolderId()+":"+msg.getFolder().getName()+"] still associated with conv ["+conv.getId()+"]";
                                sb.append(logMsg);
                                ZimbraLog.mailbox.error(logMsg);
                            }
                        }
                    } else if (item instanceof Folder) {
                        Folder folder = (Folder) item;
                        List<UnderlyingData> children = DbMailItem.getByFolder(folder, MailItem.TYPE_UNKNOWN, SortBy.NONE);
                        if (children != null && children.size() > 0) {
                            ZimbraLog.mailbox.error("folder["+folder.getId()+"] still has "+children.size()+" children.");
                            for (UnderlyingData data: children) {
                                logMsg = "child["+item+"] type["+data.type+"] subject["+data.subject+"] still present in folder ["+folder.getId()+"]";
                                sb.append(logMsg);
                                ZimbraLog.mailbox.error(logMsg);
                            }
                        }
                    } else {
                        ZimbraLog.mailbox.warn("cascade failure in unexpected type ["+item.getType()+":"+item.getClass().getSimpleName()+"] other than folder or conversation");
                    }
                }
                throw ServiceException.FAILURE(e.getMessage()+"---"+sb.toString(),e);
            } else {
                throw ServiceException.FAILURE(e.getMessage()+"--- no additional data available from attempting individual deletes.",e);
            }
        } else {
            throw e;    
        }
    }
    
    private static boolean causeMatchesFKFailure(Throwable t) {
        if (t instanceof SQLException && (Db.errorMatches(((SQLException)t),Db.Error.FOREIGN_KEY_CHILD_EXISTS))) {
            return true; 
        } else if (t.getCause() != null) {
            return causeMatchesFKFailure(t.getCause());
        }
        return false;
    }

}
