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
package org.apache.fineract.portfolio.validation.limit.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.portfolio.validation.limit.api.ValidationLimitApiConstants;

@Entity
@Table(name = "m_validation_limits")
public class ValidationLimit extends AbstractPersistableCustom<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_level_cv_id", nullable = true)
    private CodeValue clientLevel;

    @Column(name = "maximum_single_deposit_amount")
    private BigDecimal maximumSingleDepositAmount;

    @Column(name = "maximum_cumulative_balance")
    private BigDecimal maximumCumulativeBalance;

    @Column(name = "maximum_transaction_limit")
    private BigDecimal maximumTransactionLimit;

    @Column(name = "maximum_daily_transaction_amount_limit")
    private BigDecimal maximumDailyTransactionAmountLimit;

    private ValidationLimit(final CodeValue clientLevel, final BigDecimal maximumSingleDepositAmount,
            final BigDecimal maximumCumulativeBalance, final BigDecimal maximumTransactionLimit,
            final BigDecimal maximumDailyTransactionAmountLimit) {
        this.clientLevel = clientLevel;
        this.maximumSingleDepositAmount = maximumSingleDepositAmount;
        this.maximumCumulativeBalance = maximumCumulativeBalance;
        this.maximumTransactionLimit = maximumTransactionLimit;
        this.maximumDailyTransactionAmountLimit = maximumDailyTransactionAmountLimit;

    }

    public static ValidationLimit fromJson(final CodeValue clientLevel, final JsonCommand command) {
        final BigDecimal maximumSingleDepositAmount = command.bigDecimalValueOfParameterNamed(ValidationLimitApiConstants.maximumSingleDepositAmountParamName);
        final BigDecimal maximumCumulativeBalance = command.bigDecimalValueOfParameterNamed(ValidationLimitApiConstants.maximumCumulativeBalanceParamName);
        final BigDecimal maximumTransactionLimit = command.bigDecimalValueOfParameterNamed(ValidationLimitApiConstants.maximumTransactionLimitParamName);
        final BigDecimal maximumDailyTransactionAmountLimit = command.bigDecimalValueOfParameterNamed(ValidationLimitApiConstants.maximumDailyTransactionAmountLimitParamName);
        return new ValidationLimit(clientLevel, maximumSingleDepositAmount, maximumCumulativeBalance, maximumTransactionLimit,
                maximumDailyTransactionAmountLimit);
    }

    public CodeValue getClientLevel() {
        return this.clientLevel;
    }

    public BigDecimal getMaximumSingleDepositAmount() {
        return this.maximumSingleDepositAmount;
    }

    public BigDecimal getMaximumCumulativeBalance() {
        return this.maximumCumulativeBalance;
    }

    public BigDecimal getMaximumTransactionLimit() {
        return this.maximumTransactionLimit;
    }

    public BigDecimal getMaximumDailyTransactionAmountLimit() {
        return this.maximumDailyTransactionAmountLimit;
    }

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        final String localeAsInput = command.locale();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        if (command.isChangeInLongParameterNamed(ValidationLimitApiConstants.clientLevelIdParamName, clientLevelId())) {
            final Long newValue = command.longValueOfParameterNamed(ValidationLimitApiConstants.clientLevelIdParamName);
            actualChanges.put(ValidationLimitApiConstants.clientLevelIdParamName, newValue);
        }

        final String maximumSingleDepositAmountParamName = ValidationLimitApiConstants.maximumSingleDepositAmountParamName;
        if (command.isChangeInBigDecimalParameterNamed(maximumSingleDepositAmountParamName, this.maximumSingleDepositAmount)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(maximumSingleDepositAmountParamName);
            actualChanges.put(maximumSingleDepositAmountParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            this.maximumSingleDepositAmount = newValue;
        }

        final String maximumCumulativeBalanceParamName = ValidationLimitApiConstants.maximumCumulativeBalanceParamName;
        if (command.isChangeInBigDecimalParameterNamed(maximumCumulativeBalanceParamName, this.maximumCumulativeBalance)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(maximumCumulativeBalanceParamName);
            actualChanges.put(maximumCumulativeBalanceParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            this.maximumCumulativeBalance = newValue;
        }

        final String maximumTransactionLimitParamName = ValidationLimitApiConstants.maximumTransactionLimitParamName;
        if (command.isChangeInBigDecimalParameterNamed(maximumTransactionLimitParamName, this.maximumTransactionLimit)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(maximumTransactionLimitParamName);
            actualChanges.put(maximumTransactionLimitParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            this.maximumTransactionLimit = newValue;
        }

        final String maximumDailyTransactionAmountLimitParamName = ValidationLimitApiConstants.maximumDailyTransactionAmountLimitParamName;
        if (command.isChangeInBigDecimalParameterNamed(maximumDailyTransactionAmountLimitParamName,
                this.maximumDailyTransactionAmountLimit)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(maximumDailyTransactionAmountLimitParamName);
            actualChanges.put(maximumDailyTransactionAmountLimitParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            this.maximumDailyTransactionAmountLimit = newValue;
        }

        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }

        return actualChanges;
    }

    public Long clientLevelId() {
        Long clientLevelId = null;
        if (this.clientLevel != null) {
            clientLevelId = this.clientLevel.getId();
        }
        return clientLevelId;
    }

    public void updateClientLevel(CodeValue clientLevel) {
        this.clientLevel = clientLevel;
    }

}
