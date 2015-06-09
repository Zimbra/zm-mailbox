package com.zimbra.cs.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.xerces.impl.dv.util.Base64;
import org.springframework.amqp.core.MessageProperties;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ChangeMeta;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.session.PendingModifications.ModificationKeyMeta;

public class PendingModificationsJavaSerializer implements PendingModificationsSerializer {
    public static final String CONTENT_TYPE = MessageProperties.CONTENT_TYPE_SERIALIZED_OBJECT;

    @Override
    public String getContentType() {
        return CONTENT_TYPE + "; class=" + PendingModifications.class.getName();
    }

    @Override
    public String getContentEncoding() {
        return "base64";
    }

    @Override
    public byte[] serialize(PendingModifications pendingMods) throws IOException {

        // assemble temporary created, modified, deleted with Metadata
        LinkedHashMap<ModificationKeyMeta, String> metaCreated = null;
        Map<ModificationKeyMeta, ChangeMeta> metaModified = null;
        Map<ModificationKeyMeta, ChangeMeta> metaDeleted = null;

        if (pendingMods.created != null) {
            metaCreated = new LinkedHashMap<ModificationKeyMeta, String>();
            Iterator<Entry<ModificationKey, MailItem>> iter = pendingMods.created.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<ModificationKey, MailItem> entry = iter.next();
                ModificationKeyMeta keyMeta = new ModificationKeyMeta(entry.getKey().getAccountId(), entry.getKey().getItemId());
                MailItem item = entry.getValue();
                Metadata meta = item.serializeUnderlyingData();
                metaCreated.put(keyMeta, meta.toString());
            }
        }
        metaModified = getSerializable(pendingMods.modified);
        metaDeleted = getSerializable(pendingMods.deleted);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(pendingMods.changedTypes);
        oos.writeObject(metaCreated);
        oos.writeObject(metaModified);
        oos.writeObject(metaDeleted);
        oos.flush();
        oos.close();
        return Base64.encode(bos.toByteArray()).getBytes();
    }

    @Override
    @SuppressWarnings("unchecked")
    public PendingModifications deserialize(Mailbox mbox, byte[] data) throws IOException, ClassNotFoundException, ServiceException {
        ByteArrayInputStream bis = new ByteArrayInputStream(Base64.decode(new String(data)));
        ObjectInputStream ois = new ObjectInputStream(bis);
        PendingModifications pms = new PendingModifications();
        pms.changedTypes = (Set<Type>) ois.readObject();

        LinkedHashMap<ModificationKeyMeta, String> metaCreated = (LinkedHashMap<ModificationKeyMeta, String>) ois.readObject();
        if (metaCreated != null) {
            pms.created = new LinkedHashMap<ModificationKey, MailItem>();
            Iterator<Entry<ModificationKeyMeta, String>> iter = metaCreated.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<ModificationKeyMeta, String> entry = iter.next();
                Metadata meta = new Metadata(entry.getValue());
                MailItem.UnderlyingData ud = new MailItem.UnderlyingData();
                ud.deserialize(meta);
                MailItem item = MailItem.constructItem(mbox, ud, true);
                if (item instanceof Folder) {
                    Folder folder = ((Folder) item);
                    folder.setParent(mbox.getFolderById(null, folder.getFolderId()));

                }
                ModificationKey key = new ModificationKey(entry.getKey().accountId, entry.getKey().itemId);
                pms.created.put(key, item);
            }
        }

        Map<ModificationKeyMeta, ChangeMeta> metaModified =  (Map<ModificationKeyMeta, ChangeMeta>) ois.readObject();
        pms.modified = PendingModifications.getOriginal(mbox, metaModified);

        Map<ModificationKeyMeta, ChangeMeta> metaDeleted =  (Map<ModificationKeyMeta, ChangeMeta>) ois.readObject();
        pms.deleted = PendingModifications.getOriginal(mbox, metaDeleted);

        return pms;
    }

    static Map<ModificationKeyMeta, ChangeMeta> getSerializable(Map<ModificationKey, Change> map) {
        if (map == null) {
            return null;
        }
        Map<ModificationKeyMeta, ChangeMeta> ret = new LinkedHashMap<ModificationKeyMeta, ChangeMeta>();
        Iterator<Entry<ModificationKey, Change>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<ModificationKey, Change> entry = iter.next();
            Change change = entry.getValue();
            ChangeMeta.ObjectType whatType;
            String metaWhat;
            ChangeMeta.ObjectType metaPreModifyObjType = null;
            String metaPreModifyObj = null;
            if (change.what instanceof MailItem) {
                whatType = ChangeMeta.ObjectType.MAILITEM;
                metaWhat = ((MailItem) change.what).serializeUnderlyingData().toString();
            } else if (change.what instanceof MailItem.Type) {
                whatType = ChangeMeta.ObjectType.MAILITEMTYPE;
                metaWhat = ((MailItem.Type) change.what).name();
            } else if (change.what instanceof Mailbox) {
                whatType = ChangeMeta.ObjectType.MAILBOX;
                // do not serialize mailbox. let the other server load the mailbox again.
                metaWhat = null;
            } else {
                ZimbraLog.session.warn("Unexpected mailbox change : " + change.what);
                continue;
            }

            if (change.preModifyObj instanceof MailItem) {
                metaPreModifyObjType = ChangeMeta.ObjectType.MAILITEM;
                metaPreModifyObj =  ((MailItem) change.preModifyObj).serializeUnderlyingData().toString();
            } else if (change.preModifyObj instanceof MailItem.Type) {
                metaPreModifyObjType = ChangeMeta.ObjectType.MAILITEMTYPE;
                metaPreModifyObj = ((MailItem.Type) change.preModifyObj).name();
            } else if (change.preModifyObj instanceof Mailbox) {
                metaPreModifyObjType = ChangeMeta.ObjectType.MAILBOX;
                metaPreModifyObj = null;
            }

            ModificationKeyMeta keyMeta = new ModificationKeyMeta(entry.getKey().getAccountId(), entry.getKey().getItemId());
            ChangeMeta changeMeta = new ChangeMeta(whatType, metaWhat, change.why, metaPreModifyObjType, metaPreModifyObj);
            ret.put(keyMeta, changeMeta);
        }
        return ret;
    }
}