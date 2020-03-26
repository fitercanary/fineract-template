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
package org.apache.fineract.portfolio.validation.limit.serialization;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.validation.limit.api.ValidationLimitApiCollectionConstants;
import org.apache.fineract.portfolio.validation.limit.api.ValidationLimitApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

@Component
public final class ValidationLimitCommandFromApiJsonDeserializer {

    /**
     * The parameters supported for this command.
     */

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public ValidationLimitCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) { throw new InvalidJsonException(); }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                ValidationLimitApiCollectionConstants.VALIDATIONLIMIT_CREATE_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("validationLimit");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        if (this.fromApiJsonHelper.parameterExists(ValidationLimitApiConstants.clientLevelIdParamName, element)) {
            final Integer clientLevelId = this.fromApiJsonHelper
                    .extractIntegerSansLocaleNamed(ValidationLimitApiConstants.clientLevelIdParamName, element);
            baseDataValidator.reset().parameter(ValidationLimitApiConstants.clientLevelIdParamName).value(clientLevelId)
                    .integerGreaterThanZero();
        }

        final BigDecimal maximumSingleDepositAmount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(
                ValidationLimitApiConstants.maximumSingleDepositAmountParamName, element.getAsJsonObject());
        baseDataValidator.reset().parameter(ValidationLimitApiConstants.maximumSingleDepositAmountParamName)
                .value(maximumSingleDepositAmount).positiveAmount();

        final BigDecimal maximumCumulativeBalance = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(ValidationLimitApiConstants.maximumCumulativeBalanceParamName, element.getAsJsonObject());
        baseDataValidator.reset().parameter(ValidationLimitApiConstants.maximumCumulativeBalanceParamName).value(maximumCumulativeBalance)
                .positiveAmount();

        final BigDecimal maximumTransactionLimit = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(ValidationLimitApiConstants.maximumTransactionLimitParamName, element.getAsJsonObject());
        baseDataValidator.reset().parameter(ValidationLimitApiConstants.maximumTransactionLimitParamName).value(maximumTransactionLimit)
                .positiveAmount();

        final BigDecimal maximumDailyTransactionAmountLimit = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(
                ValidationLimitApiConstants.maximumDailyTransactionAmountLimitParamName, element.getAsJsonObject());
        baseDataValidator.reset().parameter(ValidationLimitApiConstants.maximumDailyTransactionAmountLimitParamName)
                .value(maximumDailyTransactionAmountLimit).positiveAmount();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForUpdate(final String json) {
        if (StringUtils.isBlank(json)) { throw new InvalidJsonException(); }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                ValidationLimitApiCollectionConstants.VALIDATIONLIMIT_UPDATE_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("validationLimit");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        if (this.fromApiJsonHelper.parameterExists(ValidationLimitApiConstants.clientLevelIdParamName, element)) {
            final Integer clientLevelId = this.fromApiJsonHelper
                    .extractIntegerSansLocaleNamed(ValidationLimitApiConstants.clientLevelIdParamName, element);
            baseDataValidator.reset().parameter(ValidationLimitApiConstants.clientLevelIdParamName).value(clientLevelId)
                    .integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(ValidationLimitApiConstants.maximumSingleDepositAmountParamName, element)) {
            final BigDecimal maximumSingleDepositAmount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(
                    ValidationLimitApiConstants.maximumSingleDepositAmountParamName, element.getAsJsonObject());
            baseDataValidator.reset().parameter(ValidationLimitApiConstants.maximumSingleDepositAmountParamName)
                    .value(maximumSingleDepositAmount).positiveAmount();
        }

        if (this.fromApiJsonHelper.parameterExists(ValidationLimitApiConstants.maximumCumulativeBalanceParamName, element)) {
            final BigDecimal maximumCumulativeBalance = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(
                    ValidationLimitApiConstants.maximumCumulativeBalanceParamName, element.getAsJsonObject());
            baseDataValidator.reset().parameter(ValidationLimitApiConstants.maximumCumulativeBalanceParamName)
                    .value(maximumCumulativeBalance).positiveAmount();
        }

        if (this.fromApiJsonHelper.parameterExists(ValidationLimitApiConstants.maximumTransactionLimitParamName, element)) {
            final BigDecimal maximumTransactionLimit = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(
                    ValidationLimitApiConstants.maximumTransactionLimitParamName, element.getAsJsonObject());
            baseDataValidator.reset().parameter(ValidationLimitApiConstants.maximumTransactionLimitParamName).value(maximumTransactionLimit)
                    .positiveAmount();
        }

        if (this.fromApiJsonHelper.parameterExists(ValidationLimitApiConstants.maximumDailyTransactionAmountLimitParamName, element)) {
            final BigDecimal maximumDailyTransactionAmountLimit = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(
                    ValidationLimitApiConstants.maximumDailyTransactionAmountLimitParamName, element.getAsJsonObject());
            baseDataValidator.reset().parameter(ValidationLimitApiConstants.maximumDailyTransactionAmountLimitParamName)
                    .value(maximumDailyTransactionAmountLimit).positiveAmount();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
    }
}