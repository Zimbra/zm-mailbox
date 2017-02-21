/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap.custom;

import java.util.Map;

import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.ldap.SpecialAttrs;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;

/*
 * This class and CustomerLdapProvisioning should really go under the
 * Velodrome extension.  But there is an initializing sequence problem
 * that the class specified in the zimbra_class_provisioning key cannot
 * be in an extension.  Currently it just calls Class.forName, which does
 * not take into account that the class could be in an extension.
 * However even if it does and uses ExtensionUtil.loadClass
 * (we can use {ext-name}:{class-name} format for the zimbra_class_provisioning key),
 * the class still cannot be loaded because:
 *
 * when server is starting up, Provisioning.getInstance is first called from
 * PrivilegedServlet.init, then called from SoapServlet.init() ... -> Config.init(),
 * but ExtensionUtil.loadAll is not called until SoapServlet.init -> Zimbra.startup(),
 * which happens later.
 *
 * For now the Velodrome Provisioning impl will have to reside in the core product.
 *
 * While CustomLdapProvisioning and CustomLdapDIT try to make DIT layout as generic
 * as possible, it was implemented to meet the requirement from Velodrome.  It provides
 * flexibility and differs from the default DIT via localconfig keys as follows:
 *
 * See ZimbraCustomerServices/velodrome/ZimbraServer/docs/custom-DIT.txt for detailed
 * descrittion of the functionalities of the custom DIT.
 *
 */

public class CustomLdapDIT extends LdapDIT {

    private final String DEFAULT_BASE_RDN_DOMAIN = "cn=domains";
    private String BASE_DN_DOMAIN;

    public CustomLdapDIT(LdapProv prov) {
        super(prov);
    }

    private String getLC(KnownKey key, String defaultValue) {
        String lcValue = key.value();

        if (StringUtil.isNullOrEmpty(lcValue))
            return defaultValue;
        else
            return lcValue;
    }

    private String getLCAndValidateUnderConfigBranchDN(KnownKey key, String defaultValue) {
        String dn = getLC(key, defaultValue);

        if (!validateUnderDN(BASE_DN_CONFIG_BRANCH, dn)) {
            ZimbraLog.account.warn("dn " + dn + " must be under " + BASE_DN_CONFIG_BRANCH + ", localconfig value " + dn + " ignored, using default value " + defaultValue);
            dn = defaultValue;
        }

        return dn;
    }

