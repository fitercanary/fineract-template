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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.client.domain.ClientLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

@Component
public final class ValidationLimitCommandFromApiJsonDeserializer {

    /**
     * The parameters supported for this command.
     */
    private final Set<String> supportedParameters = new HashSet<>(Arrays.asList("clientLevelId", "maximumSingleDepositAmount",
            "maximumCumulativeBalance", "maximumTransactionLimit", "maximumDailyTransactionAmountLimit", "locale"));

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public ValidationLimitCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) { throw new InvalidJsonException(); }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("validationLimit");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final Integer clientLevelId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed("clientLevelId", element);
        baseDataValidator.reset().parameter("clientLevelId").value(clientLevelId).notNull();
        if (clientLevelId != null) {
            baseDataValidator.reset().parameter("clientLevelId").value(clientLevelId).isOneOfTheseValues(ClientLevel.validValues());
        }

        final BigDecimal maximumSingleDepositAmount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed("maximumSingleDepositAmount",
                element.getAsJsonObject());
        baseDataValidator.reset().parameter("maximumSingleDepositAmount").value(maximumSingleDepositAmount).positiveAmount();

        final BigDecimal maximumCumulativeBalance = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed("maximumCumulativeBalance",
                element.getAsJsonObject());
        baseDataValidator.reset().parameter("maximumCumulativeBalance").value(maximumCumulativeBalance).positiveAmount();

        final BigDecimal maximumTransactionLimit = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed("maximumTransactionLimit",
                element.getAsJsonObject());
        baseDataValidator.reset().parameter("maximumTransactionLimit").value(maximumTransactionLimit).positiveAmount();

        final BigDecimal maximumDailyTransactionAmountLimit = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed("maximumDailyTransactionAmountLimit", element.getAsJsonObject());
        baseDataValidator.reset().parameter("maximumDailyTransactionAmountLimit").value(maximumDailyTransactionAmountLimit)
                .positiveAmount();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForUpdate(final String json) {
        if (StringUtils.isBlank(json)) { throw new InvalidJsonException(); }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("charge");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        if (this.fromApiJsonHelper.parameterExists("clientLevelId", element)) {
            final Integer clientLevelId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed("clientLevelId", element);
            baseDataValidator.reset().parameter("clientLevelId").value(clientLevelId).notNull();
            if (clientLevelId != null) {
                baseDataValidator.reset().parameter("clientLevelId").value(clientLevelId).isOneOfTheseValues(ClientLevel.validValues());
            }
        }

        if (this.fromApiJsonHelper.parameterExists("maximumSingleDepositAmount", element)) {
            final BigDecimal maximumSingleDepositAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed("maximumSingleDepositAmount", element.getAsJsonObject());
            baseDataValidator.reset().parameter("maximumSingleDepositAmount").value(maximumSingleDepositAmount).positiveAmount();
        }

        if (this.fromApiJsonHelper.parameterExists("maximumCumulativeBalance", element)) {
            final BigDecimal maximumCumulativeBalance = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed("maximumCumulativeBalance",
                    element.getAsJsonObject());
            baseDataValidator.reset().parameter("maximumCumulativeBalance").value(maximumCumulativeBalance).positiveAmount();
        }

        if (this.fromApiJsonHelper.parameterExists("maximumTransactionLimit", element)) {
            final BigDecimal maximumTransactionLimit = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed("maximumTransactionLimit",
                    element.getAsJsonObject());
            baseDataValidator.reset().parameter("maximumTransactionLimit").value(maximumTransactionLimit).positiveAmount();
        }

        if (this.fromApiJsonHelper.parameterExists("maximumDailyTransactionAmountLimit", element)) {
            final BigDecimal maximumDailyTransactionAmountLimit = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed("maximumDailyTransactionAmountLimit", element.getAsJsonObject());
            baseDataValidator.reset().parameter("maximumDailyTransactionAmountLimit").value(maximumDailyTransactionAmountLimit)
                    .positiveAmount();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
    }
}