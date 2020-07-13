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
package org.apache.fineract.notification.domain;

import java.math.BigDecimal;

import org.apache.fineract.portfolio.account.domain.AccountTransferRequestBody;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.Gson;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VfdTransferNotification {

    private Long senderClientId;
    private String senderAccountNumber;
    private String senderNarration;
    private BigDecimal charge;
    private Long senderAccountId;
    private String alertType;
    private BigDecimal amount;
    private Long beneficiaryClientId;
    private String beneficiaryAccountNumber;
    private String beneficiaryNarration;
    private Long beneficiaryAccountId;

    public VfdTransferNotification() {}

    public VfdTransferNotification(Long senderClientId, String senderAccountNumber, String senderNarration, String alertType,
            BigDecimal amount) {
        this.senderClientId = senderClientId;
        this.senderAccountNumber = senderAccountNumber;
        this.senderNarration = senderNarration;
        this.alertType = alertType;
        this.amount = amount;
    }

    public VfdTransferNotification(Long senderClientId, String senderAccountNumber, String senderNarration, BigDecimal charge,
            Long senderAccountId, String alertType, BigDecimal amount, Long beneficiaryClientId, String beneficiaryAccountNumber,
            String beneficiaryNarration, Long beneficiaryAccountId) {
        this.senderClientId = senderClientId;
        this.senderAccountNumber = senderAccountNumber;
        this.senderNarration = senderNarration;
        this.charge = charge;
        this.senderAccountId = senderAccountId;
        this.alertType = alertType;
        this.amount = amount;
        this.beneficiaryClientId = beneficiaryClientId;
        this.beneficiaryAccountNumber = beneficiaryAccountNumber;
        this.beneficiaryNarration = beneficiaryNarration;
        this.beneficiaryAccountId = beneficiaryAccountId;
    }

    public static VfdTransferNotification fromRequest(String apiJsonRequestString) {

        Gson gson = new Gson();
        AccountTransferRequestBody request = gson.fromJson(apiJsonRequestString, AccountTransferRequestBody.class);

        return new VfdTransferNotification(request.getFromClientId(), null, request.getTransferDescription(), null,
                request.getFromAccountId(), "both", request.getTransferAmount(), request.getToClientId(), null, request.getRemarks(),
                request.getToAccountId());

    }

    public static VfdTransferNotification fromQueueString(String request) {
        return null;
    }

    public Long getSenderClientId() {
        return this.senderClientId;
    }

    public void setSenderClientId(Long senderClientId) {
        this.senderClientId = senderClientId;
    }

    public String getSenderAccountNumber() {
        return this.senderAccountNumber;
    }

    public void setSenderAccountNumber(String senderAccountNumber) {
        this.senderAccountNumber = senderAccountNumber;
    }

    public String getSenderNarration() {
        return this.senderNarration;
    }

    public void setSenderNarration(String senderNarration) {
        this.senderNarration = senderNarration;
    }

    public BigDecimal getCharge() {
        return this.charge;
    }

    public void setCharge(BigDecimal charge) {
        this.charge = charge;
    }

    public Long getSenderAccountId() {
        return this.senderAccountId;
    }

    public void setSenderAccountId(Long senderAccountId) {
        this.senderAccountId = senderAccountId;
    }

    public String getAlertType() {
        return this.alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getBeneficiaryClientId() {
        return this.beneficiaryClientId;
    }

    public void setBeneficiaryClientId(Long beneficiaryClientId) {
        this.beneficiaryClientId = beneficiaryClientId;
    }

    public String getBeneficiaryAccountNumber() {
        return this.beneficiaryAccountNumber;
    }

    public void setBeneficiaryAccountNumber(String beneficiaryAccountNumber) {
        this.beneficiaryAccountNumber = beneficiaryAccountNumber;
    }

    public String getBeneficiaryNarration() {
        return this.beneficiaryNarration;
    }

    public void setBeneficiaryNarration(String beneficiaryNarration) {
        this.beneficiaryNarration = beneficiaryNarration;
    }

    public Long getBeneficiaryAccountId() {
        return this.beneficiaryAccountId;
    }

    public void setBeneficiaryAccountId(Long beneficiaryAccountId) {
        this.beneficiaryAccountId = beneficiaryAccountId;
    }

    @Override
    public String toString() {
        return "VfdTransferNotification [senderClientId=" + this.senderClientId + ", senderAccountNumber=" + this.senderAccountNumber
                + ", senderNarration=" + this.senderNarration + ", charge=" + this.charge + ", senderAccountId=" + this.senderAccountId
                + ", alertType=" + this.alertType + ", amount=" + this.amount + ", beneficiaryClientId=" + this.beneficiaryClientId
                + ", beneficiaryAccountNumber=" + this.beneficiaryAccountNumber + ", beneficiaryNarration=" + this.beneficiaryNarration
                + ", beneficiaryAccountId=" + this.beneficiaryAccountId + "]";
    }

}
