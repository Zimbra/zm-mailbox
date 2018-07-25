package com.zimbra.cs.account.callback;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.SolrStopwordManager;

public class SolrSchemaCallback extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry)
            throws ServiceException {
        if (attrName.equals(Provisioning.A_zimbraDefaultAnalyzerStopWords)) {
            SolrStopwordManager stopwordManager = new SolrStopwordManager();
            MultiValueMod mod = multiValueMod(attrsToModify, attrName);
            if (mod.adding()) {
                List<String> curWords = stopwordManager.getStopwords();
                Set<String> newWords = Sets.difference(mod.valuesSet(), Sets.newHashSet(curWords));
                stopwordManager.addStopwords(newWords);
            } else if (mod.replacing()) {
                stopwordManager.deleteAllStopwords();
                stopwordManager.addStopwords(mod.valuesSet());
            } else if (mod.deleting()) {
                stopwordManager.deleteAllStopwords();
            } else if (mod.removing()) {
                stopwordManager.deleteStopwords(mod.values());
            }
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        ZimbraLog.index.info("reloading solr collection after modifying %s", attrName);
        try {
            SolrStopwordManager stopwordManager = new SolrStopwordManager();
            stopwordManager.reloadCollection();
        } catch (ServiceException e) {
            ZimbraLog.index.error("unable to reload solr collection after modifying schema; changes won't be applied until the collection is reloaded", e);
        }
    }

}
