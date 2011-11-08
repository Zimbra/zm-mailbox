package com.zimbra.cs.account;

import java.util.Map;

public class MockCos extends Cos {

    public MockCos(String name, String id, Map<String, Object> attrs,
            Provisioning prov) {
        super(name, id, attrs, prov);
    }
}
