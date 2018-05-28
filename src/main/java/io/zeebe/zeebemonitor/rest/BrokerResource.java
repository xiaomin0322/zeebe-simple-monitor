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

import java.util.Iterator;

import io.zeebe.zeebemonitor.entity.ConfigurationEntity;
import io.zeebe.zeebemonitor.repository.ConfigurationRepository;
import io.zeebe.zeebemonitor.zeebe.ZeebeConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@RestController
@RequestMapping("/api/broker")
public class BrokerResource
{

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private ZeebeConnectionService zeebeConnections;



    @RequestMapping("/")
    public ConfigurationEntity getConfiguration()
    {
        return getConfig();
    }

    private ConfigurationEntity getConfig()
    {
        final Iterable<ConfigurationEntity> configs = configurationRepository.findAll();
        final Iterator<ConfigurationEntity> configIterator = configs.iterator();

        if (configIterator.hasNext())
        {
            return configIterator.next();
        }
        else
        {
            return null;
        }
    }

    @RequestMapping(path = "/connect", method = RequestMethod.POST)
    public ConfigurationEntity connect(@RequestBody String connectionString)
    {
        final ConfigurationEntity config = getConfig();

        if (config != null)
        {
            throw new RuntimeException("Monitor is already connected to: " + config.getConnectionString());
        }
        else
        {
            final ConfigurationEntity newConfig = new ConfigurationEntity(connectionString);
            configurationRepository.save(newConfig);

            zeebeConnections.connect(newConfig);

            return newConfig;
        }
    }

    @RequestMapping(path = "/check-connection")
    public boolean checkConnection()
    {
        return zeebeConnections.isConnected();
    }

    @RequestMapping(path = "/cleanup", method = RequestMethod.POST)
    public void cleanup()
    {
        zeebeConnections.deleteAllData();
    }

}
