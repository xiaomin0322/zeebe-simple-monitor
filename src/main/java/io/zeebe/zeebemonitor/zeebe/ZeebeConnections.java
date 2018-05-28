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

import java.util.Iterator;

import javax.annotation.PostConstruct;

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.events.IncidentEvent;
import io.zeebe.client.api.events.WorkflowInstanceEvent;
import io.zeebe.client.api.record.Record;
import io.zeebe.client.api.record.RecordMetadata;
import io.zeebe.zeebemonitor.entity.*;
import io.zeebe.zeebemonitor.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

@Component
@ApplicationScope
public class ZeebeConnections
{
    @Autowired
    private WorkflowDefinitionRepository workflowDefinitionRepository;
    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;
    @Autowired
    private IncidentRepository incidentRepository;
    @Autowired
    private RecordLogRepository loggedEventRepository;
    @Autowired
    private ConfigurationRepository configurationRepository;

    private ZeebeClient client;

    @PostConstruct
    public void initConnection()
    {
        final Iterable<Configuration> configs = configurationRepository.findAll();
        final Iterator<Configuration> configIterator = configs.iterator();
        if (configIterator.hasNext())
        {
            final Configuration conf = configIterator.next();

            connect(conf);
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
                client.newTopologyRequest()
                    .send()
                    .join();

                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }

    public boolean connect(final Configuration conf)
    {
        this.client = ZeebeClient
                .newClientBuilder()
                .brokerContactPoint(conf.getConnectionString())
                .build();

        if (!isConnected())
        {
            return false;
        }

        final String subscriptionName = "zsm-" + conf.getClientId();

        client.topicClient()
              .newSubscription()
              .name(subscriptionName)
              .incidentEventHandler((event) ->
              {
                  switch (event.getState())
                  {
                      case CREATED:
                          workflowInstanceIncidentOccured(event);
                          break;

                      case RESOLVE_FAILED:
                          workflowInstanceIncidentUpdated(event);
                          break;

                      case RESOLVED:
                      case DELETED:
                          workflowInstanceIncidentResolved(event);
                          break;

                      default:
                          break;
                  }

                  insertRecord(event);
              })
              .workflowInstanceEventHandler((event) ->
              {
                  switch (event.getState())
                  {
                      case CREATED:
                          workflowInstanceStarted(WorkflowInstance.from(event));
                          break;

                      case COMPLETED:
                          workflowInstanceEnded(event);
                          break;

                      case CANCELED:
                          workflowInstanceCanceled(event);
                          break;

                      case ACTIVITY_ACTIVATED:
                          workflowInstanceActivityStarted(event);
                          break;

                      case ACTIVITY_READY:
                      case ACTIVITY_COMPLETING:
                          workflowInstanceUpdated(event);
                          break;

                      case ACTIVITY_COMPLETED:
                      case ACTIVITY_TERMINATED:
                      case GATEWAY_ACTIVATED:
                      case START_EVENT_OCCURRED:
                      case END_EVENT_OCCURRED:
                          workflowInstanceActivityEnded(event);
                          break;

                      case SEQUENCE_FLOW_TAKEN:
                          sequenceFlowTaken(event);
                          break;

                      case PAYLOAD_UPDATED:
                          workflowInstancePayloadUpdated(event);
                          break;

                      default:
                          break;
                  }

                  insertRecord(event);
              })
              .recordHandler(this::insertRecord)
              .startAtHeadOfTopic()
              .forcedStart()
              .open();

        return true;
    }

    private void insertRecord(Record record)
    {
        final RecordMetadata metadata = record.getMetadata();

        loggedEventRepository.save(new RecordLog(metadata.getPartitionId(), metadata.getPosition(), record.toJson()));
    }

    private void workflowInstanceStarted(WorkflowInstance instance)
    {
        workflowInstanceRepository.save(instance);
    }

    private void workflowInstanceEnded(WorkflowInstanceEvent event)
    {
        final WorkflowInstance instance = workflowInstanceRepository.findByWorkflowInstanceKeyAndPartitionId(event.getWorkflowInstanceKey(), event.getMetadata().getPartitionId());

        instance.setEnded(true);

        workflowInstanceRepository.save(instance);
    }

    private void workflowInstanceActivityStarted(WorkflowInstanceEvent event)
    {
        final WorkflowInstance instance = workflowInstanceRepository.findByWorkflowInstanceKeyAndPartitionId(event.getWorkflowInstanceKey(), event.getMetadata().getPartitionId());

        instance.activityStarted(event.getActivityId(), event.getPayload())
                .setLastEventPosition(event.getMetadata().getPosition());

        workflowInstanceRepository.save(instance);
    }

    private void workflowInstanceActivityEnded(WorkflowInstanceEvent event)
    {
        final WorkflowInstance instance = workflowInstanceRepository.findByWorkflowInstanceKeyAndPartitionId(event.getWorkflowInstanceKey(), event.getMetadata().getPartitionId());

        instance.activityEnded(event.getActivityId(), event.getPayload())
                .setLastEventPosition(event.getMetadata().getPosition());

        workflowInstanceRepository.save(instance);
    }

    private void workflowInstanceCanceled(WorkflowInstanceEvent event)
    {
        final WorkflowInstance instance = workflowInstanceRepository.findByWorkflowInstanceKeyAndPartitionId(event.getWorkflowInstanceKey(), event.getMetadata().getPartitionId());

        instance.setEnded(true);

        workflowInstanceRepository.save(instance);
    }

    private void workflowInstancePayloadUpdated(WorkflowInstanceEvent event)
    {
        final WorkflowInstance instance = workflowInstanceRepository.findByWorkflowInstanceKeyAndPartitionId(event.getWorkflowInstanceKey(), event.getMetadata().getPartitionId());

        instance.setPayload(event.getPayload());

        workflowInstanceRepository.save(instance);
    }

    private void workflowInstanceUpdated(WorkflowInstanceEvent event)
    {
        final WorkflowInstance instance = workflowInstanceRepository.findByWorkflowInstanceKeyAndPartitionId(event.getWorkflowInstanceKey(), event.getMetadata().getPartitionId());

        instance.setPayload(event.getPayload())
                .setLastEventPosition(event.getMetadata().getPosition());

        workflowInstanceRepository.save(instance);
    }

    private void workflowInstanceIncidentOccured(IncidentEvent event)
    {
        final Incident incident = new Incident(event.getMetadata().getKey(), event.getActivityId(), event.getErrorType(), event.getErrorMessage());

        incidentRepository.save(incident);

        final WorkflowInstance instance = workflowInstanceRepository.findByWorkflowInstanceKeyAndPartitionId(event.getWorkflowInstanceKey(), event.getMetadata().getPartitionId());

        instance.incidentOccured(incident);

        workflowInstanceRepository.save(instance);
    }

    private void workflowInstanceIncidentUpdated(IncidentEvent event)
    {
        final Incident incident = incidentRepository.findOne(event.getMetadata().getKey());

        if (incident != null)
        {
            incidentRepository.save(//
                    incident.setErrorType(event.getErrorType()).setErrorMessage(event.getErrorMessage()));
        }
    }

    private void workflowInstanceIncidentResolved(IncidentEvent event)
    {
        final Incident incident = incidentRepository.findOne(event.getMetadata().getKey());

        final WorkflowInstance instance = workflowInstanceRepository.findByWorkflowInstanceKeyAndPartitionId(event.getWorkflowInstanceKey(), event.getMetadata().getPartitionId());

        instance.incidentResolved(incident);

        workflowInstanceRepository.save(instance);

        incidentRepository.delete(incident);
    }

    private void sequenceFlowTaken(WorkflowInstanceEvent event)
    {
        final WorkflowInstance instance = workflowInstanceRepository.findByWorkflowInstanceKeyAndPartitionId(event.getWorkflowInstanceKey(), event.getMetadata().getPartitionId());

        instance.sequenceFlowTaken(event.getActivityId());

        workflowInstanceRepository.save(instance);
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

    public void disconnect()
    {
        client.close();
    }

    public void deleteAllData()
    {
        workflowInstanceRepository.deleteAll();
        workflowDefinitionRepository.deleteAll();
        incidentRepository.deleteAll();
        loggedEventRepository.deleteAll();
        configurationRepository.deleteAll();
    }

}
