package io.zeebe.zeebemonitor.repository;

import java.util.List;

import io.zeebe.zeebemonitor.entity.IncidentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IncidentRepositoryImpl implements IncidentRepositoryCustom
{
    @Autowired
    private IncidentRepository repository;

    @Override
    public IncidentEntity getIncident(int partitionId, long key)
    {
        final List<IncidentEntity> incidents = repository.findByPartitionIdAndKey(partitionId, key);

        if (incidents.isEmpty())
        {
            return null;
        }
        else
        {
            return incidents.get(0);
        }
    }

}
