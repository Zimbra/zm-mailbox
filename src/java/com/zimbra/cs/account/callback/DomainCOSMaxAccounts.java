package com.zimbra.cs.account.callback;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class DomainCOSMaxAccounts extends AttributeCallback {

    @Override
    public void preModify(Map context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry, boolean isCreate)
            throws ServiceException {
        
        String attr = Provisioning.A_zimbraDomainCOSMaxAccounts;
        String addAttr = "+" + attr;
        String delAttr = "-" + attr;
            
        Map<String, String> cur = new HashMap<String, String>();
        if (entry != null) {
            Set<String> curValues = entry.getMultiAttrSet(attr);
            for (String v : curValues) {
                Pair<String, String> parsed = parse(v);
                cur.put(parsed.getFirst(), parsed.getSecond());
            }
            curValues = new HashSet<String>();
        }
            
        // go through the attrsToModify once first to check all removing ones
        for (Object e : attrsToModify.entrySet()) {       
            Map.Entry<String, Object> keyVal = (Map.Entry<String, Object>)e; 
            String aName = keyVal.getKey();
            List<String> vals = getMultiValue(keyVal.getValue());
                
            if (delAttr.equals(aName)) {
                // removing
                for (String v : vals) {
                    Pair<String, String> parsed = parse(v);
                    cur.remove(parsed.getFirst());
                }
            }
        }
        
        // go through the attrsToModify again to check dups for replacing and adding
        for (Object e : attrsToModify.entrySet()) {       
            Map.Entry<String, Object> keyVal = (Map.Entry<String, Object>)e; 
            String aName = keyVal.getKey();
            List<String> vals = getMultiValue(keyVal.getValue());
                
            if (attr.equals(aName)) {
                // replacing
                checkDup(new HashMap<String, String>(), vals);
            } else if (addAttr.equals(aName)) {
                // adding
                checkDup(cur, vals);
            } 
        }
    }
    
    private void checkDup(Map<String, String> curVals, List<String> newVals) throws ServiceException {
        for (String v : newVals) {
            Pair<String, String> parsed = parse(v);
            String other = curVals.get(parsed.getFirst());
            if (other != null)
                throw ServiceException.INVALID_REQUEST("cannot contain multiple values for the same cos " + parsed.getFirst() + 
                        ": " + parsed.getSecond() + ", " + other, null);
            else
                curVals.put(parsed.getFirst(), parsed.getSecond());
        }
    }
    
    private Pair<String, String> parse(String value) throws ServiceException {
        String[] parts = value.split(":");
        if (parts.length != 2)
            throw ServiceException.INVALID_REQUEST("invalid format", null);
        return new Pair<String, String>(parts[0], parts[1]);
    }

    @Override
    public void postModify(Map context, String attrName, Entry entry,
            boolean isCreate) {
    }

}
