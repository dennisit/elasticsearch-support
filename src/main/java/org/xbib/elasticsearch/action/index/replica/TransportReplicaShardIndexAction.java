package org.xbib.elasticsearch.action.index.replica;

import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.action.shard.ShardStateAction;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.routing.ShardIterator;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.mapper.SourceToParse;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.xbib.elasticsearch.action.index.IndexAction;
import org.xbib.elasticsearch.action.index.IndexRequest;
import org.xbib.elasticsearch.action.support.replication.replica.TransportReplicaShardOperationAction;

public class TransportReplicaShardIndexAction
        extends TransportReplicaShardOperationAction<IndexReplicaShardRequest, IndexReplicaShardResponse, TransportReplicaShardOperationAction.ReplicaOperationResponse> {

    @Inject
    public TransportReplicaShardIndexAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                             IndicesService indicesService, ThreadPool threadPool, ShardStateAction shardStateAction,
                                             ActionFilters actionFilters) {
        super(settings, IndexAction.NAME, transportService, clusterService, indicesService, threadPool, shardStateAction, actionFilters);
    }

    @Override
    protected IndexReplicaShardRequest newRequestInstance() {
        return new IndexReplicaShardRequest();
    }

    @Override
    protected IndexReplicaShardResponse newResponseInstance() {
        return new IndexReplicaShardResponse();
    }

    @Override
    protected String transportAction() {
        return IndexAction.NAME;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.INDEX;
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, IndexReplicaShardRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.WRITE);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, IndexReplicaShardRequest request) {
        return state.blocks().indexBlockedException(ClusterBlockLevel.WRITE, request.index());
    }

    @Override
    protected ShardIterator shards(ClusterState clusterState, IndexReplicaShardRequest request) {
        return clusterService.operationRouting()
                .indexShards(clusterService.state(), request.request().index(), request.request().type(),
                        request.request().id(), request.request().routing());
    }

    @Override
    protected ReplicaOperationResponse newReplicaResponseInstance() {
        return new ReplicaOperationResponse();
    }

    @Override
    protected IndexReplicaShardResponse shardOperationOnReplica(ReplicaOperationRequest shardRequest) {
        final long t0 = shardRequest.startTime();
        IndexShard indexShard = indicesService.indexServiceSafe(shardRequest.request().index()).shardSafe(shardRequest.shardId());
        IndexRequest request = shardRequest.request().request();
        SourceToParse sourceToParse = SourceToParse.source(SourceToParse.Origin.REPLICA, request.source()).type(request.type()).id(request.id())
                .routing(request.routing()).parent(request.parent()).timestamp(request.timestamp()).ttl(request.ttl());
        if (request.opType() == IndexRequest.OpType.INDEX) {
            Engine.Index index = indexShard.prepareIndex(sourceToParse, request.version(), request.versionType(), Engine.Operation.Origin.REPLICA, false);
            indexShard.index(index);
        } else {
            Engine.Create create = indexShard.prepareCreate(sourceToParse,
                    request.version(), request.versionType(), Engine.Operation.Origin.REPLICA, false, request.autoGeneratedId());
            indexShard.create(create);
        }
        return new IndexReplicaShardResponse(shardRequest.request().shardId(), shardRequest.replicaId(),
                System.currentTimeMillis() - t0);
    }
}
