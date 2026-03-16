package com.zimbra.cs.service.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Maps;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.mail.ServiceTestUtil;

/**
 * Base class for com.zimbra.cs.service.admin handler tests.
 *
 * <h3>Two usage tiers</h3>
 * <ol>
 *   <li><b>Full server</b> (default) – subclass as-is, call {@link #createAdminAccount} /
 *       {@link #createRegularAccount} in a {@code @BeforeClass} of your own. Suitable for
 *       tests that need a real {@link com.zimbra.cs.mailbox.Mailbox} or full SOAP pipeline.</li>
 *   <li><b>Lightweight</b> – override {@code init()} annotated with {@code @BeforeClass} and
 *       call {@code MailboxTestUtil.initProvisioning()} instead of {@code initServer()}.
 *       Suitable for pure handler-logic tests that do not touch the DB.</li>
 * </ol>
 *
 * <h3>Mockito</h3>
 * Mockito 1.10.19 is already on the test classpath (see store/ivy.xml).
 * Fields annotated with {@code @Mock} in a subclass are initialised automatically
 * because {@link #initMocks()} calls {@code MockitoAnnotations.initMocks(this)}.
 * For static/singleton overrides, use PowerMock (also on classpath) or the
 * inner-class override pattern shown in {@code ContactBackupApiTest}.
 *
 * <h3>Provisioning</h3>
 * {@code MailboxTestUtil.initServer()} installs {@link MockProvisioning} as the
 * singleton — no LDAP or network required. Use {@link #prov} directly; it is a
 * fully in-memory implementation that supports createAccount / createDomain.
 */
public abstract class BaseAdminTest {

    // -----------------------------------------------------------------------
    // Shared fixtures — populated by initServer(); subclasses may also add
    // their own static fields for test-specific accounts/domains.
    // -----------------------------------------------------------------------

    /** In-memory MockProvisioning instance, set by initServer(). */
    protected static Provisioning prov;

    /** Reusable admin account available to all subclass tests. */
    protected static Account adminAccount;

    /** Default domain created alongside the admin account. */
    protected static Domain defaultDomain;

    protected static final String DEFAULT_DOMAIN = "test.zimbra.com";
    protected static final String DEFAULT_ADMIN  = "admin@" + DEFAULT_DOMAIN;
    protected static final String DEFAULT_PASSWD  = "test123";

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Boots the in-memory server stack: MockProvisioning, HSQLDB, IndexStore,
     * MockStoreManager. ZimbraLog is initialised here — no NDC errors.
     * Subclasses should call {@code super.init()} or keep the {@code @BeforeClass}
     * on this method by not overriding it.
     */
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();

        defaultDomain   = createDomain(DEFAULT_DOMAIN);
        adminAccount    = createAdminAccount(DEFAULT_ADMIN);
        // Materialise a mailbox so getMailboxByAccount() succeeds in tests.
        MailboxManager.getInstance().getMailboxByAccount(adminAccount);
    }

    /**
     * Purges all in-memory data: HSQLDB tables, mailbox cache, index, store.
     * Always runs even when tests fail.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    /**
     * Initialises {@code @Mock}-annotated fields in the concrete subclass.
     * Call this from a {@code @Before} method in the subclass when you use Mockito mocks.
     *
     * <pre>
     * {@literal @}Before
     * public void setUp() { initMocks(); }
     * </pre>
     */
    protected void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    // -----------------------------------------------------------------------
    // Account / domain fixture helpers
    // -----------------------------------------------------------------------

    /**
     * Creates an account with {@code zimbraIsAdminAccount=TRUE} and a stable UUID.
     * The UUID is deterministic per email to make test output reproducible.
     */
    protected static Account createAdminAccount(String email) throws Exception {
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.nameUUIDFromBytes(email.getBytes()).toString());
        attrs.put(Provisioning.A_zimbraIsAdminAccount, Boolean.TRUE.toString());
        prov.createAccount(email, DEFAULT_PASSWD, attrs);
        return prov.getAccountByName(email);
    }

    /**
     * Creates a regular (non-admin) account with a stable UUID.
     */
    protected static Account createRegularAccount(String email) throws Exception {
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.nameUUIDFromBytes(email.getBytes()).toString());
        prov.createAccount(email, DEFAULT_PASSWD, attrs);
        return prov.getAccountByName(email);
    }

    /**
     * Creates a domain if it does not already exist.
     */
    protected static Domain createDomain(String domainName) throws Exception {
        Domain d = prov.getDomainByName(domainName);
        if (d != null) {
            return d;
        }
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraId, UUID.nameUUIDFromBytes(domainName.getBytes()).toString());
        return prov.createDomain(domainName, attrs);
    }

    // -----------------------------------------------------------------------
    // Request context helpers
    // -----------------------------------------------------------------------

    /**
     * Builds the {@code Map<String,Object>} context expected by
     * {@link AdminDocumentHandler#handle}, where the authenticated user and the
     * target account are the same.
     */
    protected static Map<String, Object> getRequestContext(Account account) throws Exception {
        return ServiceTestUtil.getRequestContext(account);
    }

    /**
     * Builds the context where an admin acts on behalf of a target account.
     */
    protected static Map<String, Object> getRequestContext(Account authAccount,
            Account targetAccount) throws Exception {
        return ServiceTestUtil.getRequestContext(authAccount, targetAccount);
    }

    /**
     * Convenience: build context for the shared {@link #adminAccount}.
     */
    protected static Map<String, Object> adminContext() throws Exception {
        return getRequestContext(adminAccount);
    }

    // -----------------------------------------------------------------------
    // Handler execution helpers
    // -----------------------------------------------------------------------

    /**
     * Executes a handler using the shared admin context.
     *
     * @throws com.zimbra.common.service.ServiceException propagated as-is so
     *         tests can assert on {@code se.getCode()}.
     */
    protected Element execute(AdminDocumentHandler handler, Element request)
            throws Exception {
        return handler.handle(request, adminContext());
    }

    /**
     * Executes a handler with an explicit caller account (e.g. a non-admin).
     */
    protected Element execute(AdminDocumentHandler handler, Element request,
            Account caller) throws Exception {
        return handler.handle(request, getRequestContext(caller));
    }
}
