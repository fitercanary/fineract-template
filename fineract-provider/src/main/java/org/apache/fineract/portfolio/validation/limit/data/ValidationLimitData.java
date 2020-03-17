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

import org.apache.fineract.infrastructure.core.data.EnumOptionData;

/**
 * Immutable data object for charge data.
 */
public class ValidationLimitData implements Comparable<ValidationLimitData>, Serializable {

    private final Long id;
    private final Long clientLevelId;
    private final BigDecimal maximumSingleDepositAmount;
    private final BigDecimal maximumCumulativeBalance;
    private final BigDecimal maximumTransactionLimit;
    private final BigDecimal maximumDailyTransactionAmountLimit;
    private List<EnumOptionData> clientLevelOptions;

    private ValidationLimitData(final Long id, final Long clientLevelId, final BigDecimal maximumSingleDepositAmount,
            final BigDecimal maximumCumulativeBalance, final BigDecimal maximumTransactionLimit,
            final BigDecimal maximumDailyTransactionAmountLimit, List<EnumOptionData> clientLevelOptions) {
        this.id = id;
        this.clientLevelId = clientLevelId;
        this.maximumSingleDepositAmount = maximumSingleDepositAmount;
        this.maximumCumulativeBalance = maximumCumulativeBalance;
        this.maximumTransactionLimit = maximumTransactionLimit;
        this.maximumDailyTransactionAmountLimit = maximumDailyTransactionAmountLimit;
        this.clientLevelOptions = clientLevelOptions;

    }

    public static ValidationLimitData template(final List<EnumOptionData> clientLevelOptions) {

        return new ValidationLimitData(null, null, null, null, null, null, clientLevelOptions);
    }

    public static ValidationLimitData withTemplate(final ValidationLimitData limitData, final ValidationLimitData template) {
        return new ValidationLimitData(limitData.id, limitData.clientLevelId, limitData.maximumSingleDepositAmount,
                limitData.maximumCumulativeBalance, limitData.maximumTransactionLimit, limitData.maximumDailyTransactionAmountLimit,
                template.clientLevelOptions);
    }

    public static ValidationLimitData instance(final Long id, final Long clientLevelId, final BigDecimal maximumSingleDepositAmount,
            final BigDecimal maximumCumulativeBalance, final BigDecimal maximumTransactionLimit,
            final BigDecimal maximumDailyTransactionAmountLimit) {
        return new ValidationLimitData(id, clientLevelId, maximumSingleDepositAmount, maximumCumulativeBalance, maximumTransactionLimit,
                maximumDailyTransactionAmountLimit, null);
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
        if (obj == null) { return -1; }

        return obj.id.compareTo(this.id);
    }

}