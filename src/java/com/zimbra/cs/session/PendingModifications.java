/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Nov 28, 2004
 */
package com.zimbra.cs.session;

import java.util.HashMap;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;


public final class PendingModifications {
    public static final class Change {
	    public static final int UNMODIFIED         = 0x00000000;
        public static final int MODIFIED_UNREAD    = 0x00000001;
	    public static final int MODIFIED_TAGS      = 0x00000002;
	    public static final int MODIFIED_FLAGS     = 0x00000004;
        public static final int MODIFIED_CONFIG    = 0x00000008;
        public static final int MODIFIED_SIZE      = 0x00000010;
        public static final int MODIFIED_DATE      = 0x00000020;
        public static final int MODIFIED_SUBJECT   = 0x00000040;
        public static final int MODIFIED_MSG_COUNT = 0x00000080;
	    public static final int MODIFIED_FOLDER    = 0x00000100;
	    public static final int MODIFIED_PARENT    = 0x00000200;
	    public static final int MODIFIED_CHILDREN  = 0x00000400;
        public static final int MODIFIED_SENDERS   = 0x00000800;
        public static final int MODIFIED_NAME      = 0x00001000;
	    public static final int MODIFIED_COLOR     = 0x00002000;
	    public static final int MODIFIED_POSITION  = 0x00004000;
	    public static final int MODIFIED_QUERY     = 0x00008000;
        public static final int MODIFIED_CONTENT   = 0x00010000;
        public static final int MODIFIED_INVITE    = 0x00020000;
        public static final int MODIFIED_URL       = 0x00040000;
        public static final int MODIFIED_IMAP_UID  = 0x00080000;
        public static final int MODIFIED_VIEW      = 0x00100000;
        public static final int MODIFIED_ACL       = 0x00200000;
        public static final int MODIFIED_CONFLICT  = 0x00400000;
        public static final int INTERNAL_ONLY      = 0x10000000;
        public static final int ALL_FIELDS         = ~0;
	
	    public Object what;
	    public int    why;
	
	    Change(Object thing, int reason)  { what = thing;  why = reason; }
	}

	public HashMap created, modified, deleted;

    public boolean hasNotifications() {
        return ((deleted  != null && deleted.size() > 0) ||
                (created  != null && created.size() > 0) ||
                (modified != null && modified.size() > 0));
    }

    public void recordCreated(MailItem item) {
//        ZimbraLog.mailbox.debug("--> NOTIFY: created " + item.getId());
        if (created == null)
            created = new HashMap();
        created.put(new Integer(item.getId()), item);
    }

    public void recordDeleted(int id) {
        Integer key = new Integer(id);
        delete(key, key);
    }
    public void recordDeleted(MailItem item) {
        delete(new Integer(item.getId()), item);
    }
    private void delete(Integer key, Object value) {
//        ZimbraLog.mailbox.debug("--> NOTIFY: deleted " + key);
        if (created != null)
            if (created.remove(key) != null)
                return;
        if (modified != null)
            modified.remove(key);
        if (deleted == null)
            deleted = new HashMap();
        deleted.put(key, value);
    }

    public void recordModified(Mailbox mbox, int reason) {
        recordModified(new Integer(0), mbox, reason);
    }
    public void recordModified(MailItem item, int reason) {
        recordModified(new Integer(item.getId()), item, reason);
    }
    private void recordModified(Integer key, Object item, int reason) {
//        ZimbraLog.mailbox.debug("--> NOTIFY: modified " + key + " (" + reason + ')');
        Change chg = null;
        if (created != null && created.containsKey(key))
            return;
        else if (deleted != null && deleted.containsKey(key))
            return;
        else if (modified == null)
            modified = new HashMap();
        else {
            chg = (Change) modified.get(key);
            if (chg != null) {
                chg.what = item;
                chg.why |= reason;
            }
        }
        if (chg == null)
            chg = new Change(item, reason);
        modified.put(key, chg);
    }
    
    public void clear()  { created = deleted = modified = null; }
}