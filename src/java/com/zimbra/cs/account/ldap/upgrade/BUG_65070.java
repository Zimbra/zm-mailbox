package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;

public class BUG_65070 extends UpgradeOp {
    
    private static final String ATTR_NAME = Provisioning.A_zimbraPrefHtmlEditorDefaultFontFamily;
    private static final String OLD_VALUE = "Times New Roman";
    private static final String NEW_VALUE = "times new roman, new york, times, serif";

    @Override
    void doUpgrade() throws ServiceException {
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
        try {
            doAllCos(zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    Description getDescription() {
        return new Description(this, 
                new String[] {ATTR_NAME}, 
                new EntryType[] {EntryType.COS},
                OLD_VALUE, 
                NEW_VALUE, 
                String.format("Upgrade attribute %s on all cos from \"%s\" to \"%s\"", 
                        ATTR_NAME, OLD_VALUE, NEW_VALUE));
    }
    
    private void doEntry(ZLdapContext zlc, Entry entry) throws ServiceException {
        String entryName = entry.getLabel();
        
        printer.println();
        printer.println("------------------------------");
        printer.println("Checking " + ATTR_NAME + " on " + entryName);
        
        String curValue = entry.getAttr(ATTR_NAME, false);
        if (OLD_VALUE.equals(curValue)) {
            printer.println(String.format(
                    "    Changing %s on cos %s from \"%s\" to \"%s\"", 
                    ATTR_NAME, entryName, curValue, NEW_VALUE));

            Map<String, Object> attr = new HashMap<String, Object>();
            attr.put(ATTR_NAME, NEW_VALUE);
            try {
                modifyAttrs(zlc, entry, attr);
            } catch (ServiceException e) {
                // log the exception and continue
                printer.println("Caught ServiceException while modifying " + entryName + " attribute " + attr);
                printer.printStackTrace(e);
            }
        } else {
            printer.println(
                    String.format("    Current value of %s on cos %s is \"%s\" - not changed",
                    ATTR_NAME, entryName, curValue));
        }
    }
    
    private void doAllCos(ZLdapContext zlc) throws ServiceException {
        List<Cos> coses = prov.getAllCos();
        for (Cos cos : coses) {
            doEntry(zlc, cos);
        }
    }
}
