package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Maps;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.filter.RuleManager.AdminFilterType;
import com.zimbra.cs.filter.RuleManager.FilterType;
import com.zimbra.cs.filter.SoapToSieve;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.admin.AdminDocumentHandler.AccountHarvestingCheckerBase;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.type.EditheaderTest;
import com.zimbra.soap.mail.type.FilterAction.AddheaderAction;
import com.zimbra.soap.mail.type.FilterAction.DeleteheaderAction;
import com.zimbra.soap.mail.type.FilterAction.ReplaceheaderAction;
import com.zimbra.soap.mail.type.FilterRule;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AccountHarvestingCheckerBase.class, ZimbraSoapContext.class})
public class ModifyFilterRulesAdminTest {
    private static final String ACCOUNTNAME = "test1_zcs273_"+System.currentTimeMillis()+"@zimbra.com";
    private static Provisioning prov = null;
    private Account account = null;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        prov.createAccount(ACCOUNTNAME, "secret", attrs);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        account = prov.getAccountByName(ACCOUNTNAME);
    }

    /******************addheader*********************/
    @Test
    public void testSoapToSieveAddheaderActionWithoutLast() throws ServiceException, Exception {
        AddheaderAction action = new AddheaderAction("X-New-Header", "Test vallue");
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script = "require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "addheader \"X-New-Header\" \"Test vallue\";\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }

    @Test
    public void testSoapToSieveAddheaderActionWithLast() throws ServiceException, Exception {
        AddheaderAction action = new AddheaderAction("X-New-Header", "Test vallue", true);
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script = "require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "addheader :last \"X-New-Header\" \"Test vallue\";\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }

    /******************deleteheader*********************/
    @Test
    public void testSoapToSieveDeleteheaderActionBasic() throws ServiceException, Exception {
        EditheaderTest test = new EditheaderTest(null, null, null, null, null, "X-Test-Header", null);
        DeleteheaderAction action = new DeleteheaderAction(null, null, test);
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script = "require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "deleteheader \"X-Test-Header\";\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }

    // headerName and headerValue only
    // default comparator and matchType
    @Test
    public void testSoapToSieveDeleteheaderAction2() throws ServiceException, Exception {
        List<String> values = new ArrayList<String>();
        values.add("Test value");
        EditheaderTest test = new EditheaderTest(null, null, null, null, null, "X-Test-Header", values);
        DeleteheaderAction action = new DeleteheaderAction(null, null, test);
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script = "require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "deleteheader :comparator \"i;ascii-casemap\" :is \"X-Test-Header\" \"Test value\";\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }

    // headerName and multiple headerValues
    // default comparator and matchType
    @Test
    public void testSoapToSieveDeleteheaderAction3() throws ServiceException, Exception {
        List<String> values = new ArrayList<String>();
        values.add("Value1");
        values.add("Value2");
        values.add("Value3");
        EditheaderTest test = new EditheaderTest(null, null, null, null, null, "X-Test-Header", values);
        DeleteheaderAction action = new DeleteheaderAction(null, null, test);
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script = "require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "deleteheader :comparator \"i;ascii-casemap\" :is \"X-Test-Header\" [ \"Value1\", \"Value2\", \"Value3\" ];\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }

    // headerName, multiple headerValues, matchType and comparator
    @Test
    public void testSoapToSieveDeleteheaderAction4() throws ServiceException, Exception {
        List<String> values = new ArrayList<String>();
        values.add("Value1");
        values.add("Value2");
        values.add("Value3");
        EditheaderTest test = new EditheaderTest("contains", null, null, null, "i;octet", "X-Test-Header", values);
        DeleteheaderAction action = new DeleteheaderAction(null, null, test);
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script = "require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "deleteheader :comparator \"i;octet\" :contains \"X-Test-Header\" [ \"Value1\", \"Value2\", \"Value3\" ];\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }

    // headerName, multiple headerValues, comparator, value, relationalComparator
    @Test
    public void testSoapToSieveDeleteheaderAction5() throws ServiceException, Exception {
        List<String> values = new ArrayList<String>();
        values.add("2");
        EditheaderTest test = new EditheaderTest(null, null, true, "ge", "i;ascii-numeric", "X-Test-Header", values);
        DeleteheaderAction action = new DeleteheaderAction(null, null, test);
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script = "require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "deleteheader :value \"ge\" :comparator \"i;ascii-numeric\" \"X-Test-Header\" \"2\";\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }

    // headerName, multiple headerValues, comparator, value, relationalComparator
    @Test
    public void testSoapToSieveDeleteheaderAction6() throws ServiceException, Exception {
        List<String> values = new ArrayList<String>();
        values.add("2");
        EditheaderTest test = new EditheaderTest(null, null, true, "ge", "i;ascii-numeric", "X-Test-Header", values);
        DeleteheaderAction action = new DeleteheaderAction(null, null, test);
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script = "require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "deleteheader :value \"ge\" :comparator \"i;ascii-numeric\" \"X-Test-Header\" \"2\";\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }

    // headerName, multiple headerValues, comparator, count, relationalComparator
    @Test
    public void testSoapToSieveDeleteheaderAction7() throws ServiceException, Exception {
        List<String> values = new ArrayList<String>();
        values.add("2");
        EditheaderTest test = new EditheaderTest(null, true, null, "ge", "i;ascii-numeric", "X-Test-Header", values);
        DeleteheaderAction action = new DeleteheaderAction(null, null, test);
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script = "require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "deleteheader :count \"ge\" :comparator \"i;ascii-numeric\" \"X-Test-Header\" \"2\";\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }

    /******************replaceheader*********************/
    @Test
    public void testSoapToSieveReplaceheaderActionBasic() throws ServiceException, Exception {
        EditheaderTest test = new EditheaderTest(null, null, null, null, null, "X-Test-Header", null);
        ReplaceheaderAction action = new ReplaceheaderAction(null, null, test, null, "[test] ${1}");
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script = "require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "replaceheader :newvalue \"[test] ${1}\" \"X-Test-Header\";\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }

    // headerName and headerValue only
    // default comparator and matchType
    @Test
    public void testSoapToSieveReplaceheaderAction2() throws ServiceException, Exception {
        List<String> values = new ArrayList<String>();
        values.add("Test value");
        EditheaderTest test = new EditheaderTest(null, null, null, null, null, "X-Test-Header", values);
        ReplaceheaderAction action = new ReplaceheaderAction(null, null, test, null, "[test] ${1}");
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script ="require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "replaceheader :newvalue \"[test] ${1}\" :comparator \"i;ascii-casemap\" :is \"X-Test-Header\" \"Test value\";\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }

    // headerName and multiple headerValues
    // default comparator and matchType
    @Test
    public void testSoapToSieveReplaceheaderAction3() throws ServiceException, Exception {
        List<String> values = new ArrayList<String>();
        values.add("Value1");
        values.add("Value2");
        values.add("Value3");
        EditheaderTest test = new EditheaderTest(null, null, null, null, null, "X-Test-Header", values);
        ReplaceheaderAction action = new ReplaceheaderAction(null, null, test, null, "[test] ${1}");
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script = "require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "replaceheader :newvalue \"[test] ${1}\" :comparator \"i;ascii-casemap\" :is \"X-Test-Header\" [ \"Value1\", \"Value2\", \"Value3\" ];\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }

    // headerName, multiple headerValues, matchType and comparator
    @Test
    public void testSoapToSieveReplaceheaderAction4() throws ServiceException, Exception {
        List<String> values = new ArrayList<String>();
        values.add("Value1");
        values.add("Value2");
        values.add("Value3");
        EditheaderTest test = new EditheaderTest("contains", null, null, null, "i;octet", "X-Test-Header", values);
        ReplaceheaderAction action = new ReplaceheaderAction(null, null, test, null, "[test] ${1}");
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script = "require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "replaceheader :newvalue \"[test] ${1}\" :comparator \"i;octet\" :contains \"X-Test-Header\" [ \"Value1\", \"Value2\", \"Value3\" ];\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }

    // headerName, multiple headerValues, comparator, value, relationalComparator
    @Test
    public void testSoapToSieveReplaceheaderAction5() throws ServiceException, Exception {
        List<String> values = new ArrayList<String>();
        values.add("2");
        EditheaderTest test = new EditheaderTest(null, null, true, "ge", "i;ascii-numeric", "X-Test-Header", values);
        ReplaceheaderAction action = new ReplaceheaderAction(null, null, test, null, "[test] ${1}");
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script = "require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "replaceheader :newvalue \"[test] ${1}\" :value \"ge\" :comparator \"i;ascii-numeric\" \"X-Test-Header\" \"2\";\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }

    // headerName, multiple headerValues, comparator, value, relationalComparator
    @Test
    public void testSoapToSieveReplaceheaderAction6() throws ServiceException, Exception {
        List<String> values = new ArrayList<String>();
        values.add("2");
        EditheaderTest test = new EditheaderTest(null, null, true, "ge", "i;ascii-numeric", "X-Test-Header", values);
        ReplaceheaderAction action = new ReplaceheaderAction(null, null, test, null, "[test] ${1}");
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script = "require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "replaceheader :newvalue \"[test] ${1}\" :value \"ge\" :comparator \"i;ascii-numeric\" \"X-Test-Header\" \"2\";\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }

    // headerName, multiple headerValues, comparator, count, relationalComparator
    @Test
    public void testSoapToSieveReplaceheaderAction7() throws ServiceException, Exception {
        List<String> values = new ArrayList<String>();
        values.add("2");
        EditheaderTest test = new EditheaderTest(null, true, null, "ge", "i;ascii-numeric", "X-Test-Header", values);
        ReplaceheaderAction action = new ReplaceheaderAction(null, null, test, "X-Test-Header-New", "[test] ${1}");
        FilterRule filterRule = new FilterRule("rule1", true);
        filterRule.addFilterAction(action);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        filterRules.add(filterRule);

        RuleManager.clearCachedRules(account);
        RuleManager.setAdminRulesFromXML(account, filterRules, FilterType.INCOMING, AdminFilterType.BEFORE);

        String script = "require [" + SoapToSieve.requireCommon + ", \"editheader\"];\n\n"
                + "# rule1\n"
                + "replaceheader :newname \"X-Test-Header-New\" :newvalue \"[test] ${1}\" :count \"ge\" :comparator \"i;ascii-numeric\" \"X-Test-Header\" \"2\";\n";

        Assert.assertEquals(script, account.getAdminSieveScriptBefore());
    }
}
