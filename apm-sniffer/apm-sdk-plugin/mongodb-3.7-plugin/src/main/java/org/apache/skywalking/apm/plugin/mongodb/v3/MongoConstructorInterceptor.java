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
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;

/**
 * @author xuwenzhen
 */
public class MongoConstructorInterceptor implements InstanceConstructorInterceptor {
    private static final ThreadLocal<String> THREAD_LOCAL_CONNECTPOINT = new ThreadLocal<String>();

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        String connectPoint = ((MongoClient) objInst).getConnectPoint();
        objInst.setSkyWalkingDynamicField(connectPoint);
        THREAD_LOCAL_CONNECTPOINT.set(connectPoint);
    }

    public static String getConnectPoint() {
        String connectPoint = THREAD_LOCAL_CONNECTPOINT.get();
        THREAD_LOCAL_CONNECTPOINT.remove();
        return connectPoint;
    }
}
