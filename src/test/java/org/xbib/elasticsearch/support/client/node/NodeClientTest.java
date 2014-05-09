
package org.xbib.elasticsearch.support.client.node;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.client.transport.NoNodeAvailableException;

import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.xbib.elasticsearch.support.helper.AbstractNodeRandomTestHelper;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class NodeClientTest extends AbstractNodeRandomTestHelper {

    private final static ESLogger logger = ESLoggerFactory.getLogger(NodeClientTest.class.getSimpleName());

    @Test
    public void testNewIndexNodeClient() throws Exception {
        final NodeClient es = new NodeClient()
                .flushInterval(TimeValue.timeValueSeconds(5))
                .newClient(client("1"))
                .newIndex("test");
        es.shutdown();
        if (es.hasThrowable()) {
            logger.error("error", es.getThrowable());
        }
        assertFalse(es.hasThrowable());
    }

    @Test
    public void testMappingNodeClient() throws Exception {
        final NodeClient es = new NodeClient()
                .flushInterval(TimeValue.timeValueSeconds(5))
                .newClient(client("1"));
        es.addMapping("test", "{\"test\":{\"properties\":{\"location\":{\"type\":\"geo_point\"}}}}");
        es.newIndex("test");

        GetMappingsRequest getMappingsRequest = new GetMappingsRequest()
                .indices("test");
        GetMappingsResponse getMappingsResponse = es.client().admin().indices().getMappings(getMappingsRequest).actionGet();

        logger.info("mappings={}", getMappingsResponse.getMappings());

        es.shutdown();
        if (es.hasThrowable()) {
            logger.error("error", es.getThrowable());
        }
        assertFalse(es.hasThrowable());
    }

    @Test
    public void testRandomDocsNodeClient() throws Exception {
        final NodeClient es = new NodeClient()
                .maxActionsPerBulkRequest(1000)
                .flushInterval(TimeValue.timeValueSeconds(10))
                .newClient(client("1"))
                .newIndex("test");

        try {
            for (int i = 0; i < 12345; i++) {
                es.index("test", "test", null, "{ \"name\" : \"" + randomString(32) + "\"}");
            }
            es.flush();
        } catch (NoNodeAvailableException e) {
            logger.warn("skipping, no node available");
        } finally {
            es.shutdown();
            assertEquals(13, es.getState().getTotalIngest().count());
            if (es.hasThrowable()) {
                logger.error("error", es.getThrowable());
            }
            assertFalse(es.hasThrowable());
        }
    }

    @Test
    public void testThreadedRandomDocsNodeClient() throws Exception {
        int max = Runtime.getRuntime().availableProcessors();
        int maxactions = 1000;
        final int maxloop = 12345;
        final NodeClient client = new NodeClient()
                .maxActionsPerBulkRequest(maxactions)
                .flushInterval(TimeValue.timeValueSeconds(600)) // disable auto flush for this test
                .newClient(client("1"))
                .newIndex("test")
                .startBulk("test");
        try {
            ThreadPoolExecutor pool = EsExecutors.newFixed(max, 30,
                    EsExecutors.daemonThreadFactory("nodeclient-test"));
            final CountDownLatch latch = new CountDownLatch(max);
            for (int i = 0; i < max; i++) {
                pool.execute(new Runnable() {
                    public void run() {
                        for (int i = 0; i < maxloop; i++) {
                            client.index("test", "test", null, "{ \"name\" : \"" + randomString(32) + "\"}");
                        }
                        latch.countDown();
                    }
                });
            }
            logger.info("waiting for max 60 seconds...");
            latch.await(60, TimeUnit.SECONDS);
            logger.info("flush...");
            client.flush();
            logger.info("waiting for pool shutdown...");
            pool.shutdown();
            logger.info("pool is shut down");
        } catch (NoNodeAvailableException e) {
            logger.warn("skipping, no node available");
        } finally {
            client.stopBulk("test").shutdown();
            logger.info("total bulk requests = {}", client.getState().getTotalIngest().count());
            assertEquals(max * maxloop / maxactions + 1, client.getState().getTotalIngest().count());
            if (client.hasThrowable()) {
                logger.error("error", client.getThrowable());
            }
            assertFalse(client.hasThrowable());
        }

    }

}
