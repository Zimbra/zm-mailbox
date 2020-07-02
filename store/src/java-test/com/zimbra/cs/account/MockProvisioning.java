/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2019 Synacor, Inc.
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
package com.zimbra.cs.account;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.AlwaysOnClusterBy;
import com.zimbra.common.account.Key.ShareLocatorBy;
import com.zimbra.common.account.Key.UCServiceBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.NamedEntry.Visitor;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.auth.AuthContext.Protocol;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.mime.MockMimeTypeInfo;
import com.zimbra.cs.mime.handler.MessageRFC822Handler;
import com.zimbra.cs.mime.handler.TextCalendarHandler;
import com.zimbra.cs.mime.handler.TextHtmlHandler;
import com.zimbra.cs.mime.handler.TextPlainHandler;
import com.zimbra.cs.mime.handler.UnknownTypeHandler;
import com.zimbra.cs.redolog.MockRedoLogProvider;
import com.zimbra.soap.admin.type.CacheEntryType;
import com.zimbra.soap.admin.type.DataSourceType;

/**
 * Mock implementation of {@link Provisioning} for testing.
 *
 * @author ysasaki
 */
public final class MockProvisioning extends Provisioning {
    public static final String DEFAULT_ACCOUNT_ID = new UUID(0L, 0L).toString();

    private final Map<String, Account> id2account = Maps.newHashMap();
    private final Map<String, Account> name2account = Maps.newHashMap();

    private static final String DATA_SOURCE_LIST_CACHE_KEY = "MockProvisioning.DATA_SOURCE_CACHE";
    private final Map<String, Domain> id2domain = Maps.newHashMap();

    private final Map<String, Cos> id2cos = Maps.newHashMap();

    private final Map<String, List<MimeTypeInfo>> mimeConfig = Maps.newHashMap();
    private final Config config = new Config(new HashMap<String, Object>(), this);
    private final Map<String, ShareLocator> shareLocators = Maps.newHashMap();

    private final Server localhost;
    private final Map<String, Server> servers = Maps.newHashMap();

