package com.zimbra.cs.lmtpserver.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.zimbra.cs.lmtpserver.LmtpProtocolException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ThreadPool;

public class LmtpInject {

	private static Log mLog = LogFactory.getLog(LmtpInject.class);

	private static Options mOptions = new Options();
	
	static {
		mOptions.addOption("d", "directory", true,  "message file directory");
		mOptions.addOption("a", "address",   true,  "lmtp server (default localhost)");
		mOptions.addOption("p", "port",      true,  "lmtp server port (default 7025)");
		mOptions.addOption("s", "sender",    true,  "envelope sender (mail from)");
        Option ropt = new Option("r", "recipient", true, "envelope recipients (rcpt to)");
        ropt.setArgs(Option.UNLIMITED_VALUES);
		mOptions.addOption(ropt);
		mOptions.addOption("t", "threads",   true,  "number of worker threads (default 1)");
		mOptions.addOption("q", "quiet",     false, "don't print per-message status");
		mOptions.addOption("T", "trace",     false, "trace server/client traffic");
		mOptions.addOption("N", "every",     true,  "report progress after every N messages (default 100)");
		mOptions.addOption("w", "warmUpThreshold", true, "warm-up server with first N messages, then start measuring (default no warm-up)");
        mOptions.addOption("S", "stopAfter", true,  "stop after sending this many messages after warm-up");
        mOptions.addOption("u", "username",  true,  "username prefix (default \"user\")");
        mOptions.addOption("D", "domain",    true,  "default per-connection recipient domain (default example.zimbra.com)");
        mOptions.addOption("z", "repeat",    true,  "repeatedly inject these messages NUM times");
	}

	private static void usage(String errmsg) {
		if (errmsg != null) { 
			mLog.error(errmsg);
		}
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("LmtpInject [options] [files]",
        		"where [options] are one of:", mOptions,
				"and [files] contain rfc822 messages.  If directory is specified, then " +
				"[files] are ignored.");
		System.exit((errmsg == null) ? 0 : 1);
	}

