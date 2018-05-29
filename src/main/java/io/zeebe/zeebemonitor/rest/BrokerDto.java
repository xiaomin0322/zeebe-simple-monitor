package io.zeebe.zeebemonitor.rest;

import java.util.ArrayList;
import java.util.List;

public class BrokerDto
{
    private String address;

    private List<PartitionDto> partitions = new ArrayList<>();

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public List<PartitionDto> getPartitions()
    {
        return partitions;
    }

    public void setPartitions(List<PartitionDto> partitions)
    {
        this.partitions = partitions;
    }

}
