package com.zimbra.cs.ephemeral;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Alias;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.UCService;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.ldap.entry.LdapDynamicGroup.DynamicUnit;
import com.zimbra.cs.account.ldap.entry.LdapDynamicGroup.StaticUnit;
import com.zimbra.cs.account.ldap.entry.LdapMimeTypeBase;

/**
 * Implementation of @EphemeralLocation used for LDAP entries.
 *
 * This class exposes an additional getEntry() method to simply return the
 * LDAP entry it was instantiated with. This is used by @LdapEphemeralStore
 * to simplify the process of accessing the entry to modify.
 *
 * @author iraykin
 *
 */
public class LdapEntryLocation extends EphemeralLocation {
    private EntryType entryType;
    private String entryValue;
    private Entry entry;

    public LdapEntryLocation(Entry entry) {
        this.entry = entry;
        this.entryType = entry.getEntryType();
        this.entryValue = getEntryId(entry);
    }

    private String getEntryId(Entry entry) {
        switch(entryType) {
        case ACCOUNT:
            return ((Account) entry).getId();
        case ALIAS:
            Alias alias = (Alias) entry;
            return ((Alias) entry).getId();
        case CALRESOURCE:
            return ((CalendarResource) entry).getId();
        case COS:
            return ((Cos) entry).getId();
        case DATASOURCE:
            return ((DataSource) entry).getId();
        case DISTRIBUTIONLIST:
            return ((DistributionList) entry).getId();
        case DOMAIN:
            return ((Domain) entry).getId();
        case DYNAMICGROUP:
            return ((DynamicGroup) entry).getId();
        case DYNAMICGROUP_DYNAMIC_UNIT:
            return ((DynamicUnit) entry).getId();
        case DYNAMICGROUP_STATIC_UNIT:
            return ((StaticUnit) entry).getId();
        case GLOBALCONFIG:
            return EntryType.GLOBALCONFIG.toString();
        case GLOBALGRANT:
            return EntryType.GLOBALGRANT.toString();
        case IDENTITY:
            return ((Identity) entry).getId();
        case MIMETYPE:
            return ((LdapMimeTypeBase) entry).getLabel();
        case SERVER:
            return ((Server) entry).getId();
        case SIGNATURE:
            return ((Signature) entry).getId();
        case UCSERVICE:
            return ((UCService) entry).getId();
        case XMPPCOMPONENT:
            return ((XMPPComponent) entry).getId();
        case ZIMLET:
            return ((Zimlet) entry).getId();
        default:
            return null;
        }
    }

    @Override
    public String[] getLocation() {
        return new String[] { entryType.toString(), entryValue };
        }

    public Entry getEntry() {
        return entry;
    }
}

