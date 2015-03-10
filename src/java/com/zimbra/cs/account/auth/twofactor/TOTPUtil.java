package com.zimbra.cs.account.auth.twofactor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.twofactor.CredentialConfig.Encoding;

public class TOTPUtil {

    private static Options OPTIONS = new Options();

    static {
        OPTIONS.addOption("s", "secret", true,  "shared secret");
        OPTIONS.addOption("a", "account", true, "account name, defaults to user1");
    }

    public static void main(String[] args) throws ParseException, ServiceException {
        CliUtil.toolSetup();
        CommandLineParser parser = new GnuParser();
        CommandLine cl = parser.parse(OPTIONS, args);
        Provisioning prov = Provisioning.getInstance();
        String acctName;
        String secret;
        if (cl.hasOption("a")) {
            acctName = cl.getOptionValue("a");
        } else {
            System.err.println("please specify a user name");
            return;
        }
        if (cl.hasOption("s")) {
            secret = cl.getOptionValue("s");
        } else {
            System.err.println("please specify a shared secret");
            return;
        }
        Account acct = prov.getAccountByName(acctName);
        TwoFactorManager manager = new TwoFactorManager(acct);
        AuthenticatorConfig config = manager.getAuthenticatorConfig();
        TOTPAuthenticator authenticator = new TOTPAuthenticator(config);
        Encoding encoding = manager.getCredentialConfig().getEncoding();
        Long timestamp = System.currentTimeMillis() / 1000;
        String code = authenticator.generateCode(secret, timestamp, encoding);
        System.out.println("Current TOTP code is: " + code);
    }
}
