package io.zeebe.zeebemonitor.zeebe;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.zeebe.client.api.commands.Partition;
import io.zeebe.client.api.commands.Topic;
import io.zeebe.protocol.Protocol;
import io.zeebe.zeebemonitor.entity.PartitionEntity;
import io.zeebe.zeebemonitor.repository.PartitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class TopicService
{
    @Autowired
    private PartitionRepository partitionRepository;

    @Autowired
    private ZeebeConnectionService connectionService;

    @Autowired
    private ZeebeSubscriber subscriber;

    @Async
    public void synchronizeAsync()
    {
        synchronizeWithBroker();
    }

    public void synchronizeWithBroker()
    {
        final List<Topic> topics = connectionService
            .getClient()
            .newTopicsRequest()
            .send()
            .join()
            .getTopics();

        final List<Partition> partitions = topics
                .stream()
                .flatMap(t -> t.getPartitions().stream())
                .collect(Collectors.toList());

        final List<Integer> availablePartitions = new ArrayList<>();
        for (PartitionEntity partitionEntity : partitionRepository.findAll())
        {
            availablePartitions.add(partitionEntity.getId());
        }

        partitions.removeIf(p -> availablePartitions.contains(p.getId()));

        partitions.forEach(p ->
        {
            final PartitionEntity partitionEntity = new PartitionEntity();
            partitionEntity.setId(p.getId());
            partitionEntity.setTopicName(p.getTopicName());

            partitionRepository.save(partitionEntity);
        });

        partitions
            .stream()
            .map(Partition::getTopicName)
            .filter(t -> !Protocol.SYSTEM_TOPIC.equals(t))
            .distinct()
            .forEach(newTopic -> subscriber.openSubscription(newTopic));
    }

}
