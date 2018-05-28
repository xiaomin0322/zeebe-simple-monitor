package io.zeebe.zeebemonitor.rest;

import io.zeebe.zeebemonitor.entity.WorkflowEntity;

public class WorkflowDto
{
    private String topic;

    private long workflowKey;
    private String bpmnProcessId;
    private int version;
    private String resource;

    private long countRunning;
    private long countEnded;

    public static WorkflowDto from(WorkflowEntity entity, long countRunning, long countEnded)
    {
        final WorkflowDto dto = new WorkflowDto();

        dto.topic = entity.getTopic();
        dto.workflowKey = entity.getWorkflowKey();
        dto.bpmnProcessId = entity.getBpmnProcessId();
        dto.version = entity.getVersion();
        dto.resource = entity.getResource();

        dto.countRunning = countRunning;
        dto.countEnded = countEnded;

        return dto;
    }

    public String getTopic()
    {
        return topic;
    }

    public void setTopic(String topic)
    {
        this.topic = topic;
    }

    public long getWorkflowKey()
    {
        return workflowKey;
    }

    public void setWorkflowKey(long workflowKey)
    {
        this.workflowKey = workflowKey;
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

    public String getResource()
    {
        return resource;
    }

    public void setResource(String resource)
    {
        this.resource = resource;
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

}
