package com.zimbra.cs.ephemeral.migrate;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Log.Level;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.AllAccountsSource;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.DryRunMigrationCallback;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.EntrySource;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.MigrationCallback;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.SingleAccountSource;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.ZimbraMigrationCallback;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.util.Zimbra;

/**
 * Command-line utility to migrate attributes to ephemeral storage
 *
 * @author iraykin
 *
 */
public class AttributeMigrationUtil {

    private static final PrintStream console = System.out;
    private static Options OPTIONS = new Options();

    private static Map<String, String> extensionMap = new HashMap<String, String>();

    //This map should be updated whenever a new EphemeralStore backend extension is implemented
    static {
        extensionMap.put("ssdb", "com.zimbra.ssdb.SSDBEphemeralStoreExtension");
    }

    static {
        OPTIONS.addOption("a", "account", true, "Specific account to migrate. If not specified, all accounts will be migrated");
        OPTIONS.addOption("r", "dry-run", false, "Dry run: display info on what the migration would accomplish");
        OPTIONS.addOption("n", "num-threads", true, "Number of threads to use in the migration. If not set, defaults to 1");
        OPTIONS.addOption("k", "keep-old", false, "Do not delete old values in LDAP after migration");
        OPTIONS.addOption("d", "debug", false, "Enable debug logging");
        OPTIONS.addOption("h", "help", false, "Display this help message");
    }

    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup();
        CommandLineParser parser = new GnuParser();
        CommandLine cl = parser.parse(OPTIONS, args);
        List<String> attrsToMigrate = cl.getArgList();
        if (cl.hasOption("h") || attrsToMigrate.isEmpty()) {
            usage();
            return;
        }
        if (cl.hasOption('d')) {
            ZimbraLog.ephemeral.setLevel(Level.debug);
        }
        boolean dryRun = cl.hasOption('r');
        if (dryRun && cl.hasOption('n')) {
            ZimbraLog.ephemeral.error("cannot specify --num-threads with --dry-run option");
            return;
        }
        Integer numThreads = null;
        if (!dryRun) { //dryrun is synchronous
            try {
                numThreads = Integer.valueOf(cl.getOptionValue('n', "1"));
                if (numThreads < 1) {
                    ZimbraLog.ephemeral.error("invalid num-threads value: '%s'", numThreads);
                    return;
                }
            } catch (NumberFormatException e) {
                ZimbraLog.ephemeral.error("invalid num-threads value: '%s'", cl.getOptionValue('n'));
                return;
            }
        }
        AttributeMigration migration = new AttributeMigration(attrsToMigrate, numThreads);
        MigrationCallback callback;
        if (!dryRun) {
            initEphemeralBackendExtension();
            callback = new ZimbraMigrationCallback();
        } else {
            callback = new DryRunMigrationCallback();
        }
        migration.setCallback(callback);
        EntrySource source;
        if (cl.hasOption('a')) {
            String acctValue = cl.getOptionValue('a');
            AccountBy acctBy = StringUtil.isUUID(acctValue) ? AccountBy.id : AccountBy.name;
            source = new SingleAccountSource(acctBy, acctValue);
        } else {
            source = new AllAccountsSource();
        }
        migration.setSource(source);
        if (dryRun || cl.hasOption('k')) {
            migration.setDeleteOriginal(false);
        }
        migration.migrateAllAccounts();
    }

    private static void initEphemeralBackendExtension() throws ServiceException {
        String url = Provisioning.getInstance().getConfig().getEphemeralBackendURL();
        if (url != null) {
            String[] tokens = url.split(":");
            if (tokens != null && tokens.length > 0) {
                String backendName = tokens[0];
                if (backendName.equalsIgnoreCase("ldap")) {
                    ZimbraLog.ephemeral.info("Using LDAP ephemeral backend for attribute migration");
                    return;
                }
                String backendExtensionClass = extensionMap.get(backendName);
                if (backendExtensionClass == null) {
                    Zimbra.halt(String.format("no extension class name found for backend '%s', aborting attribute migration", backendName));
                }
                ZimbraLog.ephemeral.info("Using ephemeral backend %s (%s) for attribute migration", backendName, backendExtensionClass);
                ExtensionUtil.init(backendExtensionClass);
            }
        }
    }

    private static void usage() {
        HelpFormatter format = new HelpFormatter();
        format.printHelp(new PrintWriter(System.err, true), 80,
            "zmmigrateattrs [options] attr1 [attr2 attr3 ...]", null, OPTIONS, 2, 2, null);
            System.exit(0);
    }
}
