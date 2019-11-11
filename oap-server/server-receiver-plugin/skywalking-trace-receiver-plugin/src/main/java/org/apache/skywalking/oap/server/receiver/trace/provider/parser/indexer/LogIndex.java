package org.apache.skywalking.oap.server.receiver.trace.provider.parser.indexer;

import org.apache.skywalking.apm.network.common.KeyStringValuePair;
import org.apache.skywalking.apm.network.language.agent.v2.SpanObjectV2;
import org.apache.skywalking.oap.server.receiver.trace.provider.parser.decorator.SpanDecorator;

import java.util.List;

/**
 * @author xuwenzhen
 * @date 2019/11/8
 */
public class LogIndex implements Indexer {
    private static final String STR_TURN_LINE = "\n";
    private static final String LOG_PRE = "log/";
    private static final String STR_STACK = "stack";

    /**
     * need index
     *
     * @param spanDecorator span
     * @return
     */
    @Override
    public boolean match(SpanDecorator spanDecorator) {
        if (spanDecorator == null) {
            return false;
        }
        String operationName = spanDecorator.getOperationName();
        return operationName != null && operationName.startsWith(LOG_PRE);
    }

    /**
     * get index string
     *
     * @param stringBuilder
     * @param spanDecorator span
     * @return
     */
    @Override
    public void setIndexData(StringBuilder stringBuilder, SpanDecorator spanDecorator) {
        SpanObjectV2 spanObject = spanDecorator.getSpanObjectV2();
        if (spanObject == null) {
            return;
        }
//        this.getSpans(0).spanObjectV2.getLogs(0).getDataList()
        spanObject.getLogsList().forEach(log -> {
            List<KeyStringValuePair> dataList = log.getDataList();
            if (dataList == null || dataList.isEmpty()) {
                return;
            }
            dataList.forEach(keyStringValuePair -> {
                if (STR_STACK.equals(keyStringValuePair.getKey())) {
                    //日志内容
                    addStackIndex(stringBuilder, keyStringValuePair.getValue());
                } else {
                    addIndexStr(stringBuilder, keyStringValuePair.getValue());
                }
            });
        });

    }

    private void addStackIndex(StringBuilder stringBuilder, String stackStr) {
        if (stackStr == null || stackStr.length() == 0) {
            return;
        }

        int startIndex = 0;
        boolean addFirstCodeLine = true;
        for (int i = 0; i < stackStr.length(); i++) {
            char c = stackStr.charAt(i);
            if (c == '\n') {
                if (i == 0) {
                    continue;
                }

                addFirstCodeLine = addStackLineIndex(stringBuilder, stackStr, startIndex, i, addFirstCodeLine);
                startIndex = i + 1;
            }
        }
        if (startIndex != stackStr.length()) {
            addStackLineIndex(stringBuilder, stackStr, startIndex, stackStr.length(), addFirstCodeLine);
        }
    }

    private boolean addStackLineIndex(StringBuilder stringBuilder, String stackStr, int startIndex, int endIndex, boolean addFirstCodeLine) {
        boolean isCodeLine = isCodeLine(stackStr, startIndex, endIndex);
        if (!isCodeLine) {
            stringBuilder.append(STR_TURN_LINE).append(stackStr, startIndex, endIndex);
            return true;
        }
        if (addFirstCodeLine) {
            stringBuilder.append(STR_TURN_LINE).append(stackStr, startIndex, endIndex);
        }

        return false;
    }

    private boolean isCodeLine(String stackStr, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            char c = stackStr.charAt(i);
            if (c == ' ') {
                continue;
            }
            if (c == 'a' && i + 2 < endIndex && stackStr.charAt(i + 1) == 't' && stackStr.charAt(i + 2) == ' ') {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }


    private void addIndexStr(StringBuilder stringBuilder, String indexStr) {
        if (indexStr == null || indexStr.length() == 0) {
            return;
        }
        if (stringBuilder.length() > 0) {
            stringBuilder.append(STR_TURN_LINE);
        }
        stringBuilder.append(indexStr);
    }
}
