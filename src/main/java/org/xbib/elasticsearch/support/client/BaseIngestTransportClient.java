package org.xbib.elasticsearch.support.client;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public abstract class BaseIngestTransportClient extends BaseTransportClient
        implements Ingest {

    private final static ESLogger logger = ESLoggerFactory.getLogger(BaseIngestTransportClient.class.getSimpleName());

    public Ingest newClient(Settings settings) throws IOException {
        super.createClient(settings);
        return this;
    }

/*    @Override
    public BaseIngestTransportClient shards(int shards) {
        super.addSetting("index.number_of_shards", shards);
        return this;
    }

    @Override
    public BaseIngestTransportClient replica(int replica) {
        super.addSetting("index.number_of_replicas", replica);
        return this;
    }
    */

    @Override
    public BaseIngestTransportClient newIndex(String index) {
        return newIndex(index, null, null);
    }

    @Override
    public BaseIngestTransportClient newIndex(String index, String type, InputStream settings, InputStream mappings) throws IOException {
        configHelper.reset();
        configHelper.setting(settings);
        configHelper.mapping(type, mappings);
        return newIndex(index, configHelper.settings(), configHelper.mappings());
    }

    @Override
    public BaseIngestTransportClient newIndex(String index, Settings settings, Map<String,String> mappings) {
        if (client == null) {
            logger.warn("no client for create index");
            return this;
        }
        if (index == null) {
            logger.warn("no index name given to create index");
            return this;
        }
        CreateIndexRequestBuilder createIndexRequestBuilder =
                new CreateIndexRequestBuilder(client.admin().indices()).setIndex(index);
        if (settings != null) {
            logger.info("settings = {}", settings.getAsStructuredMap());
            createIndexRequestBuilder.setSettings(settings);
        }
        if (mappings != null ) {
            for (String type : mappings.keySet()) {
                logger.info("found mapping for {}", type);
                createIndexRequestBuilder.addMapping(type, mappings.get(type));
            }
        }
        createIndexRequestBuilder.execute().actionGet();
        logger.info("index {} created", index);
        return this;
    }

    @Override
    public BaseIngestTransportClient newMapping(String index, String type, Map<String,Object> mapping) {
        PutMappingRequestBuilder putMappingRequestBuilder =
                new PutMappingRequestBuilder(client.admin().indices())
                        .setIndices(index)
                        .setType(type)
                        .setSource(mapping);
        putMappingRequestBuilder.execute().actionGet();
        logger.info("mapping created for index {} and type {}", index, type);
        return this;
    }

    @Override
    public synchronized BaseIngestTransportClient deleteIndex(String index) {
        if (client == null) {
            logger.warn("no client for delete index");
            return this;
        }
        if (index == null) {
            logger.warn("no index name given to delete index");
            return this;
        }
        DeleteIndexRequestBuilder deleteIndexRequestBuilder =
                new DeleteIndexRequestBuilder(client.admin().indices(), index);
        deleteIndexRequestBuilder.execute().actionGet();
        return this;
    }

    public BaseIngestTransportClient putMapping(String index) {
        if (client == null) {
            logger.warn("no client for put mapping");
            return this;
        }
        configHelper.putMapping(client, index);
        return this;
    }

    public BaseIngestTransportClient deleteMapping(String index, String type) {
        if (client == null) {
            logger.warn("no client for delete mapping");
            return this;
        }
        configHelper.deleteMapping(client, index, type);
        return this;
    }

    @Override
    public BaseIngestTransportClient waitForCluster(ClusterHealthStatus status, TimeValue timeValue) throws IOException {
        ClientHelper.waitForCluster(client, status, timeValue);
        return this;
    }

}
