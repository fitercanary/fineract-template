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

package org.apache.fineract.portfolio.account.data;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.account.AccountDetailConstants;
import org.apache.fineract.portfolio.account.api.AccountTransfersApiConstants;
import org.apache.fineract.portfolio.account.domain.BalanceAccountType;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BalanceVerificationDataValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private static final Set<String> REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(
            AccountDetailConstants.localeParamName, AccountDetailConstants.dateFormatParamName,
            AccountDetailConstants.verificationDateParamName, AccountDetailConstants.accountTypeParamName));

    @Autowired
    public BalanceVerificationDataValidator(FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validate(final JsonCommand command) {

        String json = command.json();

        if (StringUtils.isBlank(json)) { throw new InvalidJsonException(); }

        Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, REQUEST_DATA_PARAMETERS);

        List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(AccountTransfersApiConstants.ACCOUNT_BALANCE_RESOURCE_NAME);

        JsonElement element = command.parsedJson();

        LocalDate verificationDate = this.fromApiJsonHelper
                .extractLocalDateNamed(AccountDetailConstants.verificationDateParamName, element);
        baseDataValidator.reset().parameter(AccountDetailConstants.verificationDateParamName).value
                (verificationDate).notNull();

        String accountType = this.fromApiJsonHelper.extractStringNamed(AccountDetailConstants.accountTypeParamName, element);
        baseDataValidator.reset().parameter(AccountDetailConstants.accountTypeParamName).value
                (accountType).isOneOfTheseStringValues(Arrays.stream(BalanceAccountType.values()).map(BalanceAccountType::getCode).collect(Collectors.toList()));

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
    }
}
