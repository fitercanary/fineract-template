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
package org.apache.fineract.accounting.provisioning.data;

import java.math.BigDecimal;

public class ProductProvisioningEntryData {

    private final Long historyId;

    private final Long officeId;

    private final String officeName;

    private final String currencyCode;

    private final Long productId;

    private final String productName;

    private final Long categoryId;

    private final String categoryName;

    private final Long overdueInDays;

    private final BigDecimal percentage;

    private final BigDecimal balance;

    private final BigDecimal amountReserved;

    private final Long liabilityAccount;

    private final String liabilityAccountCode;

    private final String liabilityAccountName;

    private final Long expenseAccount;

    private final String expenseAccountCode;

    private final String expenseAccountName;

    private final Long criteriaId;

    private final Boolean isLoan;

    private final String accountNumber;

    private Long loanId;

    public ProductProvisioningEntryData(final Long historyId, final Long officeId, final String currencyCode, final Long productId,
            final Long categoryId, final Long overdueInDays, final BigDecimal percentage, final BigDecimal balance, Long liablityAccount,
            Long expenseAccount, final Long criteriaId, final String accountNumber, final Boolean isLoan, Long loanId) {

        this.historyId = historyId;
        this.officeId = officeId;
        this.currencyCode = currencyCode;
        this.productId = productId;
        this.categoryId = categoryId;
        this.overdueInDays = overdueInDays;
        this.percentage = percentage;
        this.balance = balance;
        this.liabilityAccount = liablityAccount;
        this.expenseAccount = expenseAccount;
        this.amountReserved = null;
        this.officeName = null;
        this.productName = null;
        this.categoryName = null;
        this.liabilityAccountCode = null;
        this.liabilityAccountName = null;
        this.expenseAccountCode = null;
        this.expenseAccountName = null;
        this.criteriaId = criteriaId;
        this.accountNumber = accountNumber;
        this.isLoan = isLoan;
        this.loanId = loanId;

    }

    public ProductProvisioningEntryData(final Long historyId, final Long officeId, final String officeName, final String currencyCode,
            final Long productId, final String productName, final Long categoryId, final String categoryName, final Long overdueInDays,
            final BigDecimal amountReserved, Long liabilityAccount, String liabilityAccountGlCode, String liabilityAccountName,
            Long expenseAccount, String expenseAccountGlCode, String expenseAccountName, final Long criteriaId, final String accountNumber,
            final Boolean isLoan) {
        this.loanId = null;
        this.historyId = historyId;
        this.officeId = officeId;
        this.currencyCode = currencyCode;
        this.productId = productId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.overdueInDays = overdueInDays;
        this.percentage = null;
        this.balance = null;
        this.liabilityAccount = liabilityAccount;
        this.expenseAccount = expenseAccount;
        this.officeName = officeName;
        this.productName = productName;
        this.amountReserved = amountReserved;
        this.liabilityAccountCode = liabilityAccountGlCode;
        this.liabilityAccountName = liabilityAccountName;
        this.expenseAccountCode = expenseAccountGlCode;
        this.expenseAccountName = expenseAccountName;
        this.criteriaId = criteriaId;
        this.accountNumber = accountNumber;
        this.isLoan = isLoan;
    }

    public Long getHistoryId() {
        return this.historyId;
    }

    public Long getOfficeId() {
        return this.officeId;
    }

    public Long getProductId() {
        return this.productId;
    }

    public Long getCategoryId() {
        return this.categoryId;
    }

    public Long getOverdueInDays() {
        return this.overdueInDays;
    }

    public BigDecimal getOutstandingBalance() {
        return balance;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public Long getLiabilityAccount() {
        return this.liabilityAccount;
    }

    public Long getExpenseAccount() {
        return this.expenseAccount;
    }

    public String getCurrencyCode() {
        return this.currencyCode;
    }

    public Long getCriteriaId() {
        return this.criteriaId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Boolean isLoan() {
        return this.isLoan;
    }

    public Long getLoanId() {
        return this.loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

}
