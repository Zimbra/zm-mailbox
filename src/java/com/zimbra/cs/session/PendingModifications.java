/*
 * Created on Nov 28, 2004
 */
package com.zimbra.cs.session;

import java.util.HashMap;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
//import com.zimbra.cs.util.ZimbraLog;


public final class PendingModifications {
    public static final class Change {
	    public static final int UNMODIFIED         = 0x000000;
        public static final int MODIFIED_UNREAD    = 0x000001;
	    public static final int MODIFIED_TAGS      = 0x000002;
	    public static final int MODIFIED_FLAGS     = 0x000004;
        public static final int MODIFIED_CONFIG    = 0x000008;
        public static final int MODIFIED_SIZE      = 0x000010;
        public static final int MODIFIED_DATE      = 0x000020;
        public static final int MODIFIED_MSG_COUNT = 0x000080;
	    public static final int MODIFIED_FOLDER    = 0x000100;
	    public static final int MODIFIED_PARENT    = 0x000200;
	    public static final int MODIFIED_CHILDREN  = 0x000400;
        public static final int MODIFIED_SENDERS   = 0x000800;
        public static final int MODIFIED_NAME      = 0x001000;
	    public static final int MODIFIED_QUERY     = 0x002000;
	    public static final int MODIFIED_POSITION  = 0x004000;
	    public static final int MODIFIED_COLOR     = 0x008000;
        public static final int MODIFIED_CONTENT   = 0x010000;
        public static final int MODIFIED_INVITE    = 0x020000;
        public static final int MODIFIED_IMAP_UID  = 0x040000;
        public static final int INTERNAL_ONLY      = 0x800000;
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