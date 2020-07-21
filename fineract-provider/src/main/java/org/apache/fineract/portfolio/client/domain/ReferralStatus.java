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
package org.apache.fineract.portfolio.client.domain;

import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name = "referral_status")
public class ReferralStatus extends AbstractPersistableCustom<Long> {

	@ManyToOne
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	@Column(name = "status", nullable = false)
	private String status;

	@Column(name = "phone_no")
	private String phoneNo;

	@Column(name = "email")
	private String email;

	@Column(name = "device_id")
	private String deviceId;

	@ManyToOne
	@JoinColumn(name = "referred_client_id")
	private Client referredClient;

	@Column(name = "last_saved", nullable = false)
	@Temporal(TemporalType.DATE)
	private Date lastSaved;

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPhoneNo() {
		return phoneNo;
	}

	public void setPhoneNo(String phoneNo) {
		this.phoneNo = phoneNo;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public Client getReferredClient() {
		return referredClient;
	}

	public void setReferredClient(Client referredClient) {
		this.referredClient = referredClient;
	}

	public Date getLastSaved() {
		return lastSaved;
	}

	public void setLastSaved(Date lastSaved) {
		this.lastSaved = lastSaved;
	}
}
