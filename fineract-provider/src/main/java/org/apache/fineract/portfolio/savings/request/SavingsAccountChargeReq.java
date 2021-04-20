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

package org.apache.fineract.portfolio.savings.request;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.joda.time.LocalDate;
import org.joda.time.MonthDay;

import java.math.BigDecimal;

import static org.apache.fineract.portfolio.savings.SavingsApiConstants.amountParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.dueAsOfDateParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.feeIntervalParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.feeOnMonthDayParamName;

public class SavingsAccountChargeReq {

    private BigDecimal amount;
    private LocalDate dueDate;
    private MonthDay feeOnMonthDay;
    private Integer feeInterval;
    private BigDecimal amountPercentAppliedTo;// used incase of % charges. Note amount is the percentage in that case


    public static SavingsAccountChargeReq instance(JsonCommand command) {
        SavingsAccountChargeReq instance = new SavingsAccountChargeReq();
        instance.amount = command.bigDecimalValueOfParameterNamed(amountParamName);
        instance.dueDate = command.localDateValueOfParameterNamed(dueAsOfDateParamName);
        instance.feeOnMonthDay = command.extractMonthDayNamed(feeOnMonthDayParamName);
        instance.feeInterval = command.integerValueOfParameterNamed(feeIntervalParamName);

        return instance;
    }

    public static SavingsAccountChargeReq instance(BigDecimal amount, LocalDate dueDate, MonthDay feeOnMonthDay, Integer feeInterval) {
        SavingsAccountChargeReq instance = new SavingsAccountChargeReq();
        instance.amount = amount;
        instance.dueDate = dueDate;
        instance.feeOnMonthDay = feeOnMonthDay;
        instance.feeInterval = feeInterval;

        return instance;
    }

    public static SavingsAccountChargeReq instance(BigDecimal amount, LocalDate dueDate, MonthDay feeOnMonthDay, Integer feeInterval, BigDecimal amountChargeToBeAppliedTo) {
        SavingsAccountChargeReq instance = new SavingsAccountChargeReq();
        instance.amount = amount;
        instance.dueDate = dueDate;
        instance.feeOnMonthDay = feeOnMonthDay;
        instance.feeInterval = feeInterval;
        instance.amountPercentAppliedTo = amountChargeToBeAppliedTo;

        return instance;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public MonthDay getFeeOnMonthDay() {
        return feeOnMonthDay;
    }

    public void setFeeOnMonthDay(MonthDay feeOnMonthDay) {
        this.feeOnMonthDay = feeOnMonthDay;
    }

    public Integer getFeeInterval() {
        return feeInterval;
    }

    public void setFeeInterval(Integer feeInterval) {
        this.feeInterval = feeInterval;
    }

    public BigDecimal getAmountPercentAppliedTo() {
        return amountPercentAppliedTo;
    }

    public void setAmountPercentAppliedTo(BigDecimal amountPercentAppliedTo) {
        this.amountPercentAppliedTo = amountPercentAppliedTo;
    }
}
