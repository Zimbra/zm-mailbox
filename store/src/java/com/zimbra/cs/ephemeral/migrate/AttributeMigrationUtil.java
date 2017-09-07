package com.zimbra.cs.ephemeral.migrate;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Log.Level;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.AllAccountsSource;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.DryRunMigrationCallback;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.EntrySource;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.MigrationCallback;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.SomeAccountsSource;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.ZimbraMigrationCallback;
import com.zimbra.cs.ephemeral.migrate.MigrationInfo.Status;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.util.Zimbra;

/**
 * Command-line utility to migrate attributes to ephemeral storage
 *
 * @author iraykin
 *
 */
public class AttributeMigrationUtil {

    private static Options OPTIONS = new Options();

    static {
        OPTIONS.addOption("r", "dry-run", false, "Dry run: display info on what the migration would accomplish");
        OPTIONS.addOption("n", "num-threads", true, "Number of threads to use in the migration. If not set, defaults to 1");
        OPTIONS.addOption("d", "debug", false, "Enable debug logging");
        OPTIONS.addOption("h", "help", false, "Display this help message");
        OPTIONS.addOption("a", "account", true, "Comma-separated list of accounts to migrate. If not specified, all accounts will be migrated");
        OPTIONS.addOption("s", "status", false, "Show migration status");
        OPTIONS.addOption("c", "clear", false, "Clear the migration info");
    }

    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup();
        CommandLineParser parser = new GnuParser();
        CommandLine cl = parser.parse(OPTIONS, args);
        boolean dryRun = cl.hasOption('r');
        boolean useNumThreads = cl.hasOption('n');
        boolean debug = cl.hasOption('d');
        boolean help = cl.hasOption('h');
        boolean useAccount = cl.hasOption('a');
        boolean showStatus = cl.hasOption('s');
        boolean clear = cl.hasOption('c');
        List<String> clArgs = cl.getArgList();
        if (clArgs.isEmpty() && !help && !clear && !showStatus) {
            ZimbraLog.ephemeral.error("must specify URL of destionation ephemeral store");
            return;
        }
        if (help || (clear && showStatus)) {
            usage();
            return;
        }
        if (debug) {
            ZimbraLog.ephemeral.setLevel(Level.debug);
        }
        if (dryRun && useNumThreads) {
            ZimbraLog.ephemeral.error("cannot specify --num-threads with --dry-run option");
            return;
        }
        if (clear && (dryRun || useNumThreads || useAccount || showStatus || !clArgs.isEmpty())) {
            ZimbraLog.ephemeral.error("cannot specify --clear with arguments or other options");
            return;
        }
        if (showStatus && (dryRun || useNumThreads || useAccount || clear || !clArgs.isEmpty())) {
            ZimbraLog.ephemeral.error("cannot specify --status with arguments or other options");
            return;
        }
        //a null numThreads value causes the migration process to run synchronously
        Integer numThreads = null;
        if (!dryRun) { //dry runs are always synchronous
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
        if (showStatus) {
            showMigrationInfo();
            return;
        } else if (clear) {
            clearMigrationInfo();
            return;
        }
        String destURL = clArgs.get(0);
        List<String> attrsToMigrate;
        MigrationCallback callback;
        if (clArgs.size() > 1) {
            attrsToMigrate = clArgs.subList(1, clArgs.size());
        } else {
            attrsToMigrate = new ArrayList<String>(AttributeManager.getInstance().getEphemeralAttributeNames());
        }

        if (!dryRun) {
            String backendName = null;
            String[] tokens = destURL.split(":");
            if (tokens != null && tokens.length > 0) {
                backendName = tokens[0];
                if (backendName.equalsIgnoreCase("ldap")) {
                    ZimbraLog.ephemeral.info("migrating to LDAP is not supported");
                    return;
                }
            }
            if (Provisioning.getInstance().getConfig().getEphemeralBackendURL().equalsIgnoreCase(destURL)) {
                ZimbraLog.ephemeral.info("destination URL cannot be the same as the current ephemeral backend URL");
                return;
            }
            ExtensionUtil.initEphemeralBackendExtension(backendName);
            try {
                callback = new ZimbraMigrationCallback(destURL);
            } catch (ServiceException e) {
                Zimbra.halt(String.format("unable to connect to ephemeral backend at %s; migration cannot proceed", destURL), e);
                return;
            }
        } else {
            callback = new DryRunMigrationCallback();
            MigrationInfo.setFactory(InMemoryMigrationInfo.Factory.class);
        }
        AttributeMigration migration = new AttributeMigration(attrsToMigrate, numThreads);
        AttributeMigration.setCallback(callback);
        EntrySource source;
        if (useAccount) {
            String[] acctValues = cl.getOptionValue('a').split(",");
            source = new SomeAccountsSource(acctValues);
        } else {
            source = new AllAccountsSource();
        }
        migration.setSource(source);
        try {
            migration.migrateAllAccounts();
        } catch (ServiceException e) {
            Zimbra.halt(String.format("error encountered during migration to ephemeral backend at %s; migration cannot proceed", destURL), e);
            return;
        }
    }

    private static void clearMigrationInfo() throws ServiceException {
        MigrationInfo info = AttributeMigration.getMigrationInfo();
        Status curStatus = info.getStatus();
        if (curStatus == Status.NONE) {
            ZimbraLog.ephemeral.info("no migration info available");
        } else {
            ZimbraLog.ephemeral.info("resetting info for migration to %s currently marked as %s", info.getURL(), info.getStatus().toString());
            info.clearData();
        }
    }

    private static void showMigrationInfo() throws ServiceException {
        MigrationInfo info = AttributeMigration.getMigrationInfo();
        Status curStatus = info.getStatus();
        String url = info.getURL();
        String started = info.getDateStr("MM/dd/yyyy HH:mm:ss");
        PrintStream console = System.out;
        if (curStatus == Status.NONE) {
            console.println("No attribute migration info available");
        } else {
            console.println(String.format("Status:  %s", curStatus.toString()));
            console.println(String.format("URL:     %s", url));
            if (started != null) {
                console.println(String.format("started: %s", started));
            }
        }
    }

    @SuppressWarnings("PMD.DoNotCallSystemExit")
    private static void usage() {
        HelpFormatter format = new HelpFormatter();
        format.printHelp(new PrintWriter(System.err, true), 80,
            "zmmigrateattrs [options] [URL] [attr1 attr2 attr3 ...]", null, OPTIONS, 2, 2, null);
            System.exit(0);
    }
}
