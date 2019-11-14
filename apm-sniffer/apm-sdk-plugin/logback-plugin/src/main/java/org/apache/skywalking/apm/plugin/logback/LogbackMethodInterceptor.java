/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package org.apache.skywalking.apm.plugin.logback;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.Maps;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.StringTag;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.util.ThrowableTransformer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

/**
 * {@link LogbackMethodInterceptor} intercept method of {@link Logger} all methods. record the mongoDB host, operation name and the key of the
 * operation.
 *
 * @author xuwenzhen
 */
public class LogbackMethodInterceptor implements InstanceMethodsAroundInterceptor, InstanceConstructorInterceptor {

    private static final StringTag LOG_TAG = new StringTag(100, "log");

    private static final String LOGBACK_OP_PREFIX = "log/";
    private static final String STR_ERROR = "error";
    private static final String BOOLEAN_TRUE = "true";
    private static final String STR_INFO = "info";
    private static final String STR_LOGGING_INFO = "logging-info";

    private Boolean logInfoLevel;

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        String methodName = method.getName();

        if (logInfoLevel == null) {
            String loggingInfo = (String) Config.Plugin.Customize.CONTEXT.get(STR_LOGGING_INFO);
            logInfoLevel = BOOLEAN_TRUE.equalsIgnoreCase(loggingInfo);
        }
        if (methodName.equals(STR_INFO) && !logInfoLevel) {
            return;
        }

        org.slf4j.Logger logger = null;
        if (org.slf4j.Logger.class.isInstance(objInst)) {
            logger = (org.slf4j.Logger) objInst;
        }
        String logName = logger == null ? "logback" : logger.getName();

        AbstractSpan span = ContextManager.createLocalSpan(LOGBACK_OP_PREFIX + methodName + ":" + logName);

        if (STR_ERROR.equals(methodName)) {
            // error log
            span.errorOccurred();
        }

        long now = System.currentTimeMillis();
        Map<String, Object> logData = Maps.newHashMap();
        logData.put("threadName", Thread.currentThread().getName());
        logData.put("event", methodName);
        logData.put("logName", logName);
        logData.put("message", getParamStr(allArguments));
        for (Object argument : allArguments) {
            if (Throwable.class.isInstance(argument)) {
                logData.put("stack", ThrowableTransformer.INSTANCE.convert2String((Throwable) argument, 8000));
            }
        }

        span.log(now, logData);
        LOG_TAG.set(span, logName);
    }

    private String getParamStr(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return null;
        }
        if (arguments.length == 1 && arguments[0] != null) {
            return arguments[0].toString();
        }
        String baseMessage = (String) arguments[0];
        if (baseMessage == null) {
            return null;
        }
        Object arg1 = arguments[1];
        Object[] args;
        if (arg1.getClass().isArray()) {
            args = (Object[]) arg1;
        } else {
            args = Arrays.copyOfRange(arguments, 1, arguments.length);
        }

        StringBuilder sb = new StringBuilder();
        int length = baseMessage.length();
        int argIndex = 0;
        for (int i = 0; i < length; i++) {
            char c = baseMessage.charAt(i);
            if (c == '{' && i < length - 1 && baseMessage.charAt(i + 1) == '}') {
                if (args.length > argIndex) {
                    sb.append(args[argIndex]);
                }
                ++argIndex;
                ++i;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        if (ContextManager.isActive()) {
            ContextManager.stopSpan();
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan activeSpan = ContextManager.activeSpan();
        activeSpan.errorOccurred();
        activeSpan.log(t);
    }

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        objInst.setSkyWalkingDynamicField("local");
    }
}
