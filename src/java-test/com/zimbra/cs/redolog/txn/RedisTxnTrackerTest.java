/*
 * ***** BEGIN LICENSE BLOCK *****import java.util.HashMap;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.RedisOnLocalhostZimbraConfig;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;
CULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog.txn;

import java.util.HashMap;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.RedisOnLocalhostZimbraConfig;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;

public class RedisTxnTrackerTest extends AbstractTxnTrackerTest {

    @SuppressWarnings("unchecked")
    @Override
    public TxnTracker getTracker() {
        try {
            Pool<Jedis> pool = Zimbra.getAppContext().getBean(Pool.class);
            try (Jedis jedis = pool.getResource()) {
                jedis.ping();
            }
            return new RedisTxnTracker(pool);
        } catch (Exception e) {
            Assume.assumeNoException(e);
            return null;
        }
    }

    @Before
    public void cleanup() throws ServiceException {
        @SuppressWarnings("unchecked")
        Pool<Jedis> pool = Zimbra.getAppContext().getBean(Pool.class);
        try (Jedis jedis = pool.getResource()) {
            jedis.flushDB();
        }
    }

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class, "", RedisOnLocalhostZimbraConfig.class);
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret",
            new HashMap<String, Object>());
    }

}
