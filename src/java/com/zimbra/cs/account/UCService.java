package com.zimbra.cs.account;

import java.util.Map;

/**
 * @author pshao
 */
public class UCService extends ZAttrUCService {

    public UCService(String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(name, id, attrs, prov);
    }

    @Override
    public EntryType getEntryType() {
        return EntryType.UCSERVICE;
    }
}