	private static CommandLine parseArgs(String args[]) {
		StringBuffer gotCL = new StringBuffer("cmdline: ");
		for (int i = 0; i < args.length; i++) {
			gotCL.append("'").append(args[i]).append("' ");
		}
		//mLog.info(gotCL);
		
        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(mOptions, args);
        } catch (ParseException pe) {
            usage(pe.getMessage());
        }
        return cl;
    }

	public static void main(String[] args) {
        Zimbra.toolSetup();
        CommandLine cl = parseArgs(args);

		int threads = 1;
		if (cl.hasOption("t")) {
			threads = Integer.valueOf(cl.getOptionValue("t")).intValue();
		}
		String host = null;
		if (cl.hasOption("a")) {
			host = cl.getOptionValue("a");
		} else {
			host = "localhost";
		}

		int port;
		if (cl.hasOption("p")) {
			port = Integer.valueOf(cl.getOptionValue("p")).intValue();
		} else {
			port = 7025;
		}

        mLog.info("connections=" + threads + " host=" + host + " port=" + port);

        String defaultUsernamePrefix = "user";
        if (cl.hasOption("u")) {
            defaultUsernamePrefix = cl.getOptionValue("u");
        }
        LmtpClientFactory.setUsernamePrefix(defaultUsernamePrefix);

        String defaultRecipientDomain = "example.zimbra.com";
		if (cl.hasOption("D")) {
			defaultRecipientDomain = cl.getOptionValue("D");
		}
		LmtpClientFactory.setRecipientDomain(defaultRecipientDomain);

		String[] recipients = null;
		String sender = null;
		if (cl.hasOption("r")) 
		    recipients = cl.getOptionValues("r");
		if (recipients == null)
		    mLog.info("Using default recipients " + defaultUsernamePrefix + 1 + "@" + defaultRecipientDomain +
		            " through " + defaultUsernamePrefix + threads + "@" + defaultRecipientDomain);
		if (cl.hasOption("s")) { 
		    sender = cl.getOptionValue("s");
		} else {
		    usage("sender not specified");
		}

		boolean quietMode = cl.hasOption("q");
		boolean tracingEnabled = cl.hasOption("T");

		int everyN;
		if (cl.hasOption("N")) {
			everyN = Integer.valueOf(cl.getOptionValue("N")).intValue();
		} else {
			everyN = 100;
		}

		int warmUpThreshold = 0;
		if (cl.hasOption("w")) {
			warmUpThreshold = Integer.valueOf(cl.getOptionValue("w")).intValue();
		}
		if (warmUpThreshold < 0)
			warmUpThreshold = 0;

        int stopAfter = 0;
        if (cl.hasOption("S")) {
        	stopAfter = Integer.valueOf(cl.getOptionValue("S")).intValue();
        }        

		File[] files;
		if (cl.hasOption("d")) {
			File dir = new File(cl.getOptionValue("d"));
			files = dir.listFiles();
			if (files == null || files.length == 0) {
				mLog.error("No files found in specified directory " + dir);
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
        
        int numRuns = 1;
        if (cl.hasOption("z")) {
            numRuns = Integer.parseInt(cl.getOptionValue("z"));
            mLog.info("-z option requests "+numRuns+" runs.");
        }

        int totalFailed = 0;
        int totalSucceeded = 0;
        
        for (int runCount = 0; runCount < numRuns; runCount++) {
            
            LmtpInject injector = null;
            try {
                injector = new LmtpInject(threads,
                        sender, recipients,
                        host, port,
                        quietMode, tracingEnabled);
            } catch (Exception e) {
                mLog.error("Unable to initialize LmtpInject", e);
                System.exit(-1);
            }
            
            injector.setReportEvery(everyN);
            int numFiles = files.length;
            if (stopAfter > 0 && warmUpThreshold >= 0)
                numFiles = Math.min(files.length, warmUpThreshold + stopAfter);
            injector.setNumInputFiles(numFiles);
            injector.markStartTime();
            if (warmUpThreshold > 0)
                injector.enableWarmUp(warmUpThreshold);
            
            for (int i = 0; i < numFiles; i++) {
                if (files[i].isDirectory()) {
                    mLog.info("Ignoring directory " + files[i].getPath());
                    injector.incIgnored();
                    continue;
                }
                
                boolean result = injector.processFile(files[i]);
                if (!result)
                    injector.incFailure();
            }
            
            synchronized (injector.mFinishCond) {
                try {
                    injector.mFinishCond.wait();
                } catch (InterruptedException e) {
                    mLog.warn("InterruptedException while waiting for queue to clear", e);
                }
            }
            
            // Wait until thread pool finishes processing all scheduled injections.
            try {
                injector.cleanup();
            } catch (Exception e) {
                mLog.warn("Exception while shutting down LmtpInject", e);
            }
            
            int succeeded = injector.getSuccessCount();
            int failedThisTime = injector.getFailureCount();
            long elapsedMS = injector.getElapsedTime();
            double elapsed = ((double) elapsedMS) / 1000.0;
            double msPerMsg = 0.0;
            double msgSizeKB = 0.0;
            if (succeeded > 0) {
                msPerMsg = elapsedMS;
                msPerMsg /= succeeded;
                msPerMsg = (double) ((long) Math.round(msPerMsg * 1000)) / 1000;
                msgSizeKB = ((double) injector.mFileSizeTotal) / 1024.0;
                msgSizeKB /= succeeded;
                msgSizeKB = (double) ((long) Math.round(msgSizeKB * 1000)) / 1000;
            }
            double msgPerSec = ((double) succeeded / (double) elapsedMS) * 1000;
            msgPerSec = (double) ((long) Math.round(msgPerSec * 1000)) / 1000;
            System.out.println();
            System.out.println("LmtpInject Finished");
            System.out.println("submitted=" + succeeded + " failed=" + failedThisTime);
            System.out.println("maximum concurrent active connections: " + injector.mHwmActiveClients);
            System.out.println(elapsed + "s, " + msPerMsg + "ms/msg, " + msgPerSec + "mps");
            System.out.println("average message size = " + msgSizeKB + "KB");
            
            totalFailed+=failedThisTime;
            totalSucceeded+=succeeded;
        }
        
        if (numRuns > 1) {
            System.out.println("\nLmtpInject Finished "+numRuns+" runs");
            System.out.println("submitted=" + totalSucceeded + " failed=" + totalFailed);
        }
        
        if (totalFailed!= 0)
            System.exit(1);
    }


	private String mSender;
	private List /*<String>*/ mRecipients;

	private ThreadPool mThreadPool;
	private LmtpClientPool mLmtpClientPool;

	private final Object mFinishCond = new Object();
	private int mNumInputFiles;
	private int mSucceeded;
	private int mFailed;
	private int mIgnored;

	private long mStartTime;
	private long mEndTime;
	private long mLastProgressTime;
	private int mLastProgressCount;

    private final Object mFileSizeTotalGuard = new Object();
    private long mFileSizeTotal = 0;

	private int mActiveClients;
	private int mHwmActiveClients;

	private LmtpInject(int threadPoolSize,
					   String sender,
					   String[] recipients,
					   String host,
					   int port,
					   boolean quietMode,
					   boolean tracingEnabled)
	throws Exception {
		mSender = sender;
		if (recipients != null) {
			mRecipients = new ArrayList(recipients.length);
			for (int i = 0; i < recipients.length; i++)
				mRecipients.add(recipients[i]);
		}

		mNumInputFiles = mSucceeded = mFailed = mIgnored = 0;
		mStartTime = mEndTime = mLastProgressTime = 0;
		mLastProgressCount = 0;

		mActiveClients = 0;
		mHwmActiveClients = 0;

		if (threadPoolSize < 1)
			threadPoolSize = 1;

		mLmtpClientPool = new LmtpClientPool(threadPoolSize, host, port, quietMode, tracingEnabled);

		// Force all LMTP connections in pool to connect to server.
		LmtpClient clients[] = new LmtpClient[threadPoolSize];
		for (int i = 0; i < threadPoolSize; i++) {
			clients[i] = mLmtpClientPool.getClient();
		}
		for (int i = 0; i < threadPoolSize; i++) {
			mLmtpClientPool.releaseClient(clients[i]);
		}

		mThreadPool = new ThreadPool("LmtpInject", threadPoolSize);
	}

	public void cleanup() throws Exception {
		mThreadPool.shutdownNow();
		mLmtpClientPool.close();
	}

	public boolean processFile(File file) {
		boolean result = true;
		LmtpInjectTask task = new LmtpInjectTask(this, file);
		try {
			mThreadPool.execute(task);
		} catch (InterruptedException e) {
			mLog.error("InterruptedException while processing " + file.getName(), e);
			result = false;
		}
		return result;
	}

	public LmtpClient getClient() throws Exception {
		return mLmtpClientPool.getClient();
	}

	public void releaseClient(LmtpClient client) throws Exception {
		mLmtpClientPool.releaseClient(client);
	}

	public synchronized long getElapsedTime() { return mEndTime - mStartTime; }
	public synchronized void markStartTime() {
		mStartTime = mLastProgressTime = System.currentTimeMillis();
	}
	private int mWarmUpThreshold = 0;
	private boolean mWarmedUp = true;
	public synchronized void enableWarmUp(int warmUpThreshold) {
		mWarmedUp = false;
		mWarmUpThreshold = warmUpThreshold;
	}

    private int mReportEvery = 100;
	public synchronized void setReportEvery(int num) { mReportEvery = num; }
	public void incSuccess() {
		int count;
		int lastCount = 0;
		long lastTime = 0;
		long startTime = 0;
		long now = 0;
		boolean report = false;
		int activeClients = 0;
		int activeClientsHWM = 0;

		synchronized (this) {
			count = ++mSucceeded;
			checkDone();
			if (count % mReportEvery == 0) {
				report = true;
				startTime = mStartTime;
				lastCount = mLastProgressCount;
				lastTime = mLastProgressTime;
				mLastProgressCount = count;
				now = System.currentTimeMillis();
				mLastProgressTime = now;
				activeClients = mActiveClients;
				activeClientsHWM = mHwmActiveClients;
			}
			if (!mWarmedUp && count == mWarmUpThreshold) {
				markStartTime();
				mEndTime = 0;
				mLastProgressCount = 0;
				mSucceeded = 0;
				mWarmedUp = true;
				System.out.println("Server warmed up after " + mWarmUpThreshold + " messages.  Resetting stats.");
				report = false;
                synchronized (mFileSizeTotalGuard) {
                    mFileSizeTotal = 0;
                }
			}
		}
		if (report) {
			long elapsed = now - lastTime;
			long howmany = count - lastCount;
			long rate = howmany * 1000 / elapsed;

			long elapsedTotal = now - startTime;
			long rateAvg = count * 1000 / elapsedTotal;

			String prefix = mWarmedUp ? "[progress] " : "[warm-up] ";
			System.out.println(
			    prefix + count + " msgs, " + (elapsedTotal / 1000) + "s, " + rateAvg + "mps; " +
				"last " + howmany + " msgs: " + elapsed + "ms, " + rate + "mps (active: " +
				activeClients + "/" + activeClientsHWM + ")");
		}
	}
	public synchronized void incFailure() { mFailed++; checkDone(); }
	public synchronized void incIgnored() { mIgnored++; checkDone(); }
	public synchronized void setNumInputFiles(int n) { mNumInputFiles = n; }
	public synchronized int getSuccessCount() { return mSucceeded; }
	public synchronized int getFailureCount() { return mFailed; }
	public String getSender() { return mSender; }
	public List /*<String>*/ getRecipients() { return mRecipients; }

	private void checkDone() {
		if (mEndTime == 0 && mSucceeded + mFailed + mIgnored + mWarmUpThreshold == mNumInputFiles) {
			mEndTime = System.currentTimeMillis();
			synchronized (mFinishCond) {
				mFinishCond.notify();
			}
		}
	}

    public void addToFileSizeTotal(long size) {
        synchronized (mFileSizeTotalGuard) {
            mFileSizeTotal += size;
        }
    }

    public synchronized void incActiveClients() {
		mActiveClients++;
		if (mActiveClients > mHwmActiveClients)
			mHwmActiveClients = mActiveClients;
	}

	public synchronized void decActiveClients() {
		mActiveClients--;
	}

	private static class LmtpInjectTask implements Runnable {
		private LmtpInject mDriver;
		private File mFile;

		public LmtpInjectTask(LmtpInject driver, File file) {
			mDriver = driver;
			mFile = file;
		}

		public void run() {
			mDriver.incActiveClients();
			String filename = mFile.getName();
			LmtpClient client = null;
			try {
				//mLog.info("Processing " + filename);

				client = mDriver.getClient();

				boolean ok = false;
				byte[] msg = ByteUtil.getContent(mFile);
				ok = client.sendMessage(msg, mDriver.getRecipients(), mDriver.getSender(), filename);
				if (ok) {
					mDriver.incSuccess();
                    mDriver.addToFileSizeTotal(mFile.length());
				} else {
					mDriver.incFailure();
				}
			} catch (Exception e) {
				mDriver.incFailure();
				mLog.warn("Delivery failed for " + filename + ": ", e);
			} finally {
				if (client != null) {
					try {
						mDriver.releaseClient(client);
					} catch (Exception e) {
						mLog.warn("Unable to release LMTP client", e);
					}
				}
				mDriver.decActiveClients();
			}
		}
	}

	
	// LMTP connection pool
	
	private static class LmtpClientFactory implements PoolableObjectFactory {

		private static int sNextRecipientNum = 1;
        private static String sUsernamePrefix;
		private static String sRecipientDomain;

        private static void setUsernamePrefix(String prefix) {
        	sUsernamePrefix = prefix;
        }

        private static void setRecipientDomain(String domain) {
			sRecipientDomain = domain;
		}

		private static synchronized int getNextRecipientNum() {
			return sNextRecipientNum++;
		}

		private static String getNextRecipientAddress() {
			return sUsernamePrefix + getNextRecipientNum() + "@" + sRecipientDomain;
		}

		private String mHost;
		private int mPort;
		private boolean mQuietMode;
		private boolean mTracingEnabled;

		public LmtpClientFactory(String host, int port, boolean quietMode, boolean tracingEnabled) {
			mHost = host;
			mPort = port;
			mQuietMode = quietMode;
			mTracingEnabled = tracingEnabled;
		}

		public Object makeObject() throws Exception {
			LmtpClient client = null;
			String rcpt = getNextRecipientAddress();
			try {
				client = new NamedLmtpClient(rcpt, mHost, mPort);
			} catch (IOException e) {
				mLog.error("Connection to LMTP server failed: ", e);
				throw e;
			}
			client.quiet(mQuietMode);
			client.trace(mTracingEnabled);
			client.warnOnRejectedRecipients(false);
			return client;
		}

		public void destroyObject(Object obj) throws Exception { ((LmtpClient) obj).close(); }
		public boolean validateObject(Object obj) { return true; }
		public void activateObject(Object obj) throws Exception {}
		public void passivateObject(Object obj) throws Exception {}
	}

	private static class NamedLmtpClient extends LmtpClient {
		private List /*<String>*/ mRecipients;

		NamedLmtpClient(String recipient, String host, int port) throws IOException {
			super(host, port);
			mRecipients = new ArrayList(1);
			mRecipients.add(recipient);
		}

	    public boolean sendMessage(byte[] msg, List /*<String>*/ recipients, String sender, String logLabel)
            throws IOException, LmtpProtocolException
	    {
	    	List rcpts = recipients != null ? recipients : mRecipients;
	    	return super.sendMessage(msg, rcpts, sender, logLabel);
	    }
	}

	private static class LmtpClientPool {
		private ObjectPool mPool;

		public LmtpClientPool(int poolSize, String host, int port, boolean quietMode, boolean tracingEnabled) {
			LmtpClientFactory factory = new LmtpClientFactory(host, port, quietMode, tracingEnabled);
			mPool = new GenericObjectPool(factory, poolSize, GenericObjectPool.WHEN_EXHAUSTED_BLOCK, -1, poolSize);
		}

		public void close() throws Exception {
			mPool.close();
		}

		public LmtpClient getClient() throws Exception {
			Object instance = mPool.borrowObject();
			return (LmtpClient) instance;
		}

		public void releaseClient(LmtpClient client) throws Exception {
			mPool.returnObject(client);
		}
	}
}
