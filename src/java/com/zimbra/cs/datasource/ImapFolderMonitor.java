package com.zimbra.cs.datasource;

import com.zimbra.cs.mailclient.imap.ImapCapabilities;
import com.zimbra.cs.mailclient.imap.ResponseHandler;
import com.zimbra.cs.mailclient.imap.ImapResponse;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.account.DataSource;
import com.zimbra.common.service.ServiceException;

import java.util.TimerTask;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicLong;
import java.io.IOException;

/**
 * Monitor IMAP folder for newly arrived messages.
 */
public class ImapFolderMonitor {
    private final DataSource ds;
    private final String folderName;
    private final AtomicLong changeId = new AtomicLong();
    private ImapConnection connection;
    private TimerTask task;

    private static final Timer TIMER = new Timer("ImapFolderMonitor", true);
    private static final long PERIOD = 25 * 60 * 1000; // Restart IDLE every 25 minutes

    public ImapFolderMonitor(DataSource ds, String folderName) {
        this.ds = ds;
        this.folderName = folderName;
    }

    public synchronized void start() throws IOException, ServiceException {
        if (isRunning()) return;
        connect();
        if (!connection.hasCapability(ImapCapabilities.IDLE)) {
            disconnect();
            throw ServiceException.FAILURE("Connection must support IDLE", null);
        }
        task = new TimerTask() {
            public void run() {
                try {
                    restartIdle();
                } catch (Exception e) {
                    stop();
                }
            }
        };
        TIMER.schedule(task, 0, PERIOD);
    }

    public synchronized void stop() {
        if (isRunning()) {
            task.cancel();
            disconnect();
            task = null;
        }
    }

    private void disconnect() {
        try {
            connection.stopIdle();
            connection.close();
            connection = null;
        } catch (IOException e) {
            // Ignore
        }
    }

    private boolean isRunning() {
        return task != null;
    }

    public long getChangeId() {
        return changeId.get();
    }
    
    private synchronized void restartIdle() throws IOException, ServiceException {
        if (!isRunning()) return;
        try {
            connection.stopIdle();
        } catch (IOException e) {
            connect();
        }
        connection.select(folderName);
        changeId.incrementAndGet();
        connection.idle(new ResponseHandler() {
            public void handleResponse(ImapResponse res) {
                if (res.getCCode() == CAtom.EXISTS) {
                    changeId.incrementAndGet();
                }
            }
        });
    }

    private void connect() throws IOException, ServiceException {
        connection = new ImapConnection(ImapSync.getImapConfig(ds));
        connection.login(ds.getDecryptedPassword());
    }
}
