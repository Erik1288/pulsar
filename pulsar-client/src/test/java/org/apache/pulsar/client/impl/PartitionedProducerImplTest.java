/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.client.impl;

import io.netty.util.Timer;

import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.impl.conf.ClientConfigurationData;
import org.apache.pulsar.client.impl.conf.ProducerConfigurationData;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Unit Tests of {@link PartitionedProducerImpl}.
 */
public class PartitionedProducerImplTest {

    private static final String TOPIC_NAME = "testTopicName";
    private PulsarClientImpl client;
    private ProducerBuilderImpl producerBuilderImpl;
    private Schema schema;
    private ProducerInterceptors producerInterceptors;
    private CompletableFuture<Producer> producerCreatedFuture;

    @BeforeTest
    public void setup() {
        client = mock(PulsarClientImpl.class);
        schema = mock(Schema.class);
        producerInterceptors = mock(ProducerInterceptors.class);
        producerCreatedFuture = mock(CompletableFuture.class);
        ClientConfigurationData clientConfigurationData = mock(ClientConfigurationData.class);
        Timer timer = mock(Timer.class);

        producerBuilderImpl = new ProducerBuilderImpl(client, Schema.BYTES);

        when(client.getConfiguration()).thenReturn(clientConfigurationData);
        when(client.timer()).thenReturn(timer);
        when(client.newProducer()).thenReturn(producerBuilderImpl);
    }

    @Test
    public void testSinglePartitionMessageRouterImplInstance() throws NoSuchFieldException, IllegalAccessException {
        ProducerConfigurationData producerConfigurationData = new ProducerConfigurationData();
        producerConfigurationData.setMessageRoutingMode(MessageRoutingMode.SinglePartition);

        MessageRouter messageRouter = getMessageRouter(producerConfigurationData);
        assertTrue(messageRouter instanceof SinglePartitionMessageRouterImpl);
    }

    @Test
    public void testRoundRobinPartitionMessageRouterImplInstance() throws NoSuchFieldException, IllegalAccessException {
        ProducerConfigurationData producerConfigurationData = new ProducerConfigurationData();
        producerConfigurationData.setMessageRoutingMode(MessageRoutingMode.RoundRobinPartition);

        MessageRouter messageRouter = getMessageRouter(producerConfigurationData);
        assertTrue(messageRouter instanceof RoundRobinPartitionMessageRouterImpl);
    }

    @Test
    public void testCustomMessageRouterInstance() throws NoSuchFieldException, IllegalAccessException {
        ProducerConfigurationData producerConfigurationData = new ProducerConfigurationData();
        producerConfigurationData.setMessageRoutingMode(MessageRoutingMode.CustomPartition);
        producerConfigurationData.setCustomMessageRouter(new CustomMessageRouter());

        MessageRouter messageRouter = getMessageRouter(producerConfigurationData);
        assertTrue(messageRouter instanceof CustomMessageRouter);
    }

    private MessageRouter getMessageRouter(ProducerConfigurationData producerConfigurationData)
            throws NoSuchFieldException, IllegalAccessException {
        PartitionedProducerImpl impl = new PartitionedProducerImpl(
                client, TOPIC_NAME, producerConfigurationData,
                2, producerCreatedFuture, schema, producerInterceptors);

        Field routerPolicy = impl.getClass().getDeclaredField("routerPolicy");
        routerPolicy.setAccessible(true);
        MessageRouter messageRouter = (MessageRouter) routerPolicy.get(impl);
        assertNotNull(messageRouter);
        return messageRouter;
    }

    private class CustomMessageRouter implements MessageRouter {
        @Override
        public int choosePartition(Message<?> msg, TopicMetadata metadata) {
            int partitionIndex = Integer.parseInt(msg.getKey()) % metadata.numPartitions();
            return partitionIndex;
        }
    }

}
