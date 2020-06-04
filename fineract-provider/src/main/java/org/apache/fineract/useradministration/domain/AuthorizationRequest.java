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

import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.client.domain.Client;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name = "m_user_client_authorization_request")
public class AuthorizationRequest extends AbstractPersistableCustom<Long>{

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
    
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @Column(name = "status_enum", nullable = false)
    private Integer status;
    
    @Column(name = "requestedon_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date requestedDate;
    
    @Column(name = "aprovedon_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date approvedDate;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "approvedon_userid")
    private AppUser approvedBy;
    
    @Column(name = "rejectedon_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date rejectionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejectedon_userid")
    private AppUser rejectedBy;
    
    @Column(name = "comment")
    private String comment;
    
    public AuthorizationRequest(AppUser user, Client client, AuthorizationRequestStatusType status, Date requestedDate, String comment) {
        this.user = user;
        this.client = client;
        this.status = status != null ?  status.getValue() : null;
        this.requestedDate = requestedDate;
        this.comment = comment;
    }

    
    public AppUser getUser() {
        return this.user;
    }

    
    public Client getClient() {
        return this.client;
    }

    
    public Integer getStatus() {
        return this.status;
    }

    
    public Date getRequestedDate() {
        return this.requestedDate;
    }

    
    public Date getApprovedDate() {
        return this.approvedDate;
    }

    
    public AppUser getApprovedBy() {
        return this.approvedBy;
    }

    
    public Date getRejectionDate() {
        return this.rejectionDate;
    }

    
    public AppUser getRejectedBy() {
        return this.rejectedBy;
    }

    
    public String getComment() {
        return this.comment;
    }
    
    public void approveRequest( Date approvedDate, AppUser approvedBy ) {
        this.approvedDate = approvedDate;
        this.approvedBy = approvedBy;
        this.status = AuthorizationRequestStatusType.APPROVED.getValue();
    }
    
    public void rejectRequest( Date rejectionDate, AppUser rejectedBy ) {
        this.rejectionDate = rejectionDate;
        this.rejectedBy = rejectedBy;
        this.status = AuthorizationRequestStatusType.REJECTED.getValue();
    }
    
    
    
}
