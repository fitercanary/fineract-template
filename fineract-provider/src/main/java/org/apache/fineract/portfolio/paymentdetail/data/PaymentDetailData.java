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
package org.apache.fineract.portfolio.paymentdetail.data;

import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;

/**
 * Immutable data object representing a payment.
 */
public class PaymentDetailData {

	@SuppressWarnings("unused")
	private final Long id;
	@SuppressWarnings("unused")
	private final PaymentTypeData paymentType;
	@SuppressWarnings("unused")
	private final String accountNumber;
	@SuppressWarnings("unused")
	private final String checkNumber;
	@SuppressWarnings("unused")
	private final String routingCode;
	@SuppressWarnings("unused")
	private final String receiptNumber;
	@SuppressWarnings("unused")
	private final String bankNumber;

	private String category;
	private String imageTag;
	private String latitude;
	private String longitude;
	private String noteImage;
	private String notes;
	private String remarks;
	private String transactionBrandName;

	public PaymentDetailData() {
		id = null;
		paymentType = null;
		accountNumber = checkNumber = routingCode = receiptNumber = bankNumber = null;
	}

	public PaymentDetailData(final Long id, final PaymentTypeData paymentType, final String accountNumber, final String checkNumber,
							 final String routingCode, final String receiptNumber, final String bankNumber) {
		this.id = id;
		this.paymentType = paymentType;
		this.accountNumber = accountNumber;
		this.checkNumber = checkNumber;
		this.routingCode = routingCode;
		this.receiptNumber = receiptNumber;
		this.bankNumber = bankNumber;
	}

	public String getTransactionBrandName() {
		return transactionBrandName;
	}

	public void setTransactionBrandName(String transactionBrandName) {
		this.transactionBrandName = transactionBrandName;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getNoteImage() {
		return noteImage;
	}

	public void setNoteImage(String noteImage) {
		this.noteImage = noteImage;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getImageTag() {
		return imageTag;
	}

	public void setImageTag(String imageTag) {
		this.imageTag = imageTag;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
}