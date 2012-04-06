/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index.global;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

/**
 * Global index sweeper cron job purges the orphan terms from global index table
 * @author smukhopadhyay
 *
 */
public class GlobalIndexSweeper {
    /** The frequency at which we sweep the global index table to purge orphan terms. */
    private static final long GLOBAL_INDEX_SWEEP_INTERVAL_MSEC = 24 * 60 * Constants.MILLIS_PER_MINUTE;
    
    GlobalIndex index;
    Timer timer;
    int[] mailboxIds;
    long elapseTimeMillis; //total amount of time it should be running
    int ref; //reference where we last ended in the maboxId array
    
    public GlobalIndexSweeper(GlobalIndex index) throws ServiceException {
        this.index = index;
        
        Pair<Long, Long> sweepTimePair = getSweepTime(LC.hbase_index_sweep_time.value());
        elapseTimeMillis = sweepTimePair.getSecond();
        timer = new Timer("Timer-GlobalIndex-Sweeper", true);
        mailboxIds = MailboxManager.getInstance().getMailboxIds();
        timer.schedule(new SweeperTask(),
                sweepTimePair.getFirst(),           //initial delay
                GLOBAL_INDEX_SWEEP_INTERVAL_MSEC);  //subsequent rate
    }
    
    class SweeperTask extends TimerTask {
        @Override
        public void run() {
            long finishTime = System.currentTimeMillis() + elapseTimeMillis;
            int count = 0; //number of mailboxes purged orphan terms during this iteration
            while (ref < mailboxIds.length) {
                //break if we are running for too long or, we have completed purging all the mailboxes!!
                if (System.currentTimeMillis() >= finishTime)
                    break;
                if (count == mailboxIds.length)
                    break;
                
                Mailbox mbox;
                try {
                    mbox = MailboxManager.getInstance().getMailboxById(mailboxIds[ref]);
                    index.purgeOrphanTerms(mbox.getAccountId());
                } catch (Throwable x) {
                    //Do nothing...
                    
                } finally {
                    count++;
                    if (ref >= mailboxIds.length - 1) {
                        ref = 0;
                        //refresh the mailboxIds after a full cycle!!
                        try {
                            mailboxIds = MailboxManager.getInstance().getMailboxIds();
                        } catch (Throwable x) {}
                    } else
                        ref++;
                }
            }
        }
    }
    
    @SuppressWarnings("deprecation")
    static final Pair<Long, Long> getSweepTime(String value) {
        DateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        
        Date startDate = null, endDate = null;
        try {
            String[] splits = value.split("-");
            startDate = sdf.parse(splits[0]);
            endDate = sdf.parse(splits[1]);
        } catch (Exception x) {}        
        
        Calendar c = Calendar.getInstance();
        long now = c.getTimeInMillis();
        
        if (startDate != null) {
            c.set(Calendar.HOUR_OF_DAY, startDate.getHours());
            c.set(Calendar.MINUTE, startDate.getMinutes());
            c.set(Calendar.SECOND, startDate.getSeconds());
            c.set(Calendar.MILLISECOND, 0);
        } else {
            //default is 00:00:00
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }
        
        long future = c.getTimeInMillis() - now;
        if (future < 0)
            future = future + Constants.MILLIS_PER_DAY;
        
        long elapseTime = 30 * Constants.MILLIS_PER_MINUTE; //default
        if (endDate != null && startDate != null && (endDate.getTime() - startDate.getTime() > 0)) {
            elapseTime = endDate.getTime() - startDate.getTime();
        }
        return new Pair<Long, Long>(future, elapseTime);
    }

}
