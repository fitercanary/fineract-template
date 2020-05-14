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
    @JoinColumn(name = "client_level_cv_id")
    private CodeValue clientLevel;

    @Column(name = "maximum_single_deposit_amount")
    private BigDecimal maximumSingleDepositAmount;

    @Column(name = "maximum_cumulative_balance")
    private BigDecimal maximumCumulativeBalance;

    @Column(name = "maximum_transaction_limit")
    private BigDecimal maximumSingleWithdrawLimit;

    @Column(name = "maximum_daily_transaction_amount_limit")
    private BigDecimal maximumDailyWithdrawLimit;

    @Column
    private Boolean overridable;

    private ValidationLimit(final CodeValue clientLevel, final BigDecimal maximumSingleDepositAmount,
            final BigDecimal maximumCumulativeBalance, final BigDecimal maximumTransactionLimit,
            final BigDecimal maximumDailyTransactionAmountLimit, Boolean overridable) {
        this.clientLevel = clientLevel;
        this.overridable = overridable;
        this.maximumSingleDepositAmount = maximumSingleDepositAmount;
        this.maximumCumulativeBalance = maximumCumulativeBalance;
        this.maximumSingleWithdrawLimit = maximumTransactionLimit;
        this.maximumDailyWithdrawLimit = maximumDailyTransactionAmountLimit;

    }

    public static ValidationLimit fromJson(final CodeValue clientLevel, final JsonCommand command) {
        final BigDecimal maximumSingleDepositAmount = command.bigDecimalValueOfParameterNamed(ValidationLimitApiConstants.MAXIMUM_SINGLE_DEPOSIT_AMOUNT);
        final BigDecimal maximumCumulativeBalance = command.bigDecimalValueOfParameterNamed(ValidationLimitApiConstants.MAXIMUM_CUMULATIVE_BALANCE);
        final BigDecimal maximumSingleWithdrawLimit = command.bigDecimalValueOfParameterNamed(ValidationLimitApiConstants.MAXIMUM_SINGLE_WITHDRAW_LIMIT);
        final BigDecimal maximumDailyWithdrawLimit = command.bigDecimalValueOfParameterNamed(ValidationLimitApiConstants.MAXIMUM_DAILY_WITHDRAW_LIMIT);
        final Boolean overridable = command.booleanPrimitiveValueOfParameterNamed(ValidationLimitApiConstants.OVERRIDABLE);
        return new ValidationLimit(clientLevel, maximumSingleDepositAmount, maximumCumulativeBalance, maximumSingleWithdrawLimit,
                maximumDailyWithdrawLimit, overridable);
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
        return this.maximumSingleWithdrawLimit;
    }

    public BigDecimal getMaximumDailyTransactionAmountLimit() {
        return this.maximumDailyWithdrawLimit;
    }

    public Boolean isOverridable() {
        return overridable;
    }

    public void setOverridable(Boolean overridable) {
        this.overridable = overridable;
    }

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        final String localeAsInput = command.locale();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        if (command.isChangeInLongParameterNamed(ValidationLimitApiConstants.CLIENT_LEVEL_ID, clientLevelId())) {
            final Long newValue = command.longValueOfParameterNamed(ValidationLimitApiConstants.CLIENT_LEVEL_ID);
            actualChanges.put(ValidationLimitApiConstants.CLIENT_LEVEL_ID, newValue);
        }

        final String maximumSingleDepositAmountParamName = ValidationLimitApiConstants.MAXIMUM_SINGLE_DEPOSIT_AMOUNT;
        if (command.isChangeInBigDecimalParameterNamed(maximumSingleDepositAmountParamName, this.maximumSingleDepositAmount)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(maximumSingleDepositAmountParamName);
            actualChanges.put(maximumSingleDepositAmountParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            this.maximumSingleDepositAmount = newValue;
        }

        final String maximumCumulativeBalanceParamName = ValidationLimitApiConstants.MAXIMUM_CUMULATIVE_BALANCE;
        if (command.isChangeInBigDecimalParameterNamed(maximumCumulativeBalanceParamName, this.maximumCumulativeBalance)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(maximumCumulativeBalanceParamName);
            actualChanges.put(maximumCumulativeBalanceParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            this.maximumCumulativeBalance = newValue;
        }

        final String maximumSingleWithdrawLimitParamName = ValidationLimitApiConstants.MAXIMUM_SINGLE_WITHDRAW_LIMIT;
        if (command.isChangeInBigDecimalParameterNamed(maximumSingleWithdrawLimitParamName, this.maximumSingleWithdrawLimit)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(maximumSingleWithdrawLimitParamName);
            actualChanges.put(maximumSingleWithdrawLimitParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            this.maximumSingleWithdrawLimit = newValue;
        }

        final String maximumDailyWithdrawLimitParamName = ValidationLimitApiConstants.MAXIMUM_DAILY_WITHDRAW_LIMIT;
        if (command.isChangeInBigDecimalParameterNamed(maximumDailyWithdrawLimitParamName,
                this.maximumDailyWithdrawLimit)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(maximumDailyWithdrawLimitParamName);
            actualChanges.put(maximumDailyWithdrawLimitParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            this.maximumDailyWithdrawLimit = newValue;
        }

        if (command.isChangeInBooleanParameterNamed(ValidationLimitApiConstants.OVERRIDABLE, isOverridable())) {
            final Boolean newValue = command.booleanObjectValueOfParameterNamed(ValidationLimitApiConstants.OVERRIDABLE);
            actualChanges.put(ValidationLimitApiConstants.OVERRIDABLE, newValue);
            this.overridable = newValue;
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
