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
package org.apache.fineract.portfolio.savings.domain;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.portfolio.savings.PreClosurePenalInterestOnType;
import org.apache.fineract.portfolio.savings.service.SavingsEnumerations;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.fineract.portfolio.savings.DepositsApiConstants.preClosureChargeApplicableParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.preClosurePenalApplicableParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.preClosurePenalInterestOnTypeIdParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.preClosurePenalInterestParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.localeParamName;

/**
 * DepositPreClosureDetail encapsulates all the details of a
 * {@link FixedDepositProduct} that are also used and persisted by a
 * {@link FixedDepositAccount}.
 */
@Embeddable
public class DepositPreClosureDetail {

    @Column(name = "pre_closure_penal_applicable")
    private boolean preClosurePenalApplicable;

    @Column(name = "pre_closure_penal_interest", scale = 6, precision = 19)
    private BigDecimal preClosurePenalInterest;

    @Column(name = "pre_closure_penal_interest_on_enum")
    private Integer preClosurePenalInterestOnType;

    @Column(name = "pre_closure_charge_applicable")
    private boolean preClosureChargeApplicable;

    public static class DepositPreClosureDetailBuilder {
        private boolean preClosurePenalApplicable;
        private BigDecimal preClosurePenalInterest;
        private Integer preClosurePenalInterestOnType;
        private boolean preClosureChargeApplicable;

        public DepositPreClosureDetailBuilder preClosurePenalApplicable(boolean preClosurePenalApplicable) {
            this.preClosurePenalApplicable = preClosurePenalApplicable;
            return this;
        }

        public DepositPreClosureDetailBuilder preClosurePenalInterest(BigDecimal preClosurePenalInterest) {
            this.preClosurePenalInterest = preClosurePenalInterest;
            return this;
        }

        public DepositPreClosureDetailBuilder preClosurePenalInterestOnType(Integer preClosurePenalInterestOnType) {
            PreClosurePenalInterestOnType preClosurePenalInterestType = PreClosurePenalInterestOnType.fromInt(this.preClosurePenalInterestOnType);
            this.preClosurePenalInterestOnType = preClosurePenalInterestType.isInvalid() ? null : preClosurePenalInterestOnType;
            return this;
        }

        public DepositPreClosureDetailBuilder preClosureChargeApplicable(boolean preClosureChargeApplicable) {
            this.preClosureChargeApplicable = preClosureChargeApplicable;
            return this;
        }

        public DepositPreClosureDetail build() {
            return new DepositPreClosureDetail(this);
        }
    }

    public DepositPreClosureDetail(DepositPreClosureDetailBuilder builder) {
        this.preClosurePenalApplicable = builder.preClosurePenalApplicable;
        this.preClosurePenalInterest = builder.preClosurePenalInterest;
        this.preClosurePenalInterestOnType = builder.preClosurePenalInterestOnType;
        this.preClosureChargeApplicable = builder.preClosureChargeApplicable;
    }

    protected DepositPreClosureDetail() {
        //Keeping this here as it's needed by the ORM
    }

    protected Map<String, Object> update(final JsonCommand command, final DataValidatorBuilder baseDataValidator) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(10);
        this.updatePenalInterestChanges(command, baseDataValidator, actualChanges);
        this.updatePenalChargeChanges(command, actualChanges);
        return actualChanges;
    }

    private void updatePenalInterestChanges(JsonCommand command, DataValidatorBuilder baseDataValidator, Map<String, Object> actualChanges) {
        final String localeAsInput = command.locale();
        if (command.isChangeInBooleanParameterNamed(preClosurePenalApplicableParamName, this.preClosurePenalApplicable)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(preClosurePenalApplicableParamName);
            actualChanges.put(preClosurePenalApplicableParamName, newValue);
            this.preClosurePenalApplicable = newValue;
        }
        if (this.preClosurePenalApplicable) {
            if (command.isChangeInBigDecimalParameterNamed(preClosurePenalInterestParamName, this.preClosurePenalInterest)) {
                final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(preClosurePenalInterestParamName);
                actualChanges.put(preClosurePenalInterestParamName, newValue);
                actualChanges.put(localeParamName, localeAsInput);
                this.preClosurePenalInterest = newValue;
            }
            if (command.isChangeInIntegerParameterNamed(preClosurePenalInterestOnTypeIdParamName, this.preClosurePenalInterestOnType)) {
                final Integer newValue = command.integerValueOfParameterNamed(preClosurePenalInterestOnTypeIdParamName);
                actualChanges.put(preClosurePenalInterestOnTypeIdParamName, SavingsEnumerations.preClosurePenaltyInterestOnType(newValue));
                actualChanges.put(localeParamName, localeAsInput);
                this.preClosurePenalInterestOnType = newValue;
            }
            if (this.preClosurePenalInterest == null) {
                baseDataValidator.parameter(preClosurePenalInterestParamName).value(this.preClosurePenalInterest)
                        .cantBeBlankWhenParameterProvidedIs(preClosurePenalApplicableParamName, this.preClosurePenalApplicable);
            }
            if (this.preClosurePenalInterestOnType == null) {
                baseDataValidator.parameter(preClosurePenalInterestOnTypeIdParamName).value(this.preClosurePenalInterestOnType)
                        .cantBeBlankWhenParameterProvidedIs(preClosurePenalApplicableParamName, this.preClosurePenalApplicable);
            }
        } else {
            this.preClosurePenalInterest = null;
            this.preClosurePenalInterestOnType = null;
        }
    }

    private void updatePenalChargeChanges(JsonCommand command, Map<String, Object> actualChanges) {
        if (command.isChangeInBooleanParameterNamed(preClosureChargeApplicableParamName, this.preClosureChargeApplicable)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(preClosureChargeApplicableParamName);
            actualChanges.put(preClosureChargeApplicableParamName, newValue);
            this.preClosureChargeApplicable = newValue;
        }
    }

    public boolean isPreClosurePenalApplicable() {
        return this.preClosurePenalApplicable;
    }

    public boolean isPreClosureChargeApplicable() {
        return preClosureChargeApplicable;
    }

    public BigDecimal getPreClosurePenalInterest() {
        return this.preClosurePenalInterest;
    }

    public Integer getPreClosurePenalInterestOnType() {
        return this.preClosurePenalInterestOnType;
    }

    public PreClosurePenalInterestOnType preClosurePenalInterestOnType() {
        return PreClosurePenalInterestOnType.fromInt(preClosurePenalInterestOnType);
    }

    public DepositPreClosureDetail copy() {
        return new DepositPreClosureDetailBuilder()
                .preClosurePenalApplicable(this.preClosurePenalApplicable)
                .preClosurePenalInterest(this.preClosurePenalInterest)
                .preClosurePenalInterestOnType(this.preClosurePenalInterestOnType)
                .preClosureChargeApplicable(this.preClosureChargeApplicable)
                .build();
    }
}