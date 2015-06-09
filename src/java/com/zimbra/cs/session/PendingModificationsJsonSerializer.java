package com.zimbra.cs.session;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.ser.std.SerializerBase;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ChangeMeta;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.session.PendingModifications.ModificationKeyMeta;

public class PendingModificationsJsonSerializer implements PendingModificationsSerializer {
    public static final String CONTENT_TYPE = "application/json";
    static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public String getContentEncoding() {
        return null;
    }

    @Override
    public byte[] serialize(PendingModifications pendingMods) throws IOException {

        // assemble temporary created, modified, deleted with Metadata
        LinkedHashMap<ModificationKeyMeta, String> metaCreated = null;
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
        Map<ModificationKeyMeta, ChangeMeta> metaModified = getSerializable(pendingMods.modified);
        Map<ModificationKeyMeta, ChangeMeta> metaDeleted = getSerializable(pendingMods.deleted);

        Holder holder = new Holder(pendingMods.changedTypes, metaCreated, metaModified, metaDeleted);
        byte[] json = objectMapper.writeValueAsBytes(holder);
        return json;
    }

    @Override
    public PendingModifications deserialize(Mailbox mbox, byte[] data) throws IOException, ClassNotFoundException, ServiceException {
        Holder holder = objectMapper.readValue(data, Holder.class);
        PendingModifications pms = new PendingModifications();
        pms.changedTypes = holder.changedTypes;

        if (holder.created != null) {
            pms.created = new LinkedHashMap<ModificationKey, MailItem>();
            Iterator<Entry<ModificationKeyMeta, String>> iter = holder.created.entrySet().iterator();
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

        pms.modified = PendingModifications.getOriginal(mbox, holder.modified);
        pms.deleted = PendingModifications.getOriginal(mbox, holder.deleted);

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


    public static class ModificationKeyMetaSerializer extends SerializerBase<ModificationKeyMeta> {
        public ModificationKeyMetaSerializer() {
            super(ModificationKeyMeta.class);
        }
        @Override
        public void serialize(ModificationKeyMeta value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeFieldName(value.accountId + "." + value.itemId);
        }
    }


    public static class ModificationKeyMetaDeserializer extends KeyDeserializer {
        public ModificationKeyMetaDeserializer() {
        }
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            String[] parts = key.split("\\.");
            String accountId = parts[0];
            if (accountId.startsWith("\"") && accountId.endsWith("\"")) {
                accountId = accountId.substring(1, accountId.length() - 2);
            }
            Integer itemId = new Integer(parts[1]);
            return new ModificationKeyMeta(accountId, itemId);
        }
    }


    static class Holder {
        @JsonProperty
        SortedSet<MailItem.Type> changedTypes;

        @JsonSerialize(keyUsing=ModificationKeyMetaSerializer.class)
        @JsonDeserialize(keyUsing=ModificationKeyMetaDeserializer.class)
        LinkedHashMap<ModificationKeyMeta, String> created;

        @JsonSerialize(keyUsing=ModificationKeyMetaSerializer.class)
        @JsonDeserialize(keyUsing=ModificationKeyMetaDeserializer.class)
        Map<ModificationKeyMeta, ChangeMeta> modified;

        @JsonSerialize(keyUsing=ModificationKeyMetaSerializer.class)
        @JsonDeserialize(keyUsing=ModificationKeyMetaDeserializer.class)
        Map<ModificationKeyMeta, ChangeMeta> deleted;

        @JsonCreator
        Holder(
                @JsonProperty("changedTypes") Set<MailItem.Type> changedTypes,
                @JsonProperty("created") LinkedHashMap<ModificationKeyMeta, String> created,
                @JsonProperty("modified") Map<ModificationKeyMeta, ChangeMeta> modified,
                @JsonProperty("deleted") Map<ModificationKeyMeta, ChangeMeta> deleted)
        {
            this.changedTypes = new TreeSet<MailItem.Type>(changedTypes);
            this.created = created;
            this.modified = modified;
            this.deleted = deleted;
        }
    }
}