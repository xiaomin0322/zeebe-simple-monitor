package io.zeebe.zeebemonitor.repository;

import java.util.Iterator;
import java.util.Optional;

import io.zeebe.zeebemonitor.entity.ConfigurationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationRepositoryImpl implements ConfigurationRepositoryCustom
{
    @Autowired
    private ConfigurationRepository repository;

    @Override
    public Optional<ConfigurationEntity> getConfiguration()
    {
        final Iterator<ConfigurationEntity> iterator = repository.findAll().iterator();

        if (iterator.hasNext())
        {
            return Optional.of(iterator.next());
        }
        else
        {
            return Optional.empty();
        }
    }

}
