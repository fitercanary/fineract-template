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
package org.apache.fineract.portfolio.account.domain;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountTransferRequestBody {

    private Long fromAccountId;
    private Integer fromAccountType;
    private Integer toOfficeId;
    private Integer toAccountType;
    private Long toClientId;
    private Long toAccountId;
    private BigDecimal transferAmount;
    private String transferDate;
    private String transferDescription;
    private String remarks;
    private String locale;
    private String dateFormat;
    private Long fromClientId;
    private Integer fromOfficeId;

    public AccountTransferRequestBody() {}

    public Long getFromAccountId() {
        return this.fromAccountId;
    }

    public void setFromAccountId(Long fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public Integer getFromAccountType() {
        return this.fromAccountType;
    }

    public void setFromAccountType(Integer fromAccountType) {
        this.fromAccountType = fromAccountType;
    }

    public Integer getToOfficeId() {
        return this.toOfficeId;
    }

    public void setToOfficeId(Integer toOfficeId) {
        this.toOfficeId = toOfficeId;
    }

    public Integer getToAccountType() {
        return this.toAccountType;
    }

    public void setToAccountType(Integer toAccountType) {
        this.toAccountType = toAccountType;
    }

    public Long getToClientId() {
        return this.toClientId;
    }

    public void setToClientId(Long toClientId) {
        this.toClientId = toClientId;
    }

    public Long getToAccountId() {
        return this.toAccountId;
    }

    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
    }

    public BigDecimal getTransferAmount() {
        return this.transferAmount;
    }

    public void setTransferAmount(BigDecimal transferAmount) {
        this.transferAmount = transferAmount;
    }

    public String getTransferDate() {
        return this.transferDate;
    }

    public void setTransferDate(String transferDate) {
        this.transferDate = transferDate;
    }

    public String getTransferDescription() {
        return this.transferDescription;
    }

    public void setTransferDescription(String transferDescription) {
        this.transferDescription = transferDescription;
    }

    public String getRemarks() {
        return this.remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getLocale() {
        return this.locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getDateFormat() {
        return this.dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public Long getFromClientId() {
        return this.fromClientId;
    }

    public void setFromClientId(Long fromClientId) {
        this.fromClientId = fromClientId;
    }

    public Integer getFromOfficeId() {
        return this.fromOfficeId;
    }

    public void setFromOfficeId(Integer fromOfficeId) {
        this.fromOfficeId = fromOfficeId;
    }

    @Override
    public String toString() {
        return "AccountTransferRequestBody [fromAccountId=" + this.fromAccountId + ", fromAccountType=" + this.fromAccountType
                + ", toOfficeId=" + this.toOfficeId + ", toAccountType=" + this.toAccountType + ", toClientId=" + this.toClientId
                + ", toAccountId=" + this.toAccountId + ", transferAmount=" + this.transferAmount + ", transferDate=" + this.transferDate
                + ", transferDescription=" + this.transferDescription + ", remarks=" + this.remarks + ", locale=" + this.locale
                + ", dateFormat=" + this.dateFormat + ", fromClientId=" + this.fromClientId + ", fromOfficeId=" + this.fromOfficeId + "]";
    }

}
