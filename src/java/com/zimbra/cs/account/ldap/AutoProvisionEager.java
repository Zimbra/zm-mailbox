package com.zimbra.cs.account.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ZAttrProvisioning.AutoProvMode;
import com.zimbra.cs.account.ldap.entry.LdapEntry;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.LdapUtilCommon;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;
import com.zimbra.cs.ldap.SearchLdapOptions.StopIteratingException;

public class AutoProvisionEager extends AutoProvision {
    
    // public for unittest
    public AutoProvisionEager(LdapProv prov, Domain domain) {
        super(prov, domain);
    }
    
    // public for unittest
    public void handleBatch(ZLdapContext zlc) throws ServiceException {
        if (!autoProvisionEnabled()) {
            throw ServiceException.FAILURE("EAGER auto provisioning is not enabled on domain " + domain.getName(), null);
        }
        
        if (!lockDomain(zlc)) {
            ZimbraLog.account.info("EAGER auto provision: skip domain " + domain.getName() +
                    " on server " + prov.getLocalServer().getName());
            return;
        }
        
        List<ExternalEntry> entries = searchAccounts();
        
        String latestCreateTimestamp = null;
        try {
            for (ExternalEntry entry : entries) {
                ZAttributes externalAttrs = entry.attrs;
                
                String acctZimbraName = mapName(externalAttrs, null);
                createAccount(acctZimbraName, externalAttrs);
                
                // keep track of the last createTimeStamp in the external directory.
                // Out next batch will fetch entries with createTimeStamp later than 
                // the last create timestamp in this batch.
                String cts = (String) externalAttrs.getAttrString("createTimeStamp");
                latestCreateTimestamp = LdapUtilCommon.getLaterTimestamp(latestCreateTimestamp, cts);
            }
        } catch (ServiceException e) {
            // rethrow
            throw e;
        } finally {
            // update the last create timestamp
            if (latestCreateTimestamp != null) {
                domain.setAutoProvLastPolledTimestampAsString(latestCreateTimestamp);
            }
        }
        
    }

    @Override
    Account handle() throws ServiceException {
        throw new UnsupportedOperationException();
    }
    
    private boolean autoProvisionEnabled() {
        Set<String> modesEnabled = domain.getMultiAttrSet(Provisioning.A_zimbraAutoProvMode);
        return modesEnabled.contains(AutoProvMode.EAGER.name());
    }
    
    private boolean lockDomain(ZLdapContext zlc) throws ServiceException {
        Server localServer = prov.getLocalServer();
        
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().domainLockedForEagerAutoProvision();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAutoProvLock, localServer.getId());
        
        return prov.getHelper().tesAndModifyEntry(zlc, ((LdapEntry)domain).getDN(), filter, attrs, 
                domain, LdapUsage.AUTO_PROVISION);
    }
    
    private static class ExternalEntry {
        private String dn;
        private ZAttributes attrs;
        
        private ExternalEntry(String dn, ZAttributes attrs) {
            this.dn = dn;
            this.attrs = attrs;
        }
    }
    
    private List<ExternalEntry> searchAccounts() throws ServiceException {
        int maxResults = domain.getAutoProvBatchSize();
        String lastPolledAt = domain.getAutoProvLastPolledTimestampAsString();
        String[] returnAttrs = null; // TODO: only fetches attrs in the map
        
        final List<ExternalEntry> entries = new ArrayList<ExternalEntry>();
        
        SearchLdapVisitor visitor = new SearchLdapVisitor(false) {
            @Override
            public void visit(String dn, IAttributes ldapAttrs)
            throws StopIteratingException {
                entries.add(new ExternalEntry(dn, (ZAttributes)ldapAttrs));
            }
        };

        AutoProvision.searchAutoProvDirectory(prov, domain, null, null, 
                lastPolledAt, returnAttrs, maxResults, visitor);
        
        return entries;
    }
    
    public static void main(String[] args) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Domain domain = prov.get(DomainBy.name, "phoebe.mbp");
        AutoProvisionEager autoProv = new AutoProvisionEager((LdapProv) prov, domain);
        
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UNITTEST);
        try {
        autoProv.handleBatch(zlc);
        autoProv.handleBatch(zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

}
