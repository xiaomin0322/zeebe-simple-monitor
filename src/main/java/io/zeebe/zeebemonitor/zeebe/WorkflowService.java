package io.zeebe.zeebemonitor.zeebe;

import java.util.*;
import java.util.stream.Collectors;

import io.zeebe.client.api.commands.Workflow;
import io.zeebe.client.api.commands.WorkflowResource;
import io.zeebe.zeebemonitor.entity.WorkflowEntity;
import io.zeebe.zeebemonitor.repository.WorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class WorkflowService
{
    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private ZeebeConnectionService connectionService;

    @Async
    public void loadWorkflowByKey(long workflowKey)
    {
        loadWorkflowsByKey(Collections.singletonList(workflowKey));
    }

    @Async
    public void loadWorkflowsByKey(final List<Long> workflowKeys)
    {
        workflowKeys.forEach(workflowKey ->
        {
            final WorkflowResource resource = connectionService
                    .getClient()
                    .topicClient()
                    .workflowClient()
                    .newResourceRequest()
                    .workflowKey(workflowKey)
                    .send()
                    .join();

            final WorkflowEntity entity = WorkflowEntity.from(resource);
            workflowRepository.save(entity);
        });
    }

    public void synchronizeWithBroker()
    {
        final List<Workflow> workflows = connectionService
            .getClient()
            .topicClient()
            .workflowClient()
            .newWorkflowRequest()
            .send()
            .join()
            .getWorkflows();

        final List<Long> workflowKeys = workflows
                .stream()
                .map(Workflow::getWorkflowKey)
                .collect(Collectors.toList());

        final List<Long> availableWorkflows = new ArrayList<>();
        for (WorkflowEntity workflowEntity : workflowRepository.findAll())
        {
            availableWorkflows.add(workflowEntity.getWorkflowKey());
        }

        workflowKeys.removeAll(availableWorkflows);

        if (!workflowKeys.isEmpty())
        {
            loadWorkflowsByKey(workflowKeys);
        }
    }

}
