/*
 * Created on 2004. 11. 3.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.redolog.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.cs.redolog.logger.FileHeader;
import com.zimbra.cs.redolog.logger.FileLogReader;
import com.zimbra.cs.redolog.op.CreateMessage;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Liquid;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RedoLogVerify {

    private static Options mOptions = new Options();
    
    static {
        mOptions.addOption("m", "message",   false, "show message body data");
    }

    private static void usage(String errmsg) {
        if (errmsg != null) { 
            System.err.println(errmsg);
        }
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("RedoLogVerify [options] [log files]",
            "where [options] are:", mOptions,
            "and [log files] are redo log files.");
        System.exit((errmsg == null) ? 0 : 1);
    }

    private static CommandLine parseArgs(String args[]) {
        StringBuffer gotCL = new StringBuffer("cmdline: ");
        for (int i = 0; i < args.length; i++) {
            gotCL.append("'").append(args[i]).append("' ");
        }
        
        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(mOptions, args);
        } catch (ParseException pe) {
            usage(pe.getMessage());
        }
        return cl;
    }


    private boolean mDumpMessageBody;
    
    private RedoLogVerify(boolean dumpMsgBody) {
        mDumpMessageBody = dumpMsgBody;
    }

	public boolean scanLog(File logfile) throws Exception {
		boolean good = false;
		FileLogReader logReader = new FileLogReader(logfile, false);
		logReader.open();
        FileHeader header = logReader.getHeader();
        System.out.println("HEADER");
        System.out.println("------");
        System.out.println(header);
        System.out.println("------");
		long lastPosition = 0;

		try {
			RedoableOp op = null;
			while ((op = logReader.getNextOp()) != null) {
				lastPosition = logReader.position();
				System.out.println(op);
                if (mDumpMessageBody && op instanceof CreateMessage) {
                	CreateMessage cm = (CreateMessage) op;
                    byte[] body = cm.getMessageBody();
                    if (body != null) {
                        if (ByteUtil.isGzipped(body)) {
                        	body = ByteUtil.uncompress(body);
                        }
                        System.out.print(new String(body));
                        System.out.println("<END OF MESSAGE>");
                    }
                }
			}
			good = true;
		} catch (IOException e) {
			// The IOException could be a real I/O problem or it could mean
			// there was a server crash previously and there were half-written
			// log entries.  We can't really tell which case it is, so just
			// assume the second case and truncate the file after the last
			// successfully read item.

			long size = logReader.getSize();
			if (lastPosition < size) {
				long diff = size - lastPosition;
				System.out.println("There were " + diff + " bytes of junk data at the end.");
			}
		} finally {
			logReader.close();
		}
		return good;
	}

	private static void usage() {
		System.err.println("Usage: RedoLogVerify <logfile> [<logfile> ...]");
		System.exit(-2);
	}

	public static void main(String[] cmdlineargs) throws Exception {
        Liquid.toolSetup();
        CommandLine cl = parseArgs(cmdlineargs);
        String[] args = cl.getArgs();

		if (args.length < 1)
			usage(null);

		boolean allGood = true;
		RedoLogVerify verify = new RedoLogVerify(cl.hasOption('m'));

		for (int i = 0; i < args.length; i++) {
			File log = new File(args[i]);
			System.out.println("VERIFYING: " + log.getName());
			boolean good = false;
			try {
				good = verify.scanLog(log);
			} catch (IOException e) {
				System.err.println("Exception while verifying " + log.getName());
				e.printStackTrace();
			}
			allGood &= good;
			System.out.println();
		}

		if (!allGood)
			System.exit(-1);
	}
}
