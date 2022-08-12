/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.db;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.util.Config;

/**
 * @since Jul 7, 2004
 * @author tim
 */
public final class Versions {

    private static final Log LOG = LogFactory.getLog(Versions.class);

    /**
     * The DB_VERSION is stored into the config table of the DB when the DB is created.
     * If the DB_VERSION does not match our server's version, we will not run.
     *
     * UPDATE THESE TO REQUIRE RESET-WORLD TO BE RUN
     */
    public static final int DB_VERSION = 116;

    /**
     * The INDEX_VERSION is stored into the config table of the DB when the DB is created.
     * If the INDEX_VERSION does not match our server's version, we will not run.
     *
     * UPDATE THESE TO REQUIRE RESET-WORLD TO BE RUN
     */
    public static final int INDEX_VERSION = 2;

    private Versions() {
    }

    /////////////////////////////////////////////////////////////
    // Called at boot time
    /////////////////////////////////////////////////////////////
    public static boolean checkVersions() {
        return (checkDBVersion() && checkIndexVersion());
    }

    public static boolean checkDBVersion() {
        String val = Config.getString("db.version", "0");
        if (val.equals(Integer.toString(DB_VERSION))) {
            return true;
        } else {
            LOG.error("DB Version Mismatch: ours=%s from DB=%s", DB_VERSION, val);
            return false;
        }
    }

    public static boolean checkIndexVersion() {
        String val = Config.getString("index.version", "0");
        if (val.equals(Integer.toString(INDEX_VERSION))) {
            return true;
        } else {
            LOG.error("Index Version Mismatch: ours=%s from DB=%s", INDEX_VERSION, val);
            return false;
        }
    }

    public static int getDbVersion() {
        return DB_VERSION;
    }

    public static int getIndexVersion() {
        return INDEX_VERSION;
    }

    /////////////////////////////////////////////////////////////
    // main and command-line parsing
    /////////////////////////////////////////////////////////////

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(Versions.class.getName(), options);
        System.exit(1);
    }

    static CommandLine parseCmdlineArgs(String args[], Options options) {
        CommandLineParser parser = new GnuParser();

        // Loose convention for naming options:
        //
        // Options applicable for normal, production usage have lowercase
        // letter.  Options for debugging, testing, or diagnostic
        // uses have uppercase letter.

        options.addOption("h", "help", false, "print usage");
        options.addOption("o", "outputdir", true, "output directory for version.sql");

        CommandLine cl = null;
        boolean err = false;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException pe) {
            System.err.println("error: " + pe.getMessage());
            err = true;
        }

        if (err || cl.hasOption("h"))
            usage(options);

        return cl;
    }
}
