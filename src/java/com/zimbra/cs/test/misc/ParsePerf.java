/*
 * Created on 2004. 11. 29.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.liquidsys.coco.test.misc;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.mail.internet.MimeMessage;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.liquidsys.coco.mime.ParsedMessage;
import com.liquidsys.coco.util.ByteUtil;
import com.liquidsys.coco.util.JMSession;
import com.liquidsys.coco.util.Liquid;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ParsePerf {

    private static Options mOptions = new Options();
    
    static {
        mOptions.addOption("d", "directory", true,  "message file directory");
        mOptions.addOption("n", "every",     true,  "report progress after every N messages (default 100)");
    }

    private static void usage(String errmsg) {
        if (errmsg != null) { 
            System.err.println(errmsg);
        }
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ParsePerf [options] [files]",
                "where [options] are one of:", mOptions,
                "and [files] contain rfc822 messages.  If directory is specified, then " +
                "[files] are ignored.");
        System.exit((errmsg == null) ? 0 : 1);
    }

    private static CommandLine parseArgs(String args[]) {
        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(mOptions, args);
        } catch (ParseException pe) {
            usage(pe.getMessage());
        }
        return cl;
    }

    public static void main(String[] args) throws Exception {
        Liquid.toolSetup();

        CommandLine cl = parseArgs(args);

        int everyN;
        if (cl.hasOption("n")) {
            everyN = Integer.valueOf(cl.getOptionValue("n")).intValue();
        } else {
            everyN = 100;
        }

        File[] files;
        if (cl.hasOption("d")) {
            File dir = new File(cl.getOptionValue("d"));
            files = dir.listFiles();
            if (files == null || files.length == 0) {
                System.err.println("No files found in specified directory " + dir);
                System.exit(-1);
            }
        } else {
            args = cl.getArgs();
            if (args.length == 0) {
                usage("no input files specified");
            }
            files = new File[args.length];
            for (int i = 0; i < args.length; i++) {
                files[i] = new File(args[i]);
            }
        }

        long startTime = System.currentTimeMillis();
        int processed = 0;
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory())
                continue;

            byte[] data = ByteUtil.getContent(files[i]);
            ByteArrayInputStream is = new ByteArrayInputStream(data);
            MimeMessage mm = new MimeMessage(JMSession.getSession(), is);
            is.close();
            
            ParsedMessage pm = new ParsedMessage(mm, System.currentTimeMillis(), true).analyze();
            processed++;
            if (processed % everyN == 0) {
            	System.out.println("Processed " + processed + " files");
            }
        }
        long elapsedMS = System.currentTimeMillis() - startTime;

        double elapsed = ((double) elapsedMS) / 1000.0;
        double msPerMsg = 0.0;
        if (processed > 0) {
            msPerMsg = elapsedMS;
            msPerMsg /= processed;
            msPerMsg = (double) ((long) Math.round(msPerMsg * 1000)) / 1000;
        }
        double msgPerSec = ((double) processed / (double) elapsedMS) * 1000;
        msgPerSec = (double) ((long) Math.round(msgPerSec * 1000)) / 1000;
        System.out.println();
        System.out.println("ParsePerf finished processing " + processed + " files");
        System.out.println(elapsed + "s, " + msPerMsg + "ms/msg, " + msgPerSec + "mps");
    }
}
