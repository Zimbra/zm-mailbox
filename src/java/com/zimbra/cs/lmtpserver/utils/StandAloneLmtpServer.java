package com.zimbra.cs.lmtpserver.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.cs.lmtpserver.LmtpBackend;
import com.zimbra.cs.lmtpserver.LmtpServer;
import com.zimbra.cs.util.Zimbra;

public class StandAloneLmtpServer {

	private static Options mOptions = new Options();
	
	static {
		mOptions.addOption("p", "port", true, "listen port (required)");
		mOptions.addOption("a", "address", true, "bind address");
		mOptions.addOption("t", "threads", true, "threads in pool");
		mOptions.addOption("h", "help", false, "print usage");
		mOptions.addOption("u", "users", true, "properties file that lists users' maildir location");
	}
	
	private static void usage(String error) {
		if (error != null) { 
			System.err.println("Error: " + error);
		}
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(StandAloneLmtpServer.class.getName(), mOptions); 
		System.exit((error == null) ? 0 : 1);
	}

	private static CommandLine parseArgs(String args[]) {
        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(mOptions, args);
        } catch (ParseException pe) {
            usage(pe.getMessage());
        }
        if (cl.hasOption("h")) {
        	usage(null);
        }
        return cl;
    }

	
	public static void main(String[] args) {
        Zimbra.toolSetup();
		
        CommandLine cl = parseArgs(args);

		int port;
		InetAddress bindAddress;
		int threads;
		
		if (cl.hasOption("p")) {
			port = Integer.valueOf(cl.getOptionValue("p")).intValue();
			if (port <= 0) {
				usage("invalid port number: " + port);
			}
		} else {
			usage("port not specified");
			return;
		}
		
		if (cl.hasOption("a")) {
			String name = cl.getOptionValue("a");
			try {
				bindAddress = InetAddress.getByName(name);
			} catch (UnknownHostException uhe) {
				usage("address is unknown" + uhe.getMessage());
				return;
			}
		} else {
			bindAddress = null;
		}
		
		if (cl.hasOption("t")) {
			threads = Integer.valueOf(cl.getOptionValue("t")).intValue();
			if (threads <= 0) {
				usage("invalid number of threads: " + threads);
			}
		} else {
			threads = 5;
		}

		Properties maildirMap = null;
		if (cl.hasOption("u")) {
			try {
				maildirMap = new Properties();
				maildirMap.load(new BufferedInputStream(new FileInputStream(cl.getOptionValue("u"))));
				maildirMap.list(System.out);
			} catch (IOException ioe) {
				usage(ioe.getMessage());
			}
		} else {
			usage("maildir map file not specified");
		}

		LmtpServer lmtpServer = new LmtpServer(threads, port, bindAddress);
		lmtpServer.setConfigNameFromHostname();
		
		LmtpBackend backend = new MaildirBackend(maildirMap);
		lmtpServer.setConfigBackend(backend);
		
		Thread acceptThread = new Thread(lmtpServer);
		acceptThread.setName("LmtpServer");
		acceptThread.start();
		
		try {
			while (true) {
				byte[] line = new byte[1024];
				System.in.read(line);
				String command = new String(line, 0, 5);
				if ("quit\n".equalsIgnoreCase(command)) {
					break;
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		lmtpServer.shutdown(15);
	}
}
