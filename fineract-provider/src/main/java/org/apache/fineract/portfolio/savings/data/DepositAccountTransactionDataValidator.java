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
package org.apache.fineract.portfolio.savings.data;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.savings.DepositAccountOnClosureType;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.DepositsApiConstants;
import org.apache.fineract.portfolio.savings.SavingsApiConstants;
import org.apache.fineract.portfolio.savings.SavingsPeriodFrequencyType;
import org.apache.fineract.portfolio.savings.domain.FixedDepositAccount;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.fineract.portfolio.savings.DepositsApiConstants.activatedOnDateParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.bankNumberParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.checkNumberParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.closedOnDateParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.dateFormatParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.depositPeriodFrequencyIdParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.depositPeriodParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.onAccountClosureIdParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.paymentTypeIdParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.receiptNumberParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.routingCodeParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.toSavingsAccountIdParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.transactionAccountNumberParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.transactionAmountParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.transactionDateParamName;

@Component
public class DepositAccountTransactionDataValidator {

    private final FromJsonHelper fromApiJsonHelper;

	private static final Set<String> DEPOSIT_ACCOUNT_TRANSACTION_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(
			DepositsApiConstants.localeParamName, dateFormatParamName, transactionDateParamName,
			transactionAmountParamName, paymentTypeIdParamName, transactionAccountNumberParamName, checkNumberParamName,
			routingCodeParamName, receiptNumberParamName, bankNumberParamName));

	private static final Set<String> DEPOSIT_ACCOUNT_RECOMMENDED_DEPOSIT_AMOUNT_UPDATE_REQUEST_DATA_PARAMETERS = new HashSet<>(
			Arrays.asList(DepositsApiConstants.localeParamName, dateFormatParamName,
					DepositsApiConstants.mandatoryRecommendedDepositAmountParamName,
					DepositsApiConstants.effectiveDateParamName));

	private static final Set<String> DEPOSIT_ACCOUNT_CLOSE_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(
			DepositsApiConstants.localeParamName, dateFormatParamName, closedOnDateParamName,
			DepositsApiConstants.noteParamName, onAccountClosureIdParamName, paymentTypeIdParamName,
			transactionAccountNumberParamName, checkNumberParamName, routingCodeParamName, receiptNumberParamName,
			bankNumberParamName, DepositsApiConstants.transferDescriptionParamName, toSavingsAccountIdParamName));

	private static final Set<String> DEPOSIT_ACCOUNT_PRE_MATURE_CALCULATION_REQUEST_DATA_PARAMETERS = new HashSet<>(
			Arrays.asList(DepositsApiConstants.localeParamName, dateFormatParamName,
					closedOnDateParamName));

    private static final Set<String> DEPOSIT_ACCOUNT_PARTIAL_LIQUIDATION_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(
            DepositsApiConstants.localeParamName, DepositsApiConstants.dateFormatParamName, DepositsApiConstants.submittedOnDateParamName,
            DepositsApiConstants.noteParamName, DepositsApiConstants.depositPeriodParamName,
            DepositsApiConstants.depositPeriodFrequencyIdParamName, DepositsApiConstants.liquidationAmountParamName,DepositsApiConstants.interestRateParamName));

    private static final Set<String> DEPOSIT_ACCOUNT_TOP_UP_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(
            DepositsApiConstants.localeParamName, DepositsApiConstants.dateFormatParamName, DepositsApiConstants.submittedOnDateParamName,
            DepositsApiConstants.depositPeriodParamName, DepositsApiConstants.depositPeriodFrequencyIdParamName,
            DepositsApiConstants.depositAmountParamName, DepositsApiConstants.changeTenureParamName, DepositsApiConstants.interestRateParamName));

    private static final Set<String> MATURITY_NOTIFICATION_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(
            DepositsApiConstants.notificationTermIdParamName, DepositsApiConstants.notifyMaturityPeriodParamName,
            DepositsApiConstants.notifyAssetMaturityParamName, DepositsApiConstants.enableMaturitySmsAlertsParamName));

    private static final Set<String> DEPOSIT_ACCOUNT_RECOMMENDED_DEPOSIT_PERIOD_UPDATE_REQUEST_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList(DepositsApiConstants.localeParamName, DepositsApiConstants.depositPeriodParamName, DepositsApiConstants.depositPeriodFrequencyIdParamName));

    private static final Set<String> DEPOSIT_ACCOUNT_RECOMMENDED_DEPOSIT_PERIOD_FREQUENCY_UPDATE_REQUEST_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList(DepositsApiConstants.localeParamName, DepositsApiConstants.recurringFrequencyParamName, DepositsApiConstants.recurringFrequencyTypeParamName));


    @Autowired
    public DepositAccountTransactionDataValidator(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validate(final JsonCommand command, DepositAccountType depositAccountType) {

        final String json = command.json();

        validateJson(json);

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
		this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
				DEPOSIT_ACCOUNT_TRANSACTION_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(depositAccountType
                .resourceName());

        final JsonElement element = command.parsedJson();

        final LocalDate transactionDate = this.fromApiJsonHelper.extractLocalDateNamed(transactionDateParamName, element);
        baseDataValidator.reset().parameter(transactionDateParamName).value(transactionDate).notNull();

        final BigDecimal transactionAmount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(transactionAmountParamName, element);
        baseDataValidator.reset().parameter(transactionAmountParamName).value(transactionAmount).notNull().positiveAmount();

        // Validate all string payment detail fields for max length
        final Integer paymentTypeId = this.fromApiJsonHelper.extractIntegerWithLocaleNamed(paymentTypeIdParamName, element);
        baseDataValidator.reset().parameter(paymentTypeIdParamName).value(paymentTypeId).ignoreIfNull().integerGreaterThanZero();
        final Set<String> paymentDetailParameters = new HashSet<>(Arrays.asList(transactionAccountNumberParamName, checkNumberParamName,
                routingCodeParamName, receiptNumberParamName, bankNumberParamName));
        for (final String paymentDetailParameterName : paymentDetailParameters) {
            final String paymentDetailParameterValue = this.fromApiJsonHelper.extractStringNamed(paymentDetailParameterName, element);
            baseDataValidator.reset().parameter(paymentDetailParameterName).value(paymentDetailParameterValue).ignoreIfNull()
                    .notExceedingLengthOf(50);
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateDepositAmountUpdate(final JsonCommand command) {
        final String json = command.json();

        validateJson(json);

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
		this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
				DEPOSIT_ACCOUNT_RECOMMENDED_DEPOSIT_AMOUNT_UPDATE_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME);

        final JsonElement element = command.parsedJson();

        final LocalDate effectiveDate = this.fromApiJsonHelper.extractLocalDateNamed(DepositsApiConstants.effectiveDateParamName, element);
        baseDataValidator.reset().parameter(DepositsApiConstants.effectiveDateParamName).value(effectiveDate).notNull();

        final BigDecimal mandatoryRecommendedDepositAmount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(
                DepositsApiConstants.mandatoryRecommendedDepositAmountParamName, element);
        baseDataValidator.reset().parameter(DepositsApiConstants.mandatoryRecommendedDepositAmountParamName)
                .value(mandatoryRecommendedDepositAmount).notNull().positiveAmount();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateActivation(final JsonCommand command) {
        final String json = command.json();

        validateJson(json);

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                SavingsAccountConstant.SAVINGS_ACCOUNT_ACTIVATION_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME);

        final JsonElement element = command.parsedJson();

        final LocalDate activationDate = this.fromApiJsonHelper.extractLocalDateNamed(activatedOnDateParamName, element);
        baseDataValidator.reset().parameter(activatedOnDateParamName).value(activationDate).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validatePreMatureAmountCalculation(final String json, final DepositAccountType depositAccountType) {
        validateJson(json);
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
		this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
				DEPOSIT_ACCOUNT_PRE_MATURE_CALCULATION_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(depositAccountType
                .resourceName());

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final LocalDate closeDate = this.fromApiJsonHelper.extractLocalDateNamed(closedOnDateParamName, element);
        baseDataValidator.reset().parameter(closedOnDateParamName).value(closeDate).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateClosing(final JsonCommand command, DepositAccountType depositAccountType, final boolean isPreMatureClose) {
        final String json = command.json();
        validateJson(json);
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
		this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
				DEPOSIT_ACCOUNT_CLOSE_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(depositAccountType
                .resourceName());

        final JsonElement element = command.parsedJson();

        final LocalDate closureDate = this.fromApiJsonHelper.extractLocalDateNamed(closedOnDateParamName, element);
        baseDataValidator.reset().parameter(closedOnDateParamName).value(closureDate).notNull();

        final Integer onAccountClosureId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(onAccountClosureIdParamName, element);
        baseDataValidator.reset().parameter(onAccountClosureIdParamName).value(onAccountClosureId).notBlank()
                .isOneOfTheseValues(DepositAccountOnClosureType.integerValues());

        if (onAccountClosureId != null) {
            final DepositAccountOnClosureType accountOnClosureType = DepositAccountOnClosureType.fromInt(onAccountClosureId);
            if (accountOnClosureType.isTransferToSavings()) {
                final Long toSavingsAccountId = this.fromApiJsonHelper.extractLongNamed(toSavingsAccountIdParamName, element);
                baseDataValidator
                        .reset()
                        .parameter(toSavingsAccountIdParamName)
                        .value(toSavingsAccountId)
                        .cantBeBlankWhenParameterProvidedIs(onAccountClosureIdParamName,
                                DepositAccountOnClosureType.fromInt(onAccountClosureId).getCode());
            } else if (accountOnClosureType.isReinvest() && isPreMatureClose) {
                baseDataValidator.reset().parameter(onAccountClosureIdParamName).value(onAccountClosureId)
                        .failWithCode("reinvest.not.allowed", "Re-Invest is not supported for account pre mature close");
            }
        }

        // Validate all string payment detail fields for max length
        final Integer paymentTypeId = this.fromApiJsonHelper.extractIntegerWithLocaleNamed(paymentTypeIdParamName, element);
        baseDataValidator.reset().parameter(paymentTypeIdParamName).value(paymentTypeId).ignoreIfNull().integerGreaterThanZero();
        final Set<String> paymentDetailParameters = new HashSet<>(Arrays.asList(transactionAccountNumberParamName, checkNumberParamName,
                routingCodeParamName, receiptNumberParamName, bankNumberParamName));
        for (final String paymentDetailParameterName : paymentDetailParameters) {
            final String paymentDetailParameterValue = this.fromApiJsonHelper.extractStringNamed(paymentDetailParameterName, element);
            baseDataValidator.reset().parameter(paymentDetailParameterName).value(paymentDetailParameterValue).ignoreIfNull()
                    .notExceedingLengthOf(50);
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void checkForUnsupportedParameters(JsonCommand command, Set<String> supportedParams) {
        String json = command.json();
        validateJson(json);
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, supportedParams);
    }

    public void validatePartialLiquidation(FixedDepositAccount account, JsonCommand command) {
        final JsonElement element = command.parsedJson();
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(DepositAccountType.FIXED_DEPOSIT
                .resourceName());
        this.checkForUnsupportedParameters(command, DEPOSIT_ACCOUNT_PARTIAL_LIQUIDATION_REQUEST_DATA_PARAMETERS);
        this.validateDepositPeriod(baseDataValidator, element);

        final BigDecimal liquidationAmount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(DepositsApiConstants.liquidationAmountParamName, element);
        baseDataValidator.reset().parameter(DepositsApiConstants.liquidationAmountParamName).value(liquidationAmount).notLessThanMin(BigDecimal.ONE).notNull();

        if (liquidationAmount.compareTo(account.getAccountTermAndPreClosure().maturityAmount()) > -1) {
            baseDataValidator.reset().parameter(DepositsApiConstants.liquidationAmountParamName)
                    .failWithCode("liquidation.amount.must.be.less.than.maturity.amount");
        }

        final BigDecimal interestRate = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(DepositsApiConstants.interestRateParamName, element);
        baseDataValidator.reset().parameter(DepositsApiConstants.interestRateParamName).value(interestRate).notLessThanMin(BigDecimal.ONE).notNull();


        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateTopUp(JsonCommand command) {
        final JsonElement element = command.parsedJson();
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(DepositAccountType.FIXED_DEPOSIT
                .resourceName());
        this.checkForUnsupportedParameters(command, DEPOSIT_ACCOUNT_TOP_UP_REQUEST_DATA_PARAMETERS);
        this.validateDepositPeriod(baseDataValidator, element);

        final BigDecimal depositAmount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(DepositsApiConstants.depositAmountParamName, element);
        baseDataValidator.reset().parameter(DepositsApiConstants.depositAmountParamName).value(depositAmount).notLessThanMin(BigDecimal.ONE).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    /**
     * validate notification maturity data parameters
     * @param command
     */
    public void validateNotificationMaturity(DataValidatorBuilder baseDataValidator,JsonCommand command) {
        this.checkForUnsupportedParameters(command, MATURITY_NOTIFICATION_REQUEST_DATA_PARAMETERS);
    }

    private void validateDepositPeriod(DataValidatorBuilder baseDataValidator, JsonElement element) {
        final LocalDate submitDate = this.fromApiJsonHelper.extractLocalDateNamed(DepositsApiConstants.submittedOnDateParamName, element);
        baseDataValidator.reset().parameter(DepositsApiConstants.submittedOnDateParamName).value(submitDate).notNull();

        if (this.fromApiJsonHelper.parameterExists(depositPeriodParamName, element)) {
            final Integer depositPeriod = fromApiJsonHelper.extractIntegerSansLocaleNamed(depositPeriodParamName, element);
            baseDataValidator.reset().parameter(depositPeriodParamName).value(depositPeriod).notNull().integerGreaterThanZero();
        }

        final Integer depositPeriodFrequencyId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(depositPeriodFrequencyIdParamName,
                element);
        baseDataValidator.reset().parameter(depositPeriodFrequencyIdParamName).value(depositPeriodFrequencyId)
                .isOneOfTheseValues(SavingsPeriodFrequencyType.integerValues());
    }

    private void validateJson(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            //
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }

    public void validateDepositPeriodUpdate(final JsonCommand command) {
        final String json = command.json();

        validateJson(json);

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                DEPOSIT_ACCOUNT_RECOMMENDED_DEPOSIT_PERIOD_UPDATE_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME);

        final JsonElement element = command.parsedJson();

        final Integer depositPeriod = this.fromApiJsonHelper.extractIntegerWithLocaleNamed(DepositsApiConstants.depositPeriodParamName,
                element);
        baseDataValidator.reset().parameter(DepositsApiConstants.depositPeriodParamName).value(depositPeriod).notNull().positiveAmount();

        final Integer depositPeriodFrequencyId = this.fromApiJsonHelper.extractIntegerWithLocaleNamed(DepositsApiConstants.depositPeriodFrequencyIdParamName,
                element);
        baseDataValidator.reset().parameter(DepositsApiConstants.depositPeriodFrequencyIdParamName).value(depositPeriodFrequencyId)
        .isOneOfTheseValues(SavingsPeriodFrequencyType.integerValues());
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }
    
    public void validateDepositPeriodFrequencyUpdate(final JsonCommand command) {
        final String json = command.json();

        validateJson(json);

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                DEPOSIT_ACCOUNT_RECOMMENDED_DEPOSIT_PERIOD_FREQUENCY_UPDATE_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME);

        final JsonElement element = command.parsedJson();

        final Integer recurringFrequency = this.fromApiJsonHelper.extractIntegerWithLocaleNamed(DepositsApiConstants.recurringFrequencyParamName,
                element);
        baseDataValidator.reset().parameter(DepositsApiConstants.recurringFrequencyParamName).value(recurringFrequency).notNull().positiveAmount();

        final Integer recurringFrequencyId = this.fromApiJsonHelper.extractIntegerWithLocaleNamed(DepositsApiConstants.recurringFrequencyTypeParamName,
                element);
        baseDataValidator.reset().parameter(DepositsApiConstants.recurringFrequencyTypeParamName).value(recurringFrequencyId)
        .isOneOfTheseValues(SavingsPeriodFrequencyType.integerValues());
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }
}
