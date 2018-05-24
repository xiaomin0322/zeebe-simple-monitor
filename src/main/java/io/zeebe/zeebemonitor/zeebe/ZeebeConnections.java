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

import java.util.*;

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.events.IncidentEvent;
import io.zeebe.client.api.events.WorkflowInstanceEvent;
import io.zeebe.zeebemonitor.Constants;
import io.zeebe.zeebemonitor.entity.*;
import io.zeebe.zeebemonitor.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

@Component
@ApplicationScope
public class ZeebeConnections
{

    private static final Set<String> ACTIVITY_END_STATES;

    static
    {
        ACTIVITY_END_STATES = new HashSet<>();
        ACTIVITY_END_STATES.add("ACTIVITY_COMPLETED");
        ACTIVITY_END_STATES.add("ACTIVITY_TERMINATED");

        ACTIVITY_END_STATES.add("GATEWAY_ACTIVATED");

        ACTIVITY_END_STATES.add("START_EVENT_OCCURRED");
        ACTIVITY_END_STATES.add("END_EVENT_OCCURRED");
    }

    @Autowired
    private WorkflowDefinitionRepository workflowDefinitionRepository;
    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;
    @Autowired
    private IncidentRepository incidentRepository;
    @Autowired
    private LoggedEventRepository loggedEventRepository;
    @Autowired
    private BrokerRepository brokerRepository;

    /**
     * broker connectionString -> ZeebeClirnt
     */
    private Map<String, ZeebeClient> openConnections = new HashMap<>();

    public ArrayList<ZeebeConnectionDto> getConnectionDtoList()
    {
        final Iterable<Broker> allBrokers = brokerRepository.findAll();
        final ArrayList<ZeebeConnectionDto> result = new ArrayList<>();
        for (Broker broker : allBrokers)
        {
            result.add(getConnectionDto(broker));
        }
        return result;
    }

    public ZeebeConnectionDto getConnectionDto(Broker broker)
    {
        return new ZeebeConnectionDto(broker, isConnected(broker));
    }

    public ZeebeClient connect(final Broker broker)
    {
        final ZeebeClient client = ZeebeClient
                .newClientBuilder()
                .brokerContactPoint(broker.getConnectionString())
                .build();

        ensureThatDefaultTopicExist(broker, client);

        openConnections.put(broker.getConnectionString(), client);

        // TODO: Think about the use case when connecting to various brokers on localhost
        final String clientName = "zeebe-simple-monitor";
        final String typedSubscriptionName = clientName + "-typed";
        final String untypedSubscriptionName = clientName + "-untyped";

        client.topicClient()
              .newSubscription()
              .name(typedSubscriptionName)
              .incidentEventHandler((event) ->
              {
                  switch (event.getState())
                  {
                      case CREATED:
                          workflowInstanceIncidentOccured(broker, event);
                          break;

                      case RESOLVE_FAILED:
                          workflowInstanceIncidentUpdated(broker, event);
                          break;

                      case RESOLVED:
                      case DELETED:
                          workflowInstanceIncidentResolved(broker, event);
                          break;

                      default:
                          break;
                  }
              })
              .workflowInstanceEventHandler((event) ->
              {
                  switch (event.getState())
                  {
                      case CREATED:
                          workflowInstanceStarted(broker, WorkflowInstance.from(event));
                          break;

                      case COMPLETED:
                          workflowInstanceEnded(broker, event.getWorkflowInstanceKey());
                          break;

                      case CANCELED:
                          workflowInstanceCanceled(broker, event.getWorkflowInstanceKey());
                          break;

                      case ACTIVITY_ACTIVATED:
                          workflowInstanceActivityStarted(broker, event);
                          break;

                      case ACTIVITY_READY:
                      case ACTIVITY_COMPLETING:
                          workflowInstanceUpdated(broker, event);
                          break;

                      case ACTIVITY_COMPLETED:
                      case ACTIVITY_TERMINATED:
                      case GATEWAY_ACTIVATED:
                      case START_EVENT_OCCURRED:
                      case END_EVENT_OCCURRED:
                          workflowInstanceActivityEnded(broker, event);
                          break;

                      case SEQUENCE_FLOW_TAKEN:
                          sequenceFlowTaken(broker, event);
                          break;

                      case PAYLOAD_UPDATED:
                          workflowInstancePayloadUpdated(broker, event);
                          break;

                      default:
                          break;
                  }
              })
              .startAtHeadOfTopic()
              .forcedStart()
              .open();

        client.topicClient().newSubscription().name(untypedSubscriptionName).recordHandler((record) ->
        {
            loggedEventRepository.save(new LoggedEvent(//
                    broker, //
                    record.getMetadata().getPartitionId(), //
                    record.getMetadata().getPosition(), //
                    record.getMetadata().getKey(), //
                    record.getMetadata().getValueType().name(), //
                    record.getMetadata().getIntent(),
                    record.toJson()));
        })
        .startAtHeadOfTopic()
        .forcedStart()
        .open();

        return client;
    }

