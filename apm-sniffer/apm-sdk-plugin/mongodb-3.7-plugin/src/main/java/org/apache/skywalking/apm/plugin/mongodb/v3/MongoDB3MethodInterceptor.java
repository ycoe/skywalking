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


package org.apache.skywalking.apm.plugin.mongodb.v3;

import com.mongodb.MongoClient;
import com.mongodb.MongoNamespace;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.connection.Cluster;
import com.mongodb.connection.ServerDescription;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.lang.reflect.Method;

/**
 * {@link MongoDB3MethodInterceptor} intercept method of {@link com.mongodb.MongoClient} all methods. record the mongoDB host, operation name and the key of the
 * operation.
 *
 * @author xuwenzhen
 */
public class MongoDB3MethodInterceptor implements InstanceMethodsAroundInterceptor, InstanceConstructorInterceptor {

    private static final String DB_TYPE = "MongoDB";

    private static final String MONGO_DB_OP_PREFIX = "MongoDB/";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        Object[] arguments = allArguments;
        String methodName = method.getName();

        String remotePeer = MongoClientInterceptor.getConnectPoint();
        AbstractSpan span;
        ContextCarrier carrier = new ContextCarrier();
        if (remotePeer == null) {
            remotePeer = "N/A";
        }
        span = ContextManager.createExitSpan(MONGO_DB_OP_PREFIX + methodName, carrier, remotePeer);
        span.setComponent(ComponentsDefine.MONGO_DRIVER);
        Tags.DB_TYPE.set(span, DB_TYPE);
        SpanLayer.asDB(span);
        MongoNamespace nameSpace = ((MongoCollection) objInst).getNamespace();

        String paramStr = getFindParams(arguments);
        Tags.DB_STATEMENT.set(span, nameSpace.getDatabaseName() + "." + nameSpace + "." + methodName + "(" + paramStr + ")");
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ContextManager.stopSpan();
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
        Cluster cluster = (Cluster) allArguments[0];
        StringBuilder peers = new StringBuilder();
        for (ServerDescription description : cluster.getDescription().getServerDescriptions()) {
            ServerAddress address = description.getAddress();
            peers.append(address.getHost() + ":" + address.getPort() + ";");
        }

        objInst.setSkyWalkingDynamicField(peers.subSequence(0, peers.length() - 1).toString());
    }

    private String getFindParams(Object[] arguments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            Object argument = arguments[i];
            if (argument == null) {
                sb.append("null");
                continue;
            }

            if (argument instanceof Document) {
                sb.append(((Document) argument).toJson());
            } else if (argument instanceof Bson) {
                BsonDocument asBsonDocument = ((Bson) argument).toBsonDocument(BsonDocument.class, MongoClient.getDefaultCodecRegistry());
                sb.append(asBsonDocument.toJson());
            } else if (argument instanceof Class) {
                sb.append(((Class) argument).getName()).append(".class");
            } else {
                sb.append(argument.toString());
            }
        }
        return sb.toString();
    }
}