    public MockProvisioning() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(A_zimbraServiceHostname, "localhost");
        attrs.put(A_zimbraRedoLogProvider, MockRedoLogProvider.class.getName());
        attrs.put(A_zimbraId, UUID.randomUUID().toString());
        attrs.put(A_zimbraMailMode, MailMode.http.toString());
        attrs.put(A_zimbraSmtpPort, "7025");
        attrs.put(A_zimbraLowestSupportedAuthVersion, "1");
        attrs.put(A_zimbraSSLPrivateKey, "-----BEGIN PRIVATE KEY-----MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDj5CdJz5QNpk7skWqv7lC7gaymGiqbpEXAe8za1JTgjKkL0gzQNM79aGr+lnYKXU+rto15QT9uDyvVnY/iPlpeLtCEin9OIEccLsPivG0R8gL8O2HGF86ns+ZNbjjJVetr/qk1mFN/91qMfOhL/F6tBR5zSIVdCE4bdqCqpq4HywEaKyXxCwU4bKlmawrLRZeITiMQ6Je/VsFvz2wj7yrlCH5HKtoyaNuLR9KH1HHbFB/p9JCK9/qZpq5p4vNXr0fGs3PFQAfhau2ySmo1bEhYDIs3/nBLrXP3OHQqfEPEE1R7BrcEBBtUaY8t5JSduOQRr63qDyGjXnu+xcwA46v/AgMBAAECggEAUa57LoeKZ4IOk9hjRv/CTBLkkPyb/QFaRu2YtW6wlfOUu7nkAdSLxGRixTGkyX48ii16c9WhKI+jhINfCRaUSWG6N2d0zcnf8wgICgLDjUUTMNkP6HKsDYv7phE1pWR4Z1L1z1Hzy9Aa0nQKxwGD5bwJ+AQsWPYbGNjiKYhopD2/zbTnhhYCA9/aSf2vKUa9ITWpW7nbYK1dtOW+eX+CrBRBO54KQ0OlJBnzk2oZv7IOlp6PUck0HMurP/N/EV1lVwvsMaddy3osHrB1qjA/vSX+wYMaNRSyN8p9hRTAAPQKJKy3feXUrE/kzhV/MP0DYbHkV1SYDNt0nDarZ8wDAQKBgQD2r0UIdMmOwjGoR0y4/EXSvEf6+KP+t0GDEYfl9C5/Ei3BTFPU5bWg/1TEQJ5R4AsI6TchHE1k0o1tENZ09SO/9ESQPfRZHbbaMit64SAg3e8mvChEvVfaunDapOTuwiqzvTjc12plwXrzrkt2xtXLpWvZ8afWVaBhEZysieXvQQKBgQDsfzSc4AuUprqqLU7pyQGSy9zjv2Pt25aUdiyiVYawzDfcQly4x5b/b/NodQ2SG/5zN2/L+fvjHJvWcKsHx984siOGVMxEK5uUz00eKbVB/PZP+oT7MALStaI0U0gnwtnWgXCxMKl2QOfWnF3AMkF99xKywsuyFIg6ojoE9IYLPwKBgQDzxmi13pOAXC+uWCdddw+ZHS8UuLl3calv2NcvS4rXUCOfLcp6TTacDza5ahIKXxkIiU9NjSZ+SAQyj70ef1IA02ceE9twZYjZP1Lwb6DMWgWHhdFVfLdhE3WK3ADQYVjJnmie9NHUFMtoHAm/KucEBEj8a26sxJlk0368ktmDAQKBgBGMctv9J/7UzF8aU5O3bZ118SMZLZIVzDuh9Tfqfr8ZuD9o0TaI4OR9ayNiJCqmVyA3id0p5I36rnmgDKDcLO0pEsfB/RJF5hqJs2A8mg2WdrSCk2GMM3ltLucREvaYV8+59SHAyaJTuKBNJAvB7ugo8ENBfxnsuhsXtJRvjI7DAoGAG7HrReM3cX8k8jqopY8xlT03Q372v07PL8fs2aOP+zsA580IdDw0Xvea+dmykCzpj3DmBci+TOE7DYb1SW6+NPN4FOP6o6TFogrgNa/0LAWXPuct+1e5vy9F1/jFpvNbbD8uTJKdJWGKO78wLVybn9gTJ95ZZcBfytQMyXpqysk=-----END PRIVATE KEY-----");
        localhost = new Server("localhost", "localhost", attrs, Collections.<String, Object>emptyMap(), this);
        servers.put("localhost", localhost);
        try {
            config.setDefaultDomainName("testdomain.biz");
			config.setDefaultAnalyzerStopWords(new String[] { "a", "an", "and",
					"are", "as", "at", "be", "but", "by", "for", "if", "in",
					"into", "is", "it", "no", "not", "of", "on", "or", "such",
					"that", "the", "their", "then", "there", "these", "they",
					"this", "to", "was", "will", "with" });
        } catch (ServiceException e) {
            ZimbraLog.test.warn("Could not set default domain name?", e);
        }
        initializeMimeHandlers();
    }

    @Override
    public Account createAccount(String email, String password, Map<String, Object> attrs) throws ServiceException {
        validate(ProvisioningValidator.CREATE_ACCOUNT, email, null, attrs);
        if (!attrs.containsKey(A_zimbraId)) {
            attrs.put(A_zimbraId, DEFAULT_ACCOUNT_ID);
        }
        if (!attrs.containsKey(A_zimbraMailHost)) {
            attrs.put(A_zimbraMailHost, "localhost");
        }
        if (!attrs.containsKey(A_zimbraAccountStatus)) {
            attrs.put(A_zimbraAccountStatus, ACCOUNT_STATUS_ACTIVE);
        }
        if (!attrs.containsKey(A_zimbraDumpsterEnabled)) {
            attrs.put(A_zimbraDumpsterEnabled, ProvisioningConstants.TRUE);
        }
        attrs.put(A_zimbraBatchedIndexingSize, Integer.MAX_VALUE); // suppress indexing
        Account account = new Account(email, email, attrs, null, this);
        try {
            name2account.put(email, account);
            id2account.put(account.getId(), account);
            return account;
        } finally {
            validate(ProvisioningValidator.CREATE_ACCOUNT_SUCCEEDED, email, account);
        }
    }

    @Override
    public Account get(AccountBy keyType, String key) throws ServiceException {
        switch (keyType) {
            case name:
                if (name2account.get(key) != null) {
                    return name2account.get(key);
                } else {
                    for (Account acct : name2account.values()) {
                        if (Arrays.asList(acct.getAliases()).contains(key)) {
                            return acct;
                        }
                    }
                }
            case id:
            default:
                return id2account.get(key);
        }
    }

    @Override
    public List<MimeTypeInfo> getMimeTypes(String mime) {
        List<MimeTypeInfo> result = mimeConfig.get(mime);
        if (result != null) {
            return result;
        } else {
            MockMimeTypeInfo info = new MockMimeTypeInfo();
            info.setHandlerClass(UnknownTypeHandler.class.getName());
            return Collections.<MimeTypeInfo>singletonList(info);
        }
    }

    @Override
    public List<MimeTypeInfo> getAllMimeTypes() {
        List<MimeTypeInfo> result = new ArrayList<MimeTypeInfo>();
        for (List<MimeTypeInfo> entry : mimeConfig.values()) {
            result.addAll(entry);
        }
        return result;
    }

    public void addMimeType(String mime, MimeTypeInfo info) {
        List<MimeTypeInfo> list = mimeConfig.get(mime);
        if (list == null) {
            list = new ArrayList<MimeTypeInfo>();
            mimeConfig.put(mime, list);
        }
        list.add(info);
    }

    private void initializeMimeHandlers() {
        MockMimeTypeInfo plain = new MockMimeTypeInfo();
        plain.setMimeTypes(MimeConstants.CT_TEXT_PLAIN);
        plain.setHandlerClass(TextPlainHandler.class.getName());
        plain.setIndexingEnabled(true);
        addMimeType(MimeConstants.CT_TEXT_PLAIN, plain);

        MockMimeTypeInfo html = new MockMimeTypeInfo();
        html.setMimeTypes(MimeConstants.CT_TEXT_HTML);
        html.setHandlerClass(TextHtmlHandler.class.getName());
        html.setFileExtensions("html", "htm");
        html.setIndexingEnabled(true);
        addMimeType(MimeConstants.CT_TEXT_HTML, html);

        MockMimeTypeInfo calendar = new MockMimeTypeInfo();
        calendar.setMimeTypes(MimeConstants.CT_TEXT_CALENDAR);
        calendar.setHandlerClass(TextCalendarHandler.class.getName());
        calendar.setIndexingEnabled(true);
        addMimeType(MimeConstants.CT_TEXT_CALENDAR, calendar);

        MockMimeTypeInfo message = new MockMimeTypeInfo();
        message.setMimeTypes(MimeConstants.CT_MESSAGE_RFC822);
        message.setHandlerClass(MessageRFC822Handler.class.getName());
        message.setIndexingEnabled(true);
        addMimeType(MimeConstants.CT_MESSAGE_RFC822, message);
    }

    public void clearMimeHandlers() {
        mimeConfig.clear();
    }

    @Override
    public Config getConfig() {
        return config;
    }

    private String[] listToStringArray(List<?> list) {
        String[] strArray = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            strArray[i] = list.get(i).toString();
        }
        return strArray;
    }

    @Override
    public void modifyAttrs(Entry entry, Map<String, ? extends Object> attrs, boolean checkImmutable) {
        Map<String, Object> map = entry.getAttrs(false, false);
        for (Map.Entry<String, ? extends Object> attr : attrs.entrySet()) {
            String key = attr.getKey();
            if (attr.getValue() != null) {
                Object value = attr.getValue();
                if (value instanceof List) { // Convert list to string array.
                    value = listToStringArray((List<?>) value);
                }
                boolean add = key.startsWith("+");
                boolean remove = key.startsWith("-");
                if (add || remove) {
                    String realKey = key.substring(1);
                    Object existing = map.get(key.substring(1));
                    if (existing == null) {
                       if (add) {
                           map.put(realKey, value);
                       } else {
                           return;
                       }
                    } else {
                        List<Object> list = null;
                        if (existing instanceof Object[]) {
                            list = new ArrayList<>(Arrays.asList(existing));
                        } else if (existing instanceof Object) {
                            list = new ArrayList<Object>();
                            list.add(existing);
                        }
                        if (add) {
                            list.add(value);
                        } else {
                            list.remove(value);
                        }
                        if (list.size() > 0) {
                            map.put(realKey, listToStringArray(list));
                        } else {
                            map.remove(realKey);
                        }
                    }
                } else {
                    map.put(attr.getKey(), value);
                }
            } else {
                map.remove(attr.getKey());
            }
        }
    }

    @Override
    public Server getLocalServer() {
        return localhost;
    }

    @Override
    public Server getLocalServerIfDefined() {
        return getLocalServer();
    }

    @Override
    public void modifyAttrs(Entry e, Map<String, ? extends Object> attrs,
            boolean checkImmutable, boolean allowCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reload(Entry e) {
    }

    @Override
    public boolean inDistributionList(Account acct, String zimbraId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getDistributionLists(Account acct) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getDirectDistributionLists(Account acct)
            throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DistributionList> getDistributionLists(Account acct,
            boolean directOnly, Map<String, String> via) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DistributionList> getDistributionLists(DistributionList list,
            boolean directOnly, Map<String, String> via) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean healthCheck() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GlobalGrant getGlobalGrant() {
        return null;
    }

    @Override
    public Account restoreAccount(String emailAddress, String password,
            Map<String, Object> attrs, Map<String, Object> origAttrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAccount(String zimbraId) {
        Account account = id2account.remove(zimbraId);
        if (account != null) {
            name2account.remove(account.getName());
        }
    }

    @Override
    public void renameAccount(String zimbraId, String newName) {
        //do nothing
    }

    @Override
    public List<Account> getAllAdminAccounts() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCOS(Account acct, Cos cos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifyAccountStatus(Account acct, String newStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void authAccount(Account acct, String password, Protocol proto) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void authAccount(Account acct, String password, Protocol proto, Map<String, Object> authCtxt) throws ServiceException {
        String accountNamePassedIn = (String) authCtxt.get(AuthContext.AC_ACCOUNT_NAME_PASSEDIN);
        if (!LC.alias_login_enabled.booleanValue() &&
                Arrays.asList(acct.getAliases()).contains(authCtxt.get(AuthContext.AC_ACCOUNT_NAME_PASSEDIN))) {
            throw AuthFailedServiceException.AUTH_FAILED(accountNamePassedIn, accountNamePassedIn, "alias login not enabled.");
        }
    }

    @Override
    public void preAuthAccount(Account acct, String accountName, String accountBy, long timestamp, long expires,
            String preAuth, Map<String, Object> authCtxt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void ssoAuthAccount(Account acct, AuthContext.Protocol proto, Map<String, Object> authCtxt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changePassword(Account acct, String currentPassword, String newPassword) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SetPasswordResult setPassword(Account acct, String newPassword) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkPasswordStrength(Account acct, String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAlias(Account acct, String alias) {
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMailAlias, alias);
        modifyAttrs(acct, attrs, false);
    }

    @Override
    public void removeAlias(Account acct, String alias) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Domain createDomain(String name, Map<String, Object> attrs) throws ServiceException {
        name = name.trim().toLowerCase();
        if (get(Key.DomainBy.name, name) != null) {
            throw AccountServiceException.DOMAIN_EXISTS(name);
        }

        String id = (String) attrs.get(A_zimbraId);
        if (id == null) {
            attrs.put(A_zimbraId, id = UUID.randomUUID().toString());
        }
        if (!attrs.containsKey(A_zimbraSmtpHostname)) {
            attrs.put(A_zimbraSmtpHostname, "localhost");
        }

        Domain domain = new Domain(name, id, attrs, null, this);
        id2domain.put(id, domain);
        return domain;
    }

    @Override
    public Domain get(Key.DomainBy keyType, String key) {
        switch (keyType) {
            case id:
                return id2domain.get(key);

            case name:
                for (Domain domain : id2domain.values()) {
                    if (domain.getName().equals(key)) {
                        return domain;
                    }
                }
                break;
        }

        return null;
    }

    @Override
    public List<Domain> getAllDomains() {
        return new ArrayList<Domain>(id2domain.values());
    }

    @Override
    public void deleteDomain(String zimbraId) {
        id2domain.remove(zimbraId);
    }

    @Override
    public void deleteDomainAfterRename(String zimbraId) {
        id2domain.remove(zimbraId);
    }

    @Override
    public Cos createCos(String name, Map<String, Object> attrs) throws ServiceException {
        name = name.trim().toLowerCase();
        if (get(Key.CosBy.name, name) != null) {
            throw AccountServiceException.COS_EXISTS(name);
        }

        String id = (String) attrs.get(A_zimbraId);
        if (id == null) {
            attrs.put(A_zimbraId, id = UUID.randomUUID().toString());
        }

        Cos cos = new Cos(name, id, attrs, this);
        id2cos.put(id, cos);
        return cos;
    }

    @Override
    public Cos copyCos(String srcCosId, String destCosName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameCos(String zimbraId, String newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cos get(Key.CosBy keyType, String key) {
        switch (keyType) {
            case id:
                return id2cos.get(key);

            case name:
                for (Cos cos : id2cos.values()) {
                    if (cos.getName().equals(key)) {
                        return cos;
                    }
                }
                break;
        }

        return null;
    }

    @Override
    public List<Cos> getAllCos() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteCos(String zimbraId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Server createServer(String name, Map<String, Object> attrs) {
        Server server = servers.get(name);
        if(server == null) {
            server = new Server(name, name, attrs, Collections.<String, Object>emptyMap(), this);
            servers.put(name,  server);
        }
        return server;
    }

    @Override
    public Server get(Key.ServerBy keyName, String key) {
        switch (keyName) {
            case id:
                return localhost.getId().equals(key) ? localhost : servers.get(key);
            case name:
                return localhost.getName().equals(key) ? localhost : servers.get(key);
            default:
                return servers.get(key);
        }
    }

    @Override
    public List<Server> getAllServers() {
        return Lists.newArrayList(servers.values());
    }

    @Override
    public List<Server> getAllServers(String service) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteServer(String zimbraId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DistributionList createDistributionList(String listAddress, Map<String, Object> listAttrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DistributionList get(Key.DistributionListBy keyType, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteDistributionList(String zimbraId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAlias(DistributionList dl, String alias) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAlias(DistributionList dl, String alias) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameDistributionList(String zimbraId, String newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Zimlet getZimlet(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Zimlet> listAllZimlets() {
        return Collections.emptyList();
    }

    @Override
    public Zimlet createZimlet(String name, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteZimlet(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CalendarResource createCalendarResource(String emailAddress, String password, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteCalendarResource(String zimbraId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameCalendarResource(String zimbraId, String newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CalendarResource get(Key.CalendarResourceBy keyType, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<?> getAllAccounts(Domain d) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getAllAccounts(Domain d, Visitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getAllAccounts(Domain d, Server s, Visitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<?> getAllCalendarResources(Domain d) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getAllCalendarResources(Domain d, Visitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getAllCalendarResources(Domain d, Server s, Visitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<?> getAllDistributionLists(Domain d) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMembers(DistributionList list, String[] members) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeMembers(DistributionList list, String[] member) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Identity getDefaultIdentity(Account account) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(A_zimbraPrefIdentityName, ProvisioningConstants.DEFAULT_IDENTITY_NAME);
        attrs.put(A_zimbraPrefIdentityId, account.getId());
        return new Identity(account, ProvisioningConstants.DEFAULT_IDENTITY_NAME, account.getId(), attrs, this);
    }

    @Override
    public Identity createIdentity(Account account, String identityName, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Identity restoreIdentity(Account account, String identityName, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifyIdentity(Account account, String identityName, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteIdentity(Account account, String identityName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Identity> getAllIdentities(Account account) {
        List<Identity> result = new ArrayList<Identity>();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(A_zimbraPrefIdentityName, ProvisioningConstants.DEFAULT_IDENTITY_NAME);
        attrs.put(A_zimbraPrefIdentityId, account.getId());
        result.add(new Identity(account, ProvisioningConstants.DEFAULT_IDENTITY_NAME, account.getId(), attrs, this));
        return result;
    }

    @Override
    public Identity get(Account account, Key.IdentityBy keyType, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Signature createSignature(Account account, String signatureName, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Signature restoreSignature(Account account, String signatureName, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifySignature(Account account, String signatureId, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSignature(Account account, String signatureId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Signature> getAllSignatures(Account account) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Signature get(Account account, Key.SignatureBy keyType, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataSource createDataSource(Account account, DataSourceType type, String dataSourceName, Map<String, Object> attrs) {
        attrs.put(A_zimbraDataSourceName, dataSourceName); // must be the same
        attrs.put(Provisioning.A_zimbraDataSourceType, type.toString());

        String dsId = new UUID(0L, 0L).toString();

        DataSource ds = new DataSource(account, type, dataSourceName, dsId, attrs, this);

        List<DataSource> dsList = getAllDataSources(account);
        dsList.add(ds);

        account.setCachedData(DATA_SOURCE_LIST_CACHE_KEY, dsList);

        return ds;
    }

    @Override
    public DataSource createDataSource(Account account, DataSourceType type, String dataSourceName, Map<String, Object> attrs,
            boolean passwdAlreadyEncrypted) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataSource restoreDataSource(Account account, DataSourceType type, String dataSourceName, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifyDataSource(Account account, String dataSourceId,
            Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteDataSource(Account account, String dataSourceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataSource> getAllDataSources(Account account) {
        // Don't throw UnsupportedOperationException because Mailbox.updateRssDataSource()
        // calls this method.
        @SuppressWarnings("unchecked")
        List<DataSource> result = (List<DataSource>) account.getCachedData(DATA_SOURCE_LIST_CACHE_KEY);

        if (result != null) {
            return result;
        }

        return new ArrayList<DataSource>();
    }

    @Override
    public DataSource get(Account account, Key.DataSourceBy keyType, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public XMPPComponent createXMPPComponent(String name, Domain domain, Server server, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public XMPPComponent get(Key.XMPPComponentBy keyName, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<XMPPComponent> getAllXMPPComponents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteXMPPComponent(XMPPComponent comp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flushCache(CacheEntryType type, CacheEntry[] entries) {
        //do nothing
    }

    @Override
    public ShareLocator get(ShareLocatorBy keyType, String key) throws ServiceException {
        return shareLocators.get(key);
    }

    @Override
    public ShareLocator createShareLocator(String id, Map<String, Object> attrs) throws ServiceException {
        ShareLocator shloc = new ShareLocator(id, attrs, this);
        shareLocators.put(id, shloc);
        return shloc;
    }

    @Override
    public void deleteShareLocator(String id) throws ServiceException {
        shareLocators.remove(id);
    }

    @Override
    public UCService createUCService(String name, Map<String, Object> attrs)
            throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteUCService(String zimbraId) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public UCService get(UCServiceBy keyName, String key) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<UCService> getAllUCServices() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameUCService(String zimbraId, String newName) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AlwaysOnCluster createAlwaysOnCluster(String name,
            Map<String, Object> attrs) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AlwaysOnCluster get(AlwaysOnClusterBy keyname, String key)
            throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAlwaysOnCluster(String zimbraId) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AlwaysOnCluster> getAllAlwaysOnClusters()
            throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Server> getAllServers(String service, String clusterId)
            throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refreshUserCredentials(Account account) {
        // Does nothing
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#createHabOrgUnit(com.zimbra.cs.account.Domain, java.lang.String)
     */
    @Override
    public Set<String> createHabOrgUnit(Domain domain, String habOrgUnitName)
        throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#renameHabOrgUnit(com.zimbra.cs.account.Domain, java.lang.String, java.lang.String)
     */
    @Override
    public Set<String> renameHabOrgUnit(Domain domain, String habOrgUnitName,
        String newHabOrgUnitName) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#deleteHabOrgUnit(com.zimbra.cs.account.Domain, java.lang.String)
     */
    @Override
    public void deleteHabOrgUnit(Domain domain, String habOrgUnitName) throws ServiceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteGroup(String zimbraId, boolean cascadeDelete) throws ServiceException {
        // do nothing
    }

    @Override
    public Set<String> listHabOrgUnit(Domain domain) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }
}
