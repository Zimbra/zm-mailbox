package com.zimbra.cs.ephemeral.migrate;

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
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.AllAccountsSource;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.DryRunMigrationCallback;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.EntrySource;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.MigrationCallback;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.MigrationFlag;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.SomeAccountsSource;
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

    private static Options OPTIONS = new Options();

    static {
        OPTIONS.addOption("r", "dry-run", false, "Dry run: display info on what the migration would accomplish");
        OPTIONS.addOption("n", "num-threads", true, "Number of threads to use in the migration. If not set, defaults to 1");
        OPTIONS.addOption("k", "keep-old", false, "Do not delete old values in LDAP after migration");
        OPTIONS.addOption("d", "debug", false, "Enable debug logging");
        OPTIONS.addOption("h", "help", false, "Display this help message");
        OPTIONS.addOption("a", "account", true, "Comma-separated list of accounts to migrate. If not specified, all accounts will be migrated");
        OPTIONS.addOption("s", "set-dest", true, "Set the value of the destionation ephemeral store. Used for testing or debugging.");
        OPTIONS.addOption("u", "unset-dest", false, "Unset the value the destionation ephemeral store. Used for testing or debugging.");
    }

    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup();
        CommandLineParser parser = new GnuParser();
        CommandLine cl = parser.parse(OPTIONS, args);
        List<String> clArgs = cl.getArgList();
        if (clArgs.isEmpty() && !cl.hasOption('d')) {
            throw ServiceException.FAILURE("must specify URL of destionation ephemeral store", null);
        }
        String destURL = clArgs.get(0);

        List<String> attrsToMigrate;
        if (clArgs.size() > 1) {
            attrsToMigrate = clArgs.subList(1, clArgs.size() - 1);
        } else {
            attrsToMigrate = new ArrayList<String>(AttributeManager.getInstance().getEphemeralAttributeNames());
        }
        boolean flagChange = cl.hasOption('s') || cl.hasOption('u');
        if (cl.hasOption("h") || (cl.hasOption('s') && cl.hasOption('u'))) {
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
        if (flagChange && (dryRun || cl.hasOption('n') || cl.hasOption('a') || cl.hasOption('k'))) {
            ZimbraLog.ephemeral.error("cannot specify --set-flag or --unset-flag with -r, -n, -a, or -k options");
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
        MigrationCallback callback;
        if (!dryRun) {
            String backendName = null;
            String[] tokens = destURL.split(":");
            if (tokens != null && tokens.length > 0) {
                backendName = tokens[0];
                if (backendName.equalsIgnoreCase("ldap")) {
                    ZimbraLog.ephemeral.info("ephemeral backend is LDAP; migration is not needed");
                    return;
                }
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
        }
        if (flagChange) {
            EphemeralStore store = new ZimbraMigrationCallback(destURL).getStore(); //EphemeralStore containing the flag
            MigrationFlag flag = AttributeMigration.getMigrationFlag(store);
            if (cl.hasOption('s')) {
                //setting flag
                if (flag.isSet()) {
                    ZimbraLog.ephemeral.info("migration flag is already set on %s", store.getClass().getSimpleName());
                } else {
                    ZimbraLog.ephemeral.info("setting the migration flag on %s", store.getClass().getSimpleName());
                    flag.set();
                    AttributeMigration.clearConfigCacheOnAllServers(true);
                }
            } else {
                //unsetting flag
                if (!flag.isSet()) {
                    ZimbraLog.ephemeral.info("migration flag is not set on %s", store.getClass().getSimpleName());
                } else {
                    ZimbraLog.ephemeral.info("unsetting the migration flag on %s", store.getClass().getSimpleName());
                    flag.unset();
                    AttributeMigration.clearConfigCacheOnAllServers(true);
                }
            }
            return;
        }
        AttributeMigration migration = new AttributeMigration(attrsToMigrate, numThreads);
        migration.setCallback(callback);
        EntrySource source;
        if (cl.hasOption('a')) {
            String[] acctValues = cl.getOptionValue('a').split(",");
            source = new SomeAccountsSource(acctValues);
        } else {
            source = new AllAccountsSource();
        }
        migration.setSource(source);
        if (dryRun || cl.hasOption('k')) {
            migration.setDeleteOriginal(false);
        }
        try {
            migration.migrateAllAccounts();
        } catch (ServiceException e) {
            Zimbra.halt(String.format("error encountered during migration to ephemeral backend at %s; migration cannot proceed", destURL), e);
            return;
        }
    }

    @SuppressWarnings("PMD.DoNotCallSystemExit")
    private static void usage() {
        HelpFormatter format = new HelpFormatter();
        format.printHelp(new PrintWriter(System.err, true), 80,
            "zmmigrateattrs [options] [attr1 attr2 attr3 ...]", null, OPTIONS, 2, 2, null);
            System.exit(0);
    }
}
