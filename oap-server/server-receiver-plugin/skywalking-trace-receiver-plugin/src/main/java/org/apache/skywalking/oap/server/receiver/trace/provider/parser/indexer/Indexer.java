package org.apache.skywalking.oap.server.receiver.trace.provider.parser.indexer;

import org.apache.skywalking.oap.server.receiver.trace.provider.parser.decorator.SpanDecorator;

/**
 *
 * @author xuwenzhen
 * @date 2019/11/8
 */
public interface Indexer {
    /**
     * need index
     * @param spanDecorator span
     * @return
     */
    boolean match(SpanDecorator spanDecorator);

    /**
     * set index string
     *
     * @param stringBuilder
     * @param spanDecorator span
     * @return
     */
    void setIndexData(StringBuilder stringBuilder, SpanDecorator spanDecorator);
}
