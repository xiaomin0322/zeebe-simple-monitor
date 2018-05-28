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
package io.zeebe.zeebemonitor;

import javax.annotation.PostConstruct;

import io.zeebe.zeebemonitor.repository.ConfigurationRepository;
import io.zeebe.zeebemonitor.zeebe.ZeebeConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ZeebeSimpleMonitorApp
{
    @Autowired
    private ZeebeConnectionService connectionService;

    @Autowired
    private ConfigurationRepository configurationRepository;

    public static void main(String... args)
    {
        SpringApplication.run(ZeebeSimpleMonitorApp.class, args);
    }

    @PostConstruct
    public void initConnection()
    {
        configurationRepository.getConfiguration().ifPresent(config ->
        {
            connectionService.connect(config);
        });
    }
}
