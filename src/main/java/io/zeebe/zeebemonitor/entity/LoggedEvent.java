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
package io.zeebe.zeebemonitor.entity;

import javax.persistence.*;

@Entity
public class LoggedEvent
{
    @Id
    @GeneratedValue
    private Long id;

    @Column(length = 20000)
    private String eventPayload;

    private int partitionId;

    private long position;

    private long key;

    private String eventType;

    private String state;

    public LoggedEvent()
    {
    }

    public LoggedEvent(int partitionId, long position, long key, String eventType, String state, String eventPayload)
    {
        this.partitionId = partitionId;
        this.position = position;
        this.key = key;
        this.eventType = eventType;
        this.state = state;

        if (eventPayload != null && eventPayload.length() >= 20000)
        {
            eventPayload = eventPayload.substring(0, 19999);
        }

        this.eventPayload = eventPayload;
    }

    public String getPayload()
    {
        return eventPayload;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getEventPayload()
    {
        return eventPayload;
    }

    public int getPartitionId()
    {
        return partitionId;
    }

    public long getPosition()
    {
        return position;
    }

    public long getKey()
    {
        return key;
    }

    public String getEventType()
    {
        return eventType;
    }

    public String getState()
    {
        return state;
    }

}
