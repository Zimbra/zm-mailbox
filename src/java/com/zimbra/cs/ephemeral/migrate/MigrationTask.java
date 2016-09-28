package com.zimbra.cs.ephemeral.migrate;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.ephemeral.EphemeralInput;
import com.zimbra.cs.ephemeral.EphemeralLocation;
import com.zimbra.cs.ephemeral.LdapEntryLocation;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.MigrationCallback;

/**
 * Class representing the migration of attributes on a single LDAP Entry
 *
 * @author iraykin
 *
 */
public class MigrationTask implements Runnable {

    protected Map<String, AttributeConverter> converters;
    protected MigrationCallback callback;
    protected boolean deleteOriginal;
    protected Entry entry;

    MigrationTask(Entry entry, Map<String, AttributeConverter> converters, MigrationCallback callback, boolean deleteOriginal) {
        this.entry = entry;
        this.converters = converters;
        this.callback = callback;
        this.deleteOriginal = deleteOriginal;
    }

    /**
     * Return the ephemeral location for this migration
     * @return
     */
    public EphemeralLocation getEphemeralLocation() {
        return new LdapEntryLocation(entry);
    }

    private List<Pair<EphemeralInput, Object>> migrateMultivaluedAttr(String attr, Object obj, AttributeConverter converter) {
        String[] values;
        if (obj instanceof String) {
            values = new String[] {(String) obj };
        } else if (obj instanceof String[]) {
            values = (String[]) obj;
        } else {
            ZimbraLog.ephemeral.warn("multivalued attribute converter expects String or String[], got type %s for attribute '%s'", obj.getClass().getName(), attr);
            return Collections.emptyList();
        }
        List<Pair<EphemeralInput, Object>> inputs = new LinkedList<Pair<EphemeralInput, Object>>();
        for (String v: values) {
            EphemeralInput input = converter.convert(attr, v);
            if (input != null) {
                inputs.add(new Pair<EphemeralInput, Object>(input, v));
            }
        }
        return inputs;
    }

    private List<Pair<EphemeralInput, Object>> migrateAttr(String attr, AttributeConverter converter) {
        boolean multiValued = converter.isMultivalued();
        Object obj = multiValued ? entry.getMultiAttr(attr, false, true) : entry.getAttr(attr, false, true);
        if (obj == null) {
            return Collections.emptyList();
        }
        List<Pair<EphemeralInput, Object>> inputs = new LinkedList<Pair<EphemeralInput, Object>>();
        if (multiValued) {
            inputs.addAll(migrateMultivaluedAttr(attr, obj, converter));
        } else {
            EphemeralInput input = converter.convert(attr, obj);
            if (input != null) {
                inputs.add(new Pair<EphemeralInput, Object>(input, obj));
            }
        }
        return inputs;
    }

    @VisibleForTesting
    public void migrateAttributes()  {
        if (entry instanceof NamedEntry) {
            ZimbraLog.ephemeral.debug("migrating attributes to ephemeral storage for account %s", ((NamedEntry) entry).getId());
        }
        List<Pair<EphemeralInput, Object>> inputs = new LinkedList<Pair<EphemeralInput, Object>>();

        for (Map.Entry<String, AttributeConverter> entry: converters.entrySet()) {
            String attrName = entry.getKey();
            AttributeConverter converter = entry.getValue();
            inputs.addAll(migrateAttr(attrName, converter));
        }
        EphemeralLocation location = getEphemeralLocation();
        for (Pair<EphemeralInput, Object> pair: inputs) {
            EphemeralInput input = pair.getFirst();
            String attrName = input.getEphemeralKey().getKey();
            Object origValue = pair.getSecond();
            boolean success = false;
            try {
                callback.setEphemeralData(input, location, attrName, origValue);
                success = true;
            } catch (ServiceException e) {
                ZimbraLog.ephemeral.error("error running MigrationCallback %s on attribute %s",
                        callback.getClass().getName(), input.getEphemeralKey().toString());
            }
            if (success && deleteOriginal) {
                try {
                    callback.deleteOriginal(entry, attrName, origValue, converters.get(attrName));
                } catch (ServiceException e) {
                    ZimbraLog.ephemeral.error("error deleting original LDAP value %s of attribute %s with callback %s",
                            origValue, attrName, callback.getClass().getName());
                }
            }
        }
    }

    @Override
    public void run() {
        migrateAttributes();
    }
}