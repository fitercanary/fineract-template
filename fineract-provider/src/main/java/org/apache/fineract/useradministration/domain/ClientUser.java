/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.useradministration.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.fineract.portfolio.client.domain.Client;

@Entity
@Table(name="m_client_user")
public class ClientUser {

    @EmbeddedId
    private ClientUserId id = new ClientUserId();
    
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    @MapsId("clientId")
    private Client client;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @MapsId("userId")
    private AppUser user;
    
    @Column(name = "start_time", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;
    
    @Column(name = "end_time", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;
    
    @Column(name = "is_expired", nullable = false)
    private boolean isExpired;
    
    @OneToOne
    @Column(name = "authorized_by", nullable = false)
    private AppUser authorizedBy;
    
    @Column(name = "comment", nullable = true)
    private String comment;
    
    public ClientUser() {}
    
    public ClientUser(Client client, AppUser user, Date startTime, Date endTime, boolean isExpired) {
        this.client = client;
        this.user = user;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isExpired = isExpired;
    }
    
    public ClientUser(Client client, AppUser user, Date startTime, Date endTime, boolean isExpired, AppUser authorizedBy, String comment) {
        this.client = client;
        this.user = user;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isExpired = isExpired;
        this.authorizedBy = authorizedBy;
        this.comment = comment;
    }

    
    public ClientUserId getId() {
        return this.id;
    }

    
    public Client getClient() {
        return this.client;
    }

    
    public AppUser getUser() {
        return this.user;
    }

    
    public Date getStartTime() {
        return this.startTime;
    }

    
    public Date getEndTime() {
        return this.endTime;
    }

    
    public boolean isExpired() {
        return this.isExpired;
    }

    
    public AppUser getAuthorizedBy() {
        return this.authorizedBy;
    }

    
    public String getComment() {
        return this.comment;
    }
    
    
    
}
