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

package org.apache.fineract.portfolio.savings.domain;

import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "m_transaction_request")
public class SavingsTransactionRequest extends AbstractPersistableCustom<Long> {

	@Column
	private String category;

	@Column(name = "image_tag")
	private String imageTag;

	@Column(name = "latitude")
	private String latitude;

	@Column(name = "longitude")
	private String longitude;

	@Column(name = "note_image")
	private String noteImage;

	@Column(name = "notes")
	private String notes;

	@Column(name = "remarks")
	private String remarks;

	@Column(name = "transaction_brand_name")
	private String transactionBrandName;

	@OneToOne
	@JoinColumn(name = "transaction_id", nullable = false)
	private SavingsAccountTransaction transaction;

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getImageTag() {
		return imageTag;
	}

	public void setImageTag(String imageTag) {
		this.imageTag = imageTag;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getNoteImage() {
		return noteImage;
	}

	public void setNoteImage(String noteImage) {
		this.noteImage = noteImage;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public String getTransactionBrandName() {
		return transactionBrandName;
	}

	public void setTransactionBrandName(String transactionBrandName) {
		this.transactionBrandName = transactionBrandName;
	}

	public SavingsAccountTransaction getTransaction() {
		return transaction;
	}

	public void setTransaction(SavingsAccountTransaction transaction) {
		this.transaction = transaction;
	}
}
