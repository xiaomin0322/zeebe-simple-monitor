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
package io.zeebe.zeebemonitor.entity;

import java.util.UUID;

import javax.persistence.*;

import io.zeebe.client.api.commands.WorkflowResource;

@Entity
public class WorkflowDefinition
{

    /**
     * Generate random uuid for unique identification of workflow definition
     */
    @Id
    private String uuid = UUID.randomUUID().toString();

    private long workflowKey;

    private String bpmnProcessId;

    private int version;

    @OneToOne
    private Broker broker;

    @Column(length = 100000)
    private String resource;

    @Transient
    private long countRunning;

    @Transient
    private long countEnded;

    public static WorkflowDefinition from(WorkflowResource workflowResource)
    {
        final WorkflowDefinition dto = new WorkflowDefinition();

        dto.setWorkflowKey(workflowResource.getWorkflowKey());
        dto.setVersion(workflowResource.getVersion());
        dto.setBpmnProcessId(workflowResource.getBpmnProcessId());
        dto.setResource(workflowResource.getBpmnXml());

        return dto;
    }

    public String getResource()
    {
        return resource;
    }

    public void setResource(String resource)
    {
        this.resource = resource;
    }

    public String getBpmnProcessId()
    {
        return bpmnProcessId;
    }

    public void setBpmnProcessId(String bpmnProcessId)
    {
        this.bpmnProcessId = bpmnProcessId;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public Broker getBroker()
    {
        return broker;
    }

    public void setBroker(Broker broker)
    {
        this.broker = broker;
    }

    public long getCountRunning()
    {
        return countRunning;
    }

    public void setCountRunning(long countRunning)
    {
        this.countRunning = countRunning;
    }

    public long getCountEnded()
    {
        return countEnded;
    }

    public void setCountEnded(long countEnded)
    {
        this.countEnded = countEnded;
    }

    @Override
    public String toString()
    {
        return "WorkflowDefinitionDto [key=" + bpmnProcessId + ", broker=" + broker + ", version=" + version + ", countRunning=" + countRunning +
                ", countEnded=" + countEnded + "]";
    }

    public String getUuid()
    {
        return uuid;
    }

    public long getWorkflowKey()
    {
        return workflowKey;
    }

    public void setWorkflowKey(long workflowKey)
    {
        this.workflowKey = workflowKey;
    }

}
