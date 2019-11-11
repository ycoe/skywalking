package org.apache.skywalking.oap.server.receiver.trace.provider.parser.indexer;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * @author xuwenzhen
 * @date 2019/11/8
 */
public class IndexerFactory {
    private static final List<Indexer> INDEX_LIST = Lists.newArrayList();

    public static List<Indexer> getIndexes() {
        if (INDEX_LIST.isEmpty()) {
            INDEX_LIST.add(new LogIndex());
        }
        return INDEX_LIST;
    }
}
