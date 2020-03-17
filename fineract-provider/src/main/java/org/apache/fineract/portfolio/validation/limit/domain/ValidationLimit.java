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
import javax.persistence.Table;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;

@Entity
@Table(name = "m_validation_limits")
public class ValidationLimit extends AbstractPersistableCustom<Long> {

    @Column(name = "client_level_id")
    private Long clientLevelId;

    @Column(name = "maximum_single_deposit_amount")
    private BigDecimal maximumSingleDepositAmount;

    @Column(name = "maximum_cumulative_balance")
    private BigDecimal maximumCumulativeBalance;

    @Column(name = "maximum_transaction_limit")
    private BigDecimal maximumTransactionLimit;

    @Column(name = "maximum_daily_transaction_amount_limit")
    private BigDecimal maximumDailyTransactionAmountLimit;

    private ValidationLimit(final Long clientLevelId, final BigDecimal maximumSingleDepositAmount,
            final BigDecimal maximumCumulativeBalance, final BigDecimal maximumTransactionLimit,
            final BigDecimal maximumDailyTransactionAmountLimit) {
        this.clientLevelId = clientLevelId;
        this.maximumSingleDepositAmount = maximumSingleDepositAmount;
        this.maximumCumulativeBalance = maximumCumulativeBalance;
        this.maximumTransactionLimit = maximumTransactionLimit;
        this.maximumDailyTransactionAmountLimit = maximumDailyTransactionAmountLimit;

    }

    public static ValidationLimit fromJson(final JsonCommand command) {
        final Long clientLevelId = command.longValueOfParameterNamed("clientLevelId");
        final BigDecimal maximumSingleDepositAmount = command.bigDecimalValueOfParameterNamed("maximumSingleDepositAmount");
        final BigDecimal maximumCumulativeBalance = command.bigDecimalValueOfParameterNamed("maximumCumulativeBalance");
        final BigDecimal maximumTransactionLimit = command.bigDecimalValueOfParameterNamed("maximumTransactionLimit");
        final BigDecimal maximumDailyTransactionAmountLimit = command.bigDecimalValueOfParameterNamed("maximumDailyTransactionAmountLimit");
        return new ValidationLimit(clientLevelId, maximumSingleDepositAmount, maximumCumulativeBalance, maximumTransactionLimit,
                maximumDailyTransactionAmountLimit);
    }

    public Long getClientLevelId() {
        return this.clientLevelId;
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

        final String clientLevelParamName = "clientLevelId";
        if (command.isChangeInLongParameterNamed(clientLevelParamName, this.clientLevelId)) {
            final Long newValue = command.longValueOfParameterNamed(clientLevelParamName);
            actualChanges.put(clientLevelParamName, newValue);
            this.clientLevelId = newValue;
        }

        final String maximumSingleDepositAmountParamName = "maximumSingleDepositAmount";
        if (command.isChangeInBigDecimalParameterNamed(maximumSingleDepositAmountParamName, this.maximumSingleDepositAmount)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(maximumSingleDepositAmountParamName);
            actualChanges.put(maximumSingleDepositAmountParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            this.maximumSingleDepositAmount = newValue;
        }

        final String maximumCumulativeBalanceParamName = "maximumCumulativeBalance";
        if (command.isChangeInBigDecimalParameterNamed(maximumCumulativeBalanceParamName, this.maximumCumulativeBalance)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(maximumCumulativeBalanceParamName);
            actualChanges.put(maximumCumulativeBalanceParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            this.maximumCumulativeBalance = newValue;
        }

        final String maximumTransactionLimitParamName = "maximumTransactionLimit";
        if (command.isChangeInBigDecimalParameterNamed(maximumTransactionLimitParamName, this.maximumTransactionLimit)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(maximumTransactionLimitParamName);
            actualChanges.put(maximumTransactionLimitParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            this.maximumTransactionLimit = newValue;
        }

        final String maximumDailyTransactionAmountLimitParamName = "maximumDailyTransactionAmountLimit";
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

}
