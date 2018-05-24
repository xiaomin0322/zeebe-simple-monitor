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
package io.zeebe.zeebemonitor.rest;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

import io.zeebe.client.api.clients.WorkflowClient;
import io.zeebe.client.api.commands.Workflow;
import io.zeebe.client.api.commands.WorkflowResource;
import io.zeebe.client.api.events.DeploymentEvent;
import io.zeebe.zeebemonitor.entity.*;
import io.zeebe.zeebemonitor.repository.WorkflowDefinitionRepository;
import io.zeebe.zeebemonitor.repository.WorkflowInstanceRepository;
import io.zeebe.zeebemonitor.zeebe.ZeebeConnections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/workflow-definition")
public class WorkflowDefinitionResource
{

    @Autowired
    private ZeebeConnections connections;

    @Autowired
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    @RequestMapping(path = "/{broker}")
    public Iterable<WorkflowDefinition> getWorkflowDefinitions(@PathVariable("broker") String broker)
    {
        final List<Workflow> deployedWorkflows = connections.getZeebeClient(broker)
            .topicClient()
            .workflowClient()
            .newWorkflowRequest()
            .send()
            .join()
            .getWorkflows();

        final List<Long> deployedWorkflowKeys = deployedWorkflows.stream().map(Workflow::getWorkflowKey).collect(Collectors.toList());

        final Iterable<WorkflowDefinition> availableWorkflowDefinitions = workflowDefinitionRepository.findAll();
        for (WorkflowDefinition workflowDefinition : availableWorkflowDefinitions)
        {
            deployedWorkflowKeys.remove(workflowDefinition.getWorkflowKey());
        }

        if (deployedWorkflows.isEmpty())
        {
            // up-to-date
            return fillWorkflowInstanceCount(availableWorkflowDefinitions);
        }
        else
        {
            // not up-to-date
            final List<WorkflowDefinition> workflowDefinitions = fetchWorkflowsByKeyAndInsert(deployedWorkflowKeys, broker);

            for (WorkflowDefinition def : availableWorkflowDefinitions)
            {
                workflowDefinitions.add(def);
            }

            return fillWorkflowInstanceCount(workflowDefinitions);
        }
    }

    private Iterable<WorkflowDefinition> fillWorkflowInstanceCount(Iterable<WorkflowDefinition> workflowDefinitions)
    {
        for (WorkflowDefinition workflowDefinition : workflowDefinitions)
        {
            fillWorkflowInstanceCount(workflowDefinition);
        }
        return workflowDefinitions;
    }

    private WorkflowDefinition fillWorkflowInstanceCount(WorkflowDefinition workflowDefinition)
    {
        workflowDefinition.setCountRunning(workflowInstanceRepository.countRunningInstances(workflowDefinition.getBpmnProcessId(), workflowDefinition.getVersion()));
        workflowDefinition.setCountEnded(workflowInstanceRepository.countEndedInstances(workflowDefinition.getBpmnProcessId(), workflowDefinition.getVersion()));
        return workflowDefinition;
    }

    @RequestMapping(path = "/{broker}/{key}/{version}")
    public WorkflowDefinition findWorkflowDefinition(@PathVariable("broker") String broker, @PathVariable("key") String key,
            @PathVariable("version") int version)
    {
        final WorkflowDefinition def = workflowDefinitionRepository.findByBrokerConnectionStringAndKeyAndVersion(broker, key, version);

        if (def != null)
        {
            return fillWorkflowInstanceCount(def);
        }
        else
        {
            // TODO request workflow by key
            final long workflowKey = 123L;
            final List<WorkflowDefinition> newDef = fetchWorkflowsByKeyAndInsert(Collections.singletonList(workflowKey), broker);

            return newDef.get(0);
        }
    }

    @RequestMapping(path = "/{broker}/{key}/{version}", method = RequestMethod.PUT)
    public void startWorkflowInstance(@PathVariable("broker") String brokerConnection, @PathVariable("key") String key, @PathVariable("version") int version,
            @RequestBody String payload)
    {

        connections
            .getZeebeClient(brokerConnection)
            .topicClient()
            .workflowClient()
            .newCreateInstanceCommand()
            .bpmnProcessId(key)
            .version(version)
            .payload(payload)
            .send()
            .join();
    }

    @RequestMapping(path = "/", method = RequestMethod.POST)
    public void uploadModel(@RequestBody DeploymentDto deployment) throws UnsupportedEncodingException
    {
        final WorkflowClient workflowClient = connections.getZeebeClient(deployment.getBroker()).topicClient().workflowClient();

        final List<Long> workflowKeys = new ArrayList<>();

        for (FileDto file : deployment.getFiles())
        {
            final DeploymentEvent deploymentEvent = workflowClient //
                .newDeployCommand()
                .addResourceBytes(file.getContent(), file.getFilename())
                .send()
                .join();

            final long workflowKey = deploymentEvent.getDeployedWorkflows().get(0).getWorkflowKey();
            workflowKeys.add(workflowKey);
        }

        final String broker = deployment.getBroker();

        fetchWorkflowsByKeyAndInsert(workflowKeys, broker);
    }

    private List<WorkflowDefinition> fetchWorkflowsByKeyAndInsert(final List<Long> workflowKeys, final String broker)
    {
        return workflowKeys.stream().map(workflowKey ->
        {
            final WorkflowResource resource = connections
                .getZeebeClient(broker)
                .topicClient()
                .workflowClient()
                .newResourceRequest()
                .workflowKey(workflowKey)
                .send()
                .join();

            final WorkflowDefinition entity = WorkflowDefinition.from(resource);
            workflowDefinitionRepository.save(entity);

            return entity;
        })
        .collect(Collectors.toList());
    }

}
