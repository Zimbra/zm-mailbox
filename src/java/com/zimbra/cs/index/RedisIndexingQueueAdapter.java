package com.zimbra.cs.index;

import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.util.ProvisioningUtil;

/**
 *
 * @author Greg Solovyev
 * Redis-based indexing queue implementation
 *
 */
public class RedisIndexingQueueAdapter implements IndexingQueueAdapter {
    private static String QUEUE_NAME = "zimbra_indexing_queue";
    private static String SUCCEEDED_SUFFIX = "_succeededIndexCounters";
    private static String TOTAL_SUFFIX = "_totalIndexCounters";
    private static String FAILED_SUFFIX = "_failedIndexCounters";
    private static String STATUS_SUFFIX = "_taskIndexStatus";
    private static String CONTROL_SET_NAME = "zimbra_indexing_counters";
    @Autowired protected Pool<Jedis> jedisPool;
    protected ObjectMapper objectMapper;

    public RedisIndexingQueueAdapter() {
        objectMapper = new ObjectMapper();
    }

    /**
     * Add an item to the tail of the queue. Redis does not have a blocking PUSH, therefore if Redis is OOMing
     * or list is already larger than 4B tasks, this method will log an error and return false.
     * This method is synchronized in order to reduce probability of exceeding the size of Redis backed queue
     * limited by zimbraIndexingQueueMaxSize
     * Polling interval that this method will use to check for free space in the underlying queue is configured by zimbraIndexingQueuePollingInterval
     *
     * @param {@link com.zimbra.cs.index.AbstractIndexingTasksLocator} item
     */
    @Override
    public synchronized boolean put(AbstractIndexingTasksLocator item) {
        try (Jedis jedis = jedisPool.getResource()) {
            while(jedis.llen(QUEUE_NAME) > ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexingQueueMaxSize, 10000)) {
                try {
                    /* wait until there is space in the queue
                     * Since we are not using transactions it is possible that queue will overflow by at most N-1
                     * where N is the number of mailstores within the cluster
                    */
                    Thread.sleep(ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexingQueuePollingInterval, 500));
                } catch (InterruptedException e) {
                    return false;
                }
            }
            jedis.rpush(QUEUE_NAME, objectMapper.writeValueAsString(item));
            return true;
       } catch (IOException e) {
            ZimbraLog.index.error("Failed in add an item into a redis based indexing queue for account %s",item.accountID);
       }
       return false;
    }

    /**
     * Add an item to the tail of the queue. If the underlying queue is full this call returns FALSE.
     * Maximum size of the underlying queue is configured by zimbraIndexingQueueMaxSize.
     * This method is synchronized in order to reduce probability of exceeding the size of underlying queue.
     *
     * @param {@link com.zimbra.cs.index.AbstractIndexingTasksLocator} item
     * @return TRUE if the item was successfully added/FALSE otherwise.
     */
    @Override
    public synchronized boolean add(AbstractIndexingTasksLocator item) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            if(jedis.llen(QUEUE_NAME) > ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexingQueueMaxSize, 10000)) {
                return false;
            }
            jedis.rpush(QUEUE_NAME, objectMapper.writeValueAsString(item));
            return true;
       } catch (IOException e) {
            ZimbraLog.index.error("Failed in add an item into a redis based indexing queue for account %s",item.accountID);
       }
       return false;
    }

    /**
     * Return the next element from the queue and remove it from the queue.
     * If no element is available, return null
     * @return {@link com.zimbra.cs.index.AbstractIndexingTasksLocator}
     */
    @Override
    public AbstractIndexingTasksLocator take()  {
        try (Jedis jedis = jedisPool.getResource()) {
            String data = jedis.lpop(QUEUE_NAME);
            if(data != null) {
                return objectMapper.readValue(data, AbstractIndexingTasksLocator.class);
            }
        } catch (IOException e) {
            ZimbraLog.index.error("Failed to take next indexing task from Redis backed indexing quue");
        }
        return null;
    }

    @Override
    public AbstractIndexingTasksLocator peek() {
        try (Jedis jedis = jedisPool.getResource()) {
            String data = jedis.lindex(QUEUE_NAME, 0);
            if(data != null) {
                return objectMapper.readValue(data, AbstractIndexingTasksLocator.class);
            }
        } catch (IOException e) {
            ZimbraLog.index.error("Failed to peek next indexing task from Redis backed indexing quue");
        }
        return null;
    }

    @Override
    public boolean hasMoreItems() {
        try (Jedis jedis = jedisPool.getResource()) {
            return (jedis.llen(QUEUE_NAME) > 0);
        }
    }

    @Override
    public void drain() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(QUEUE_NAME);
        }
    }

    private void incrementInt(String accountId, String suffix, int numItems) {
        try (Jedis jedis = jedisPool.getResource()) {
            String counterName = accountId.concat(suffix);
            jedis.sadd(CONTROL_SET_NAME, counterName);
            jedis.incrBy(counterName, numItems);
        }
    }

    @Override
    public void incrementSucceededMailboxTaskCount(String accountId, int numItems) {
        incrementInt(accountId, SUCCEEDED_SUFFIX, numItems);
    }

    @Override
    public void incrementFailedMailboxTaskCount(String accountId, int numItems) {
        incrementInt(accountId, FAILED_SUFFIX, numItems);
    }

    private int getInt(String accountId, String suffix) {
        try (Jedis jedis = jedisPool.getResource()) {
            String val = jedis.get(accountId.concat(suffix));
            if(val != null) {
                try {
                    return Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    ZimbraLog.index.error("Found a non-numeric value %s at key %s in Redis",val,accountId.concat(suffix));
                }
            }
            return 0;
        }
    }

    @Override
    public int getSucceededMailboxTaskCount(String accountId) {
        return getInt(accountId, SUCCEEDED_SUFFIX);
    }

    @Override
    public int getFailedMailboxTaskCount(String accountId) {
        return getInt(accountId, FAILED_SUFFIX);
    }

    @Override
    public int getTotalMailboxTaskCount(String accountId) {
        return getInt(accountId, TOTAL_SUFFIX);
    }

    @Override
    public void deleteMailboxTaskCounts(String accountId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(accountId.concat(TOTAL_SUFFIX), accountId.concat(SUCCEEDED_SUFFIX), accountId.concat(FAILED_SUFFIX));
        }
    }

    @Override
    public void clearAllTaskCounts() {
        try (Jedis jedis = jedisPool.getResource()) {
            String [] counterNames = jedis.smembers(CONTROL_SET_NAME).toArray(new String [0]);
            if(counterNames.length > 0) {
                jedis.del(counterNames);
                jedis.srem(CONTROL_SET_NAME, counterNames);
            }
        }
   }

    private void setCounter(String accountId, String suffix, int val) {
        try (Jedis jedis = jedisPool.getResource()) {
            String counterName = accountId.concat(suffix);
            jedis.sadd(CONTROL_SET_NAME, counterName);
            jedis.set(counterName, Integer.toString(val));
        }
    }

    @Override
    public void setTotalMailboxTaskCount(String accountId, int val) {
        setCounter(accountId, TOTAL_SUFFIX, val);
    }

    @Override
    public void setSucceededMailboxTaskCount(String accountId, int val) {
        setCounter(accountId, SUCCEEDED_SUFFIX, val);
    }

    @Override
    public void setFailedMailboxTaskCount(String accountId, int val) {
        setCounter(accountId, FAILED_SUFFIX, val);
    }

    @Override
    public int getTaskStatus(String accountId) {
        return getInt(accountId, STATUS_SUFFIX);
    }

    @Override
    public void setTaskStatus(String accountId, int status) {
        setCounter(accountId, STATUS_SUFFIX, status);
    }

}
