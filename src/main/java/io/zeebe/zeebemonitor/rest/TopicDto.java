package io.zeebe.zeebemonitor.rest;

public class TopicDto
{
    private String topicName;
    private int partitionCount;
    private int replicationFactor;

    public String getTopicName()
    {
        return topicName;
    }

    public void setTopicName(String topicName)
    {
        this.topicName = topicName;
    }

    public int getPartitionCount()
    {
        return partitionCount;
    }

    public void setPartitionCount(int partitionCount)
    {
        this.partitionCount = partitionCount;
    }

    public int getReplicationFactor()
    {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor)
    {
        this.replicationFactor = replicationFactor;
    }

}
