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
package org.apache.fineract.accounting.journalentry.command;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.fineract.accounting.journalentry.api.JournalEntryJsonInputParams;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.joda.time.LocalDate;

/**
 * Immutable command for adding an accounting closure
 */
public class JournalEntryCommand {

    private final Long officeId;
    private final LocalDate transactionDate;
    private final String currencyCode;
    private final String comments;
    private final String referenceNumber;
    private final Long accountingRuleId;
    private final BigDecimal amount;
    private final Long paymentTypeId;
    @SuppressWarnings("unused")
    private final String accountNumber;
    @SuppressWarnings("unused")
    private final String checkNumber;
    @SuppressWarnings("unused")
    private final String receiptNumber;
    @SuppressWarnings("unused")
    private final String bankNumber;
    @SuppressWarnings("unused")
    private final String routingCode;

    private final SingleDebitOrCreditEntryCommand[] credits;
    private final SingleDebitOrCreditEntryCommand[] debits;

    public JournalEntryCommand(final Long officeId, final String currencyCode, final LocalDate transactionDate, final String comments,
            final SingleDebitOrCreditEntryCommand[] credits, final SingleDebitOrCreditEntryCommand[] debits, final String referenceNumber,
            final Long accountingRuleId, final BigDecimal amount, final Long paymentTypeId, final String accountNumber,
            final String checkNumber, final String receiptNumber, final String bankNumber, final String routingCode) {
        this.officeId = officeId;
        this.currencyCode = currencyCode;
        this.transactionDate = transactionDate;
        this.comments = comments;
        this.credits = credits;
        this.debits = debits;
        this.referenceNumber = referenceNumber;
        this.accountingRuleId = accountingRuleId;
        this.amount = amount;
        this.paymentTypeId = paymentTypeId;
        this.accountNumber = accountNumber;
        this.checkNumber = checkNumber;
        this.receiptNumber = receiptNumber;
        this.bankNumber = bankNumber;
        this.routingCode = routingCode;
    }

    public void validateForCreate() {

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("GLJournalEntry");

        baseDataValidator.reset().parameter("transactionDate").value(this.transactionDate).notBlank();

        baseDataValidator.reset().parameter("officeId").value(this.officeId).notNull().integerGreaterThanZero();

        baseDataValidator.reset().parameter(JournalEntryJsonInputParams.CURRENCY_CODE.getValue()).value(this.currencyCode).notBlank();

        baseDataValidator.reset().parameter("comments").value(this.comments).ignoreIfNull().notExceedingLengthOf(500);

        baseDataValidator.reset().parameter("referenceNumber").value(this.referenceNumber).ignoreIfNull().notExceedingLengthOf(100);

        baseDataValidator.reset().parameter("accountingRule").value(this.accountingRuleId).ignoreIfNull().longGreaterThanZero();

        baseDataValidator.reset().parameter("paymentTypeId").value(this.paymentTypeId).ignoreIfNull().longGreaterThanZero();

        baseDataValidator.reset().parameter("amount").value(this.amount).ignoreIfNull().zeroOrPositiveAmount();

        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist",
                "Validation errors exist.", dataValidationErrors); }
    }

    /**
     * @param baseDataValidator
     * @param i
     * @param credit
     */

    public Long getOfficeId() {
        return this.officeId;
    }

    public LocalDate getTransactionDate() {
        return this.transactionDate;
    }

    public String getComments() {
        return this.comments;
    }

    public SingleDebitOrCreditEntryCommand[] getCredits() {
        return this.credits;
    }

    public SingleDebitOrCreditEntryCommand[] getDebits() {
        return this.debits;
    }

    public String getReferenceNumber() {
        return this.referenceNumber;
    }

    public Long getAccountingRuleId() {
        return this.accountingRuleId;
    }

}