    private void ensureThatDefaultTopicExist(final Broker broker, final ZeebeClient client)
    {
        final boolean hasDefaultTopic = client
                .newTopicsRequest()
                .send()
                .join()
                .getTopics()
                .stream()
                .anyMatch(t -> Constants.DEFAULT_TOPIC.equals(t.getName()));

        if (!hasDefaultTopic)
        {
            throw new RuntimeException(String.format("Missing required topic '%s' on broker '%s'", Constants.DEFAULT_TOPIC, broker.getConnectionString()));
        }
    }

    private void workflowDefinitionDeployed(Broker broker, WorkflowDefinition def)
    {
        def.setBroker(broker);
        workflowDefinitionRepository.save(def);
    }

    private void workflowInstanceStarted(Broker broker, WorkflowInstance instance)
    {
        instance.setBroker(broker);
        workflowInstanceRepository.save(instance);
    }

    private void workflowInstanceEnded(Broker broker, long workflowInstanceKey)
    {
        workflowInstanceRepository.save(workflowInstanceRepository.findOne(workflowInstanceKey) //
                                                                  .setEnded(true));
    }

    private void workflowInstanceActivityStarted(Broker broker, WorkflowInstanceEvent event)
    {
        workflowInstanceRepository.save(workflowInstanceRepository.findOne(event.getWorkflowInstanceKey()) //
                                                                  .activityStarted(event.getActivityId(), event.getPayload())
                                                                  .setLastEventPosition(event.getMetadata().getPosition()));
    }

    private void workflowInstanceActivityEnded(Broker broker, WorkflowInstanceEvent event)
    {
        workflowInstanceRepository.save(//
                workflowInstanceRepository.findOne(event.getWorkflowInstanceKey()) //
                                          .activityEnded(event.getActivityId(), event.getPayload()).setLastEventPosition(event.getMetadata().getPosition()));
    }

    private void workflowInstanceCanceled(Broker broker, long workflowInstanceKey)
    {
        workflowInstanceRepository.save(//
                workflowInstanceRepository.findOne(workflowInstanceKey) //
                                          .setEnded(true));
    }

    private void workflowInstancePayloadUpdated(Broker broker, WorkflowInstanceEvent event)
    {
        workflowInstanceRepository.save(//
                workflowInstanceRepository.findOne(event.getWorkflowInstanceKey()) //
                                          .setPayload(event.getPayload()));
    }

    private void workflowInstanceUpdated(Broker broker, WorkflowInstanceEvent event)
    {
        workflowInstanceRepository.save(//
                workflowInstanceRepository.findOne(event.getWorkflowInstanceKey()) //
                                          .setPayload(event.getPayload()).setLastEventPosition(event.getMetadata().getPosition()));
    }

    private void workflowInstanceIncidentOccured(Broker broker, IncidentEvent event)
    {
        final Incident incident = new Incident(event.getMetadata().getKey(), event.getActivityId(), event.getErrorType(), event.getErrorMessage());

        incidentRepository.save(incident);

        workflowInstanceRepository.save(//
                workflowInstanceRepository.findOne(event.getWorkflowInstanceKey()) //
                                          .incidentOccured(incident));
    }

    private void workflowInstanceIncidentUpdated(Broker broker, IncidentEvent event)
    {
        final Incident incident = incidentRepository.findOne(event.getMetadata().getKey());

        if (incident != null)
        {
            incidentRepository.save(//
                    incident.setErrorType(event.getErrorType()).setErrorMessage(event.getErrorMessage()));
        }
    }

    private void workflowInstanceIncidentResolved(Broker broker, IncidentEvent event)
    {
        final Incident incident = incidentRepository.findOne(event.getMetadata().getKey());

        workflowInstanceRepository.save(//
                workflowInstanceRepository.findOne(event.getWorkflowInstanceKey()) //
                                          .incidentResolved(incident));

        incidentRepository.delete(incident);
    }

    private void sequenceFlowTaken(Broker broker, WorkflowInstanceEvent event)
    {
        workflowInstanceRepository.save(//
                workflowInstanceRepository.findOne(event.getWorkflowInstanceKey()) //
                                          .sequenceFlowTaken(event.getActivityId()));
    }

    public void disconnect(Broker broker)
    {
        final ZeebeClient client = openConnections.get(broker.getConnectionString());

        client.close();

        openConnections.remove(broker.getConnectionString());
    }

    public void deleteAllData()
    {
        for (ZeebeClient client : openConnections.values())
        {
            client.close();
        }
        openConnections = new HashMap<>();

        workflowInstanceRepository.deleteAll();
        workflowDefinitionRepository.deleteAll();
        loggedEventRepository.deleteAll();
        brokerRepository.deleteAll();
    }

    public ZeebeClient getZeebeClient(String brokerConnectionString)
    {
        return openConnections.get(brokerConnectionString);
    }

    public ZeebeClient getZeebeClient(Broker broker)
    {
        return openConnections.get(broker.getConnectionString());
    }

    public boolean isConnected(Broker broker)
    {
        return openConnections.containsKey(broker.getConnectionString());
    }

}
