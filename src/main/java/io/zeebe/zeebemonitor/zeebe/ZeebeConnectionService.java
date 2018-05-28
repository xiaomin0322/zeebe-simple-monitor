/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.zeebemonitor.zeebe;

import io.zeebe.client.ZeebeClient;
import io.zeebe.zeebemonitor.entity.ConfigurationEntity;
import io.zeebe.zeebemonitor.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ZeebeConnectionService
{
    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private ZeebeSubscriber subscriber;

    private ZeebeClient client;

    public void connect(final ConfigurationEntity config)
    {
        this.client = ZeebeClient
                .newClientBuilder()
                .brokerContactPoint(config.getConnectionString())
                .build();

        if (isConnected())
        {
            subscriber.openSubscription(client);
        }
    }

    public ZeebeClient getClient()
    {
        if (client != null)
        {
            return client;
        }
        else
        {
            throw new RuntimeException("Monitor is not connected");
        }
    }

    public boolean isConnected()
    {
        if (client == null)
        {
            return false;
        }
        else
        {
            // send request to check if connected or not
            try
            {
                client.newTopologyRequest().send().join();

                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }

    public void disconnect()
    {
        client.close();
    }

    public void deleteAllData()
    {
        workflowInstanceRepository.deleteAll();
        workflowRepository.deleteAll();
        incidentRepository.deleteAll();
        recordRepository.deleteAll();
        configurationRepository.deleteAll();
    }

}
