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
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientContactInformation implements RowMapper<ClientContactInformation> {

	private Long clientId;

	private String phoneNumber;

	private String email;

	private String alternatePhoneNumber;

	private Boolean enableSms;

	private Boolean enableEmail;

	private ClientContactInformation(final Long clientId, final String phoneNumber, final String email, final String alternatePhoneNumber,
									 final boolean enableSms, final boolean enableEmail) {
		this.setClientId(clientId);
		this.setEmail(email);
		this.setPhoneNumber(phoneNumber);
		this.setAlternatePhoneNumber(alternatePhoneNumber);
		this.setEnableSms(enableSms);
		this.setEnableEmail(enableEmail);

	}
	public ClientContactInformation(final Long clientId) {
		this.setClientId(clientId);
		this.setEmail(null);
		this.setPhoneNumber(null);
		this.setAlternatePhoneNumber(null);
		this.setEnableSms(null);
		this.setEnableEmail(null);

	}

	public String schema() {
		return "";
	}

	@Override
	public ClientContactInformation mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
		final Long clientId = rs.getLong("client_id");
		final String phonenumber = rs.getString("phonenumber");
		final String alternatePhonenumber = rs.getString("alternate_phonenumber");
		final String email = rs.getString("email");
		final Boolean enableEmail = rs.getBoolean("enable_email");
		final Boolean enableSms = rs.getBoolean("enable_sms");

		final ClientContactInformation clientContactInformation = new ClientContactInformation(clientId,
				phonenumber,email, alternatePhonenumber,enableEmail, enableSms);
		return clientContactInformation;
	}


	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAlternatePhoneNumber() {
		return alternatePhoneNumber;
	}

	public void setAlternatePhoneNumber(String alternatePhoneNumber) {
		this.alternatePhoneNumber = alternatePhoneNumber;
	}

	public Boolean getEnableSms() {
		return enableSms;
	}

	public void setEnableSms(Boolean enableSms) {
		this.enableSms = enableSms;
	}

	public Boolean getEnableEmail() {
		return enableEmail;
	}

	public void setEnableEmail(Boolean enableEmail) {
		this.enableEmail = enableEmail;
	}

	public Long getClientId() {
		return clientId;
	}

	public void setClientId(Long clientId) {
		this.clientId = clientId;
	}
}
