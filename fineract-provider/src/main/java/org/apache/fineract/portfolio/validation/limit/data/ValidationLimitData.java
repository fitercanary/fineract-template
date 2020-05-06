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
package org.apache.fineract.portfolio.validation.limit.data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import org.apache.fineract.infrastructure.codes.data.CodeValueData;

/**
 * Immutable data object for charge data.
 */
public class ValidationLimitData implements Comparable<ValidationLimitData>, Serializable {

    private final Long id;
    private final CodeValueData clientLevel;
    private final BigDecimal maximumSingleDepositAmount;
    private final BigDecimal maximumCumulativeBalance;
    private final BigDecimal maximumTransactionLimit;
    private final BigDecimal maximumDailyTransactionAmountLimit;
    private List<CodeValueData> clientLevelOptions;
    private final Boolean overridable;

    private ValidationLimitData(final Long id, final CodeValueData clientLevel, final BigDecimal maximumSingleDepositAmount,
                                final BigDecimal maximumCumulativeBalance, final BigDecimal maximumTransactionLimit,
                                final BigDecimal maximumDailyTransactionAmountLimit, List<CodeValueData> clientLevelOptions, Boolean overridable) {
        this.id = id;
        this.clientLevel = clientLevel;
        this.maximumSingleDepositAmount = maximumSingleDepositAmount;
        this.maximumCumulativeBalance = maximumCumulativeBalance;
        this.maximumTransactionLimit = maximumTransactionLimit;
        this.maximumDailyTransactionAmountLimit = maximumDailyTransactionAmountLimit;
        this.clientLevelOptions = clientLevelOptions;
        this.overridable = overridable;

    }

    public static ValidationLimitData template(final List<CodeValueData> clientLevelOptions) {

        return new ValidationLimitData(null, null, null, null, null, null, clientLevelOptions, null);
    }

    public static ValidationLimitData withTemplate(final ValidationLimitData limitData, final ValidationLimitData template) {
        return new ValidationLimitData(limitData.id, limitData.clientLevel, limitData.maximumSingleDepositAmount,
                limitData.maximumCumulativeBalance, limitData.maximumTransactionLimit, limitData.maximumDailyTransactionAmountLimit,
                template.clientLevelOptions, limitData.overridable);
    }

    public static ValidationLimitData instance(final Long id, final CodeValueData clientLevel, final BigDecimal maximumSingleDepositAmount,
                                               final BigDecimal maximumCumulativeBalance, final BigDecimal maximumTransactionLimit,
                                               final BigDecimal maximumDailyTransactionAmountLimit, Boolean overridable) {
        return new ValidationLimitData(id, clientLevel, maximumSingleDepositAmount, maximumCumulativeBalance, maximumTransactionLimit,
                maximumDailyTransactionAmountLimit, null, overridable);
    }

    @Override
    public boolean equals(final Object obj) {
        final ValidationLimitData validationLimitData = (ValidationLimitData) obj;
        return this.id.equals(validationLimitData.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public int compareTo(final ValidationLimitData obj) {
        if (obj == null) {
            return -1;
        }

        return obj.id.compareTo(this.id);
    }

}