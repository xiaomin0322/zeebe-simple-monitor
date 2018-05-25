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

import java.util.UUID;

import javax.persistence.*;

@Entity
public class Incident
{

    @GeneratedValue
    @Id
    private String id = UUID.randomUUID().toString();

    private long incidentKey;

    private String activityId;
    private String errorType;
    private String errorMessage;

    public Incident()
    {
    }

    public Incident(long incidentKey, String activityId, String errorType, String errorMessage)
    {
        this.setIncidentKey(incidentKey);
        this.activityId = activityId;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    public String getId()
    {
        return id;
    }

    public String getActivityId()
    {
        return activityId;
    }

    public void setActivityId(String activityId)
    {
        this.activityId = activityId;
    }

    public String getErrorType()
    {
        return errorType;
    }

    public Incident setErrorType(String errorType)
    {
        this.errorType = errorType;
        return this;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public Incident setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
        return this;
    }

    public long getIncidentKey()
    {
        return incidentKey;
    }

    public void setIncidentKey(long incidentKey)
    {
        this.incidentKey = incidentKey;
    }



}
