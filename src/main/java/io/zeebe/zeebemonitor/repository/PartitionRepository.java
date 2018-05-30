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
package io.zeebe.zeebemonitor.repository;

import java.util.List;

import io.zeebe.protocol.Protocol;
import io.zeebe.zeebemonitor.entity.PartitionEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface PartitionRepository extends CrudRepository<PartitionEntity, Integer>
{
    @Query("SELECT DISTINCT topicName FROM PartitionEntity p WHERE p.id != " + Protocol.SYSTEM_PARTITION)
    List<String> getTopicNames();

}
