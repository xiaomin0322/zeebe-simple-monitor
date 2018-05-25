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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.zeebe.client.api.events.WorkflowInstanceEvent;
import io.zeebe.client.api.subscription.TopicSubscription;
import io.zeebe.zeebemonitor.entity.WorkflowInstance;
import io.zeebe.zeebemonitor.repository.WorkflowInstanceRepository;
import io.zeebe.zeebemonitor.zeebe.ZeebeConnections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instance")
public class WorkflowInstanceResource
{

    @Autowired
    private ZeebeConnections connections;

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    @RequestMapping("/")
    public Iterable<WorkflowInstance> getWorkflowInstances()
    {
        return workflowInstanceRepository.findAll();
    }

    @RequestMapping(path = "/{id}", method = RequestMethod.DELETE)
    public void cancelWorkflowInstance(@PathVariable("id") String id) throws Exception
    {

        final WorkflowInstance workflowInstance = workflowInstanceRepository.findOne(id);
        if (workflowInstance != null)
        {
            final WorkflowInstanceEvent event = findLastWorkflowInstanceEvent(workflowInstance);

            connections
                .getClient()
                .topicClient()
                .workflowClient()
                .newCancelInstanceCommand(event)
                .send()
                .join();
        }
    }

    @RequestMapping(path = "/{id}/update-payload", method = RequestMethod.PUT)
    public void updatePayload(@PathVariable("id") String id, @RequestBody String payload) throws Exception
    {

        final WorkflowInstance workflowInstance = workflowInstanceRepository.findOne(id);
        if (workflowInstance != null)
        {
            final WorkflowInstanceEvent event = findLastWorkflowInstanceEvent(workflowInstance);

            connections
                .getClient()
                .topicClient()
                .workflowClient()
                .newUpdatePayloadCommand(event)
                .payload(payload)
                .send()
                .join();
        }
    }

    private WorkflowInstanceEvent findLastWorkflowInstanceEvent(WorkflowInstance workflowInstance) throws Exception
    {
        final CompletableFuture<WorkflowInstanceEvent> future = new CompletableFuture<>();

        TopicSubscription subscription = null;
        try
        {
            final long position = workflowInstance.getLastEventPosition();

            subscription = connections
                    .getClient()
                    .topicClient()
                     .newSubscription()
                     .name("wf-instance-lookup")
                     .workflowInstanceEventHandler(wfEvent ->
                     {
                         if (wfEvent.getMetadata().getPosition() == position)
                         {
                             future.complete(wfEvent);
                         }
                     })
                     .startAtPosition(workflowInstance.getPartitionId(), position - 1)
                     .forcedStart()
                     .open();

            return future.get(10, TimeUnit.SECONDS);
        }
        finally
        {
            if (subscription != null)
            {
                subscription.close();
            }
        }
    }

}