    @Override
    protected void init() {
        BASE_DN_CONFIG_BRANCH = getLC(LC.ldap_dit_base_dn_config, DEFAULT_CONFIG_BASE_DN);
        BASE_DN_MAIL_BRANCH   = getLC(LC.ldap_dit_base_dn_mail, DEFAULT_MAIL_BASE_DN).toLowerCase();

        BASE_RDN_ACCOUNT              = "";
        BASE_RDN_DYNAMICGROUP         = "";

        NAMING_RDN_ATTR_USER          = getLC(LC.ldap_dit_naming_rdn_attr_user,         DEFAULT_NAMING_RDN_ATTR_USER);
        NAMING_RDN_ATTR_COS           = getLC(LC.ldap_dit_naming_rdn_attr_cos,          DEFAULT_NAMING_RDN_ATTR_COS);
        NAMING_RDN_ATTR_DYNAMICGROUP  = getLC(LC.ldap_dit_naming_rdn_attr_dynamicgroup, DEFAULT_NAMING_RDN_ATTR_DYNAMICGROUP);
        NAMING_RDN_ATTR_GLOBALCONFIG  = getLC(LC.ldap_dit_naming_rdn_attr_globalconfig, DEFAULT_NAMING_RDN_ATTR_GLOBALCONFIG);
        NAMING_RDN_ATTR_GLOBALGRANT   = getLC(LC.ldap_dit_naming_rdn_attr_globalgrant,  DEFAULT_NAMING_RDN_ATTR_GLOBALGRANT);
        NAMING_RDN_ATTR_MIME          = getLC(LC.ldap_dit_naming_rdn_attr_mime,         DEFAULT_NAMING_RDN_ATTR_MIME);
        NAMING_RDN_ATTR_SERVER        = getLC(LC.ldap_dit_naming_rdn_attr_server,       DEFAULT_NAMING_RDN_ATTR_SERVER);
        NAMING_RDN_ATTR_ALWAYSONCLUSTER= getLC(LC.ldap_dit_base_dn_alwaysoncluster,     DEFAULT_NAMING_RDN_ATTR_ALWAYSONCLUSTER);
        NAMING_RDN_ATTR_UCSERVICE     = getLC(LC.ldap_dit_naming_rdn_attr_ucservice,    DEFAULT_NAMING_RDN_ATTR_UCSERVICE);
        NAMING_RDN_ATTR_SHARE_LOCATOR = getLC(LC.ldap_dit_naming_rdn_attr_share_locator,DEFAULT_NAMING_RDN_ATTR_SHARE_LOCATOR);
        NAMING_RDN_ATTR_XMPPCOMPONENT = getLC(LC.ldap_dit_naming_rdn_attr_xmppcomponent,DEFAULT_NAMING_RDN_ATTR_XMPPCOMPONENT);
        NAMING_RDN_ATTR_ZIMLET        = getLC(LC.ldap_dit_naming_rdn_attr_zimlet,       DEFAULT_NAMING_RDN_ATTR_ZIMLET);

        DN_GLOBALCONFIG    = NAMING_RDN_ATTR_GLOBALCONFIG + "=config" + "," + BASE_DN_CONFIG_BRANCH;
        DN_GLOBALGRANT     = NAMING_RDN_ATTR_GLOBALGRANT  + "=globalgrant" + "," + BASE_DN_CONFIG_BRANCH;

        BASE_DN_ADMIN         = getLCAndValidateUnderConfigBranchDN(LC.ldap_dit_base_dn_admin,         DEFAULT_BASE_RDN_ADMIN         + "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_APPADMIN      = getLCAndValidateUnderConfigBranchDN(LC.ldap_dit_base_dn_appadmin,      DEFAULT_BASE_RDN_APPADMIN      + "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_COS           = getLCAndValidateUnderConfigBranchDN(LC.ldap_dit_base_dn_cos,           DEFAULT_BASE_RDN_COS           + "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_GLOBAL_DYNAMICGROUP
                              = getLCAndValidateUnderConfigBranchDN(LC.ldap_dit_base_dn_global_dynamicgroup, DEFAULT_BASE_RDN_GLOBAL_DYNAMICGROUP + "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_MIME          = getLCAndValidateUnderConfigBranchDN(LC.ldap_dit_base_dn_mime,          DEFAULT_BASE_RDN_MIME          + "," + DN_GLOBALCONFIG);
        BASE_DN_SERVER        = getLCAndValidateUnderConfigBranchDN(LC.ldap_dit_base_dn_server,        DEFAULT_BASE_RDN_SERVER        + "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_ALWAYSONCLUSTER = getLCAndValidateUnderConfigBranchDN(LC.ldap_dit_base_dn_alwaysoncluster, DEFAULT_BASE_RDN_ALWAYSONCLUSTER+ "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_UCSERVICE     = getLCAndValidateUnderConfigBranchDN(LC.ldap_dit_base_dn_ucservice,     DEFAULT_BASE_RDN_UCSERVICE     + "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_SHARE_LOCATOR = getLCAndValidateUnderConfigBranchDN(LC.ldap_dit_base_dn_share_locator, DEFAULT_BASE_RDN_SHARE_LOCATOR + "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_XMPPCOMPONENT = getLCAndValidateUnderConfigBranchDN(LC.ldap_dit_base_dn_xmppcomponent, DEFAULT_BASE_RDN_XMPPCOMPONENT + "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_ZIMLET        = getLCAndValidateUnderConfigBranchDN(LC.ldap_dit_base_dn_zimlet,        DEFAULT_BASE_RDN_ZIMLET        + "," + BASE_DN_CONFIG_BRANCH);

        BASE_DN_DOMAIN        = getLCAndValidateUnderConfigBranchDN(LC.ldap_dit_base_dn_domain, DEFAULT_BASE_RDN_DOMAIN + "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_ZIMBRA        = computeZimbraBaseDN();
    }

    /*
     * take the common base for config branch and mail branch
     */
    private String computeZimbraBaseDN() {
        String[] rdns1 = BASE_DN_CONFIG_BRANCH.split(",");
        String[] rdns2 = BASE_DN_MAIL_BRANCH.split(",");

        int idx1 = rdns1.length - 1;
        int idx2 = rdns2.length - 1;

        int shorter = Math.min(rdns1.length,  rdns2.length);

        String commonDn = "";
        for (int i=0; i<shorter; i++, idx1--, idx2--) {
            if (rdns1[idx1].equalsIgnoreCase(rdns2[idx2])) {
                if (commonDn.isEmpty()) {
                    commonDn = rdns1[idx1];
                } else {
                    commonDn = rdns1[idx1] + "," + commonDn;
                }
            } else {
                break;
            }
        }
        return commonDn;
    }

    private ServiceException UNSUPPORTED(String msg) {
        return ServiceException.FAILURE(msg + " unsupported in " + getClass().getCanonicalName(), null);
    }

    /*
     * verify that dn is under parentDn
     */
    private boolean validateUnderDN(String parentDn, String dn) {
        return isUnder(parentDn, dn);
    }

    private void validateMailBranchEntryDN(String dn) throws ServiceException {
        if (!validateUnderDN(BASE_DN_MAIL_BRANCH, dn))
            throw ServiceException.INVALID_REQUEST("dn " + dn + " must be under " + BASE_DN_MAIL_BRANCH, null);
    }

    private String defaultDomain() throws ServiceException {
        String defaultDomain = mProv.getConfig().getAttr(Provisioning.A_zimbraDefaultDomainName, null);
        if (StringUtil.isNullOrEmpty(defaultDomain))
           throw UNSUPPORTED("default domain is empty");

        return defaultDomain;
    }

    private String acctAndDLDNCreate(String baseDn, IAttributes attrs) throws ServiceException {
        String rdnAttr = NAMING_RDN_ATTR_USER;
        String rdnValue = attrs.getAttrString(rdnAttr);

        if (rdnValue == null)
            throw ServiceException.FAILURE("missing rdn attribute" + rdnAttr, null);

        validateMailBranchEntryDN(baseDn);

        return rdnAttr + "=" + LdapUtil.escapeRDNValue(rdnValue) + "," + baseDn;
    }

    /*
     * ===========
     *   account
     * ===========
     */
    @Override
    public String emailToDN(String localPart, String domain) throws ServiceException{
        throw UNSUPPORTED("function emailToDN");
    }

    @Override
    public String emailToDN(String email) throws ServiceException {
        throw UNSUPPORTED("function emailToDN");
    }

    @Override
    public String accountDNCreate(String baseDn, IAttributes attrs, String localPart, String domain) throws ServiceException {
        if (baseDn == null)
            throw ServiceException.INVALID_REQUEST("base dn is required in DIT impl " + getClass().getCanonicalName(), null);

        return acctAndDLDNCreate(baseDn, attrs);
    }

    @Override
    public String accountDNRename(String oldDn, String newLocalPart, String newDomain) throws ServiceException {
        return oldDn;
    }

    /*
     * Get email local part attr from attrs and form the email with the default domain
     */
    @Override
    public String dnToEmail(String dn, IAttributes attrs) throws ServiceException {
        String localPart = attrs.getAttrString(DEFAULT_NAMING_RDN_ATTR_USER);

        if (localPart != null)
            return localPart + "@" + defaultDomain();
        else
            throw ServiceException.FAILURE("unable to map dn [" + dn + "] to email", null);
    }

    /*
     * ==========
     *   alias
     * ==========
     */
    /*
     *  - Aliases has to be in the same domain as the target entry, and the domain has to be the default
     *    domain(config.zimbraDefaultDomainName).  For example, if the entry is foo@domain1.com,
     *    it *cannot* have foo-alias@domain2.com.  And, in order for foo@domain1.com to have alias, domain1.com
     *    has to be the default domain.
     *
     *  - This is because alias entries do not carry the domain information.
     *    In our default DIT, aliases are created under the DN which is computed from the domain.
     *    Therefore, given a dn, we can compute it back to the domain of the alias.
     *    But in the custom DIT, aliases are always created under the same DN of the target entry,
     *    because in the custom DIT there is no co-relation between the email address and the DN.
     *    As a workaround so we can at least have basic alias functionalities, the dnToEmail function
     *    always uses zimbraDefaultDomainName for the domain, which is of course still wrong and will break
     *    when zimbraDefaultDomainName is changed!  Until we have a real solution for that, for now aliases
     *    in the custom DIT is flakey and this is the best we can do.  Maybe we should just disallow
     *    aliases in the custom DIT?
     */
    @Override
    public String aliasDN(String targetDn, String targetDomain, String aliasLocalPart, String aliasDomain) throws ServiceException {
        if (targetDn == null || targetDomain == null)
            throw UNSUPPORTED("alias DN without target dn or target domain");

        String allowedDomain = defaultDomain();
        if (!aliasDomain.equals(allowedDomain))
            throw UNSUPPORTED("alias DN not in default domain" + "(alias domain=" + aliasDomain + ", default domain=" + allowedDomain + ")");

        if (!targetDomain.equals(aliasDomain))
            throw UNSUPPORTED("alias DN with different target domain and alias domain" + "(alias domain=" + aliasDomain + ", target domain=" + targetDomain + ")");

        /*
         * alias is placed under the same dn as the target
         */
        String[] parts = LdapUtil.dnToRdnAndBaseDn(targetDn);
        return NAMING_RDN_ATTR_USER + "=" + LdapUtil.escapeRDNValue(aliasLocalPart) + "," + parts[1];
    }

    @Override
    public String aliasDNRename(String targetNewDn, String targetNewDomain, String newAliasEmail) throws ServiceException {
        if (targetNewDn == null || targetNewDomain == null)
            throw UNSUPPORTED("alias DN rename without target dn or target domain");

        String allowedDomain = defaultDomain();
        if (!targetNewDomain.equals(allowedDomain))
            throw UNSUPPORTED("alias DN rename not in default domain" + "(alias domain=" + targetNewDomain + ", default domain=" + allowedDomain + ")");

        return targetNewDn;
    }

    /* =================
     * calendar resource
     * =================
     */


    /*
     * =====================
     *   distribution list
     * =====================
     */
    /*
     * same restrictions on domain as the restrictions for aliases.
     */
    @Override
    public String distributionListDNCreate(String baseDn, IAttributes attrs, String localPart, String domain) throws ServiceException {
        if (baseDn == null)
            throw ServiceException.INVALID_REQUEST("base dn is required in DIT impl " + getClass().getCanonicalName(), null);

        String allowedDomain = defaultDomain();
        if (!domain.equals(allowedDomain))
            throw UNSUPPORTED("DL DN not in default domain" + "(DL domain=" + domain + ", default domain=" + allowedDomain + ")");

        return acctAndDLDNCreate(baseDn, attrs);
    }

    @Override
    public String distributionListDNRename(String oldDn, String newLocalPart, String newDomain) throws ServiceException {
        return oldDn;
    }


    /*
     * ==========
     *   domain
     * ==========
     */
    @Override
    public String domainBaseDN() {
        return BASE_DN_DOMAIN;
    }

    @Override
    public String[] domainToDNs(String[] parts) {
        return domainToDNsInternal(parts, BASE_DN_DOMAIN);
    }

    /*
     * account base search dn
     *
     * In custom DIT, accounts are not located under domain's DN.
     * Accounts can be created anywhere in the DIT.  We use the mail branch base if
     * it is configured, the default mail branch base is "".
     */
    @Override
    public String domainToAccountSearchDN(String domain) throws ServiceException {
        return BASE_DN_MAIL_BRANCH;
    }

    // account base search dn
    @Override
    public String domainDNToAccountSearchDN(String domainDN) throws ServiceException {
        return BASE_DN_MAIL_BRANCH;
    }


    /*
     * ==========
     *   filters
     * ==========
     */
    @Override
    public ZLdapFilter filterAccountsByDomainAndServer(Domain domain, Server server) {
        return ZLdapFilterFactory.getInstance().velodromeAllAccountsByDomainAndServer(
                domain.getName(), server.getServiceHostname());
    }

    @Override
    public ZLdapFilter filterAccountsOnlyByDomainAndServer(Domain domain, Server server) {
        return ZLdapFilterFactory.getInstance().velodromeAllAccountsOnlyByDomainAndServer(
                domain.getName(), server.getServiceHostname());
    }

    @Override
    public ZLdapFilter filterCalendarResourceByDomainAndServer(Domain domain, Server server) {
        return ZLdapFilterFactory.getInstance().velodromeAllCalendarResourcesByDomainAndServer(
                domain.getName(), server.getServiceHostname());
    }


    @Override
    public ZLdapFilter filterAccountsOnlyByDomain(Domain domain) {
        return ZLdapFilterFactory.getInstance().velodromeAllAccountsOnlyByDomain(domain.getName());
    }


    @Override
    public ZLdapFilter filterCalendarResourcesByDomain(Domain domain) {
        return ZLdapFilterFactory.getInstance().velodromeAllCalendarResourcesByDomain(domain.getName());
    }

    /**
     * DistributionLists in customDIT do not have the "zimbraMailDeliveryAddress" attribute.
     * From an earlier comment (when this method threw {@link UnsupportedOperationException})
     * we can't tell whether the main email is "zimbraMailAlias" or "mail".
     * This filter will accept the domain in either "zimbraMailAlias" or "mail".
     */
    @Override
    public ZLdapFilter filterDistributionListsByDomain(Domain domain) {
        return ZLdapFilterFactory.getInstance().velodromeAllDistributionListsByDomain(domain.getName());
    }

    /**
     * DistributionLists in customDIT do not have the "zimbraMailDeliveryAddress" attribute.
     * From an earlier comment (when this method threw {@link UnsupportedOperationException})
     * we can't tell whether the main email is "zimbraMailAlias" or "mail".
     * This filter will accept the domain in either "zimbraMailAlias" or "mail".
     */
    @Override
    public ZLdapFilter filterGroupsByDomain(Domain domain) {
        return ZLdapFilterFactory.getInstance().velodromeAllGroupsByDomain(domain.getName());
    }



    /*
     * ========================================================================================
     */
    @Override
    public SpecialAttrs handleSpecialAttrs(Map<String, Object> attrs) throws ServiceException {

        // check for required attrs
        if (SpecialAttrs.getSingleValuedAttr(attrs, SpecialAttrs.PA_ldapBase) == null)
            throw ServiceException.INVALID_REQUEST("missing required attribute " + SpecialAttrs.PA_ldapBase, null);

        if (!NAMING_RDN_ATTR_USER.equals(DEFAULT_NAMING_RDN_ATTR_USER)) {
            if (SpecialAttrs.getSingleValuedAttr(attrs, NAMING_RDN_ATTR_USER) == null)
                throw ServiceException.INVALID_REQUEST("missing required attribute " + NAMING_RDN_ATTR_USER, null);
        }

        SpecialAttrs specialAttrs = new SpecialAttrs();
        if (attrs != null) {
            specialAttrs.handleZimbraId(attrs);
            specialAttrs.handleLdapBaseDn(attrs);
        }


        return specialAttrs;
    }
}
