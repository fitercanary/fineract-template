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
package org.apache.fineract.portfolio.client.data;

import org.apache.fineract.portfolio.client.domain.ReferralStatus;
import org.joda.time.LocalDate;

public class ReferralStatusData {

	private String status;
	private String phoneNo;
	private String email;
	private String deviceId;
	private LocalDate lastSaved;
	private String referralId;
	private String dynamicLink;
	private Long referredClientId;
	private String referredClient;

	public ReferralStatusData(ReferralStatus referralStatus) {
		this.email = referralStatus.getEmail();
		this.status = referralStatus.getStatus();
		this.phoneNo = referralStatus.getPhoneNo();
		this.deviceId = referralStatus.getDeviceId();
		this.referralId = referralStatus.getClient().getReferralId();
		this.dynamicLink = referralStatus.getClient().getReferralDynamicLink();
		this.lastSaved = LocalDate.fromDateFields(referralStatus.getLastSaved());
		if (referralStatus.getReferredClient() != null) {
			this.referredClientId = referralStatus.getReferredClient().getId();
			this.referredClient = referralStatus.getReferredClient().getDisplayName();
		}
	}
}
