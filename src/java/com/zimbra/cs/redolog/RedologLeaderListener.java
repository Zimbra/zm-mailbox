package com.zimbra.cs.redolog;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.StringUtils;

import com.zimbra.common.consul.CatalogRegistration.Service;
import com.zimbra.common.consul.ConsulClient;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.servicelocator.ServiceLocator;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.redolog.LeaderChangeListener.LeaderStateChange;
import com.zimbra.cs.util.Zimbra;

public class RedologLeaderListener {

    protected ConsulClient consulClient;
    protected ServiceLocator serviceLocator;

    private final Service consulService;
    private volatile String consulSessionId;

    private volatile String leaderSessionId;
    private volatile boolean isLeader;

    private volatile boolean stopped;

    public RedologLeaderListener(String consulSessionId, Service consulService) {
        super();
        this.consulSessionId = consulSessionId;
        this.consulService = consulService;
    }

    public String getLeaderSessionId() {
        return leaderSessionId;
    }

    private void setLeaderSessionId(String leaderSessionId) {
        this.leaderSessionId = leaderSessionId;
    }

    public boolean isLeader() {
        return isLeader;
    }

    private void setLeader(boolean isLeader) {
        this.isLeader = isLeader;
    }

    public String getConsulSessionId() {
        return consulSessionId;
    }

    public void setConsulSessionId(String sessionId) {
        consulSessionId = sessionId;
    }

    private Service getConsulService() {
        return consulService;
    }

    private List<LeaderChangeListener> listeners = new CopyOnWriteArrayList<LeaderChangeListener>();

    public void addListener(LeaderChangeListener listener) {
        listeners.add(listener);
    }

    private List<LeaderChangeListener> getListeners() {
        return listeners;
    }

    public void stop() {
        stopped = true;
    }

    private static final long MAX_BACKOFF = DebugConfig.redologListenerBackoffMax;
    private static final long MIN_BACKOFF = DebugConfig.redologListenerBackoffMin;

    public void init() {
        consulClient = Zimbra.getAppContext().getBean(ConsulClient.class);
        serviceLocator = Zimbra.getAppContext().getBean(ServiceLocator.class);

        Thread t = new Thread("redolog-leader-listener") {
            @Override
            public void run() {
                String lastExceptionMsg = null;
                long backoffTime = MIN_BACKOFF;
                while (!stopped) {
                    try {
                        String sessionIdBefore = getLeaderSessionId();
                        setLeaderSessionId(consulClient.waitForLeaderChange(getConsulService(), sessionIdBefore));
                        String newSessionId = getLeaderSessionId();
                        if (!StringUtils.equals(sessionIdBefore, newSessionId)) {
                            LeaderStateChange stateChange = LeaderStateChange.NO_CHANGE;
                            //changed
                            ZimbraLog.redolog.info("leader session changed from [" + sessionIdBefore + "] to [" + newSessionId + "]");
                            String consulSessionId = getConsulSessionId();
                            if (consulSessionId != null && StringUtils.equals(consulSessionId, newSessionId)) {
                                stateChange = LeaderStateChange.GAINED_LEADERSHIP;
                                setLeader(true);
                            } else {
                                if (consulSessionId != null && StringUtils.equals(consulSessionId, sessionIdBefore)) {
                                    stateChange = LeaderStateChange.LOST_LEADERSHIP;
                                }
                                setLeader(false);
                            }
                            if (!stopped) {
                                for (LeaderChangeListener listener : getListeners()) {
                                    listener.onLeaderChange(newSessionId, stateChange);
                                }
                            }
                        }
                        backoffTime = MIN_BACKOFF;
                    } catch (Exception e) {
                        //only log warning for new exceptions, to avoid flooding log
                        if ((lastExceptionMsg == null && e.getMessage() != null) || (lastExceptionMsg != null && !lastExceptionMsg.equals(e.getMessage()))) {
                            ZimbraLog.redolog.warn("Exception waiting for leader change", e);
                            lastExceptionMsg = e.getMessage();
                        } else {
                            ZimbraLog.redolog.debug("Exception waiting for leader change", e);
                        }
                        //pause before retry
                        try {
                            ZimbraLog.redolog.debug("pausing %dms before retry", backoffTime);
                            Thread.sleep(backoffTime);
                            if (backoffTime < MAX_BACKOFF) {
                                backoffTime = Math.min(MAX_BACKOFF, backoffTime * 2);
                            }
                        } catch (InterruptedException ie) {
                        }
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }
}
