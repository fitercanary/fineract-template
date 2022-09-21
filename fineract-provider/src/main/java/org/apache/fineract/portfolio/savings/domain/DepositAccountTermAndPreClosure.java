/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.savings.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.savings.DepositAccountOnClosureType;
import org.apache.fineract.portfolio.savings.SavingsPeriodFrequencyType;
import org.apache.fineract.portfolio.savings.service.SavingsEnumerations;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.joda.time.Weeks;
import org.joda.time.Years;

import static org.apache.fineract.portfolio.savings.DepositsApiConstants.*;

@Entity
@Table(name = "m_deposit_account_term_and_preclosure")
public class DepositAccountTermAndPreClosure extends AbstractPersistableCustom<Long> {

    @Column(name = "deposit_amount", scale = 6, precision = 19)
    private BigDecimal depositAmount;

    @Column(name = "maturity_amount", scale = 6, precision = 19)
    private BigDecimal maturityAmount;

    @Temporal(TemporalType.DATE)
    @Column(name = "maturity_date")
    private Date maturityDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "expected_firstdepositon_date")
    private Date expectedFirstDepositOnDate;

    @Column(name = "deposit_period")
    private Integer depositPeriod;

    @Column(name = "deposit_period_frequency_enum")
    private Integer depositPeriodFrequency;

    @Column(name = "on_account_closure_enum")
    private Integer onAccountClosureType;

    @Column(name = "maturity_notification_period")
    private Integer maturityNotificationPeriod;

    @Column(name = "maturity_sms_notification")
    private boolean maturitySmsNotification;

    @Column(name = "maturity_notification_frequency")
    private Integer maturityNotificationFrequency;

    @Column(name = "next_maturity_notification_date")
    private Date nextMaturityNotificationDate;

    @Embedded
    private DepositPreClosureDetail preClosureDetail;

    @Embedded
    protected DepositTermDetail depositTermDetail;

    @OneToOne
    @JoinColumn(name = "savings_account_id", nullable = false)
    private SavingsAccount account;

    @Column(name = "transfer_interest_to_linked_account", nullable = false)
    private boolean transferInterestToLinkedAccount;

    @Column(name = "interest_carried_forward_on_top_up", scale = 6, precision = 19)
    private BigDecimal interestCarriedForwardOnTopUp;

    @Column(name = "target_amount", scale = 6, precision = 19)
    private BigDecimal targetAmount;

    @Column(name = "target_maturity_amount", scale = 6, precision = 19)
    private BigDecimal targetMaturityAmount;

    protected DepositAccountTermAndPreClosure() {
        super();
    }

    public static DepositAccountTermAndPreClosure createNew(
            DepositPreClosureDetail preClosureDetail, DepositTermDetail depositTermDetail,
            SavingsAccount account, BigDecimal depositAmount, BigDecimal maturityAmount, final LocalDate maturityDate,
            Integer depositPeriod, final SavingsPeriodFrequencyType depositPeriodFrequency, final LocalDate expectedFirstDepositOnDate,
            final DepositAccountOnClosureType accountOnClosureType, Boolean transferInterest, BigDecimal interestCarriedForwardOnTopUp,
            Integer maturityPeriod, SavingsPeriodFrequencyType maturityPeriodFrequency,Boolean enableMaturitySmsAlerts) {

        return new DepositAccountTermAndPreClosure(
                preClosureDetail, depositTermDetail, account, depositAmount, maturityAmount, maturityDate, depositPeriod,
                depositPeriodFrequency, expectedFirstDepositOnDate, accountOnClosureType, transferInterest,
                interestCarriedForwardOnTopUp, maturityPeriod, maturityPeriodFrequency,enableMaturitySmsAlerts);
    }

    private DepositAccountTermAndPreClosure(
            DepositPreClosureDetail preClosureDetail, DepositTermDetail depositTermDetail,
            SavingsAccount account, BigDecimal depositAmount, BigDecimal maturityAmount, final LocalDate maturityDate,
            Integer depositPeriod, final SavingsPeriodFrequencyType depositPeriodFrequency,
            final LocalDate expectedFirstDepositOnDate,final DepositAccountOnClosureType accountOnClosureType,
            Boolean transferInterest, BigDecimal interestCarriedForwardOnTopUp,
            Integer maturityPeriod,final SavingsPeriodFrequencyType maturityPeriodFrequency, Boolean enableMaturitySmsAlerts) {
        this.depositAmount = depositAmount;
        this.maturityAmount = maturityAmount;
        this.maturityDate = (maturityDate == null) ? null : maturityDate.toDate();
        this.depositPeriod = depositPeriod;
        this.depositPeriodFrequency = (depositPeriodFrequency == null) ? null : depositPeriodFrequency.getValue();
        this.preClosureDetail = preClosureDetail;
        this.depositTermDetail = depositTermDetail;
        this.account = account;
        this.expectedFirstDepositOnDate = expectedFirstDepositOnDate == null ? null : expectedFirstDepositOnDate.toDate();
        this.onAccountClosureType = (accountOnClosureType == null) ? null : accountOnClosureType.getValue();
        this.transferInterestToLinkedAccount = transferInterest;
        this.interestCarriedForwardOnTopUp = interestCarriedForwardOnTopUp;
        this.maturitySmsNotification = enableMaturitySmsAlerts;
        this.maturityNotificationPeriod = maturityPeriod;
        this.setMaturityNotificationFrequency((maturityPeriodFrequency == null) ?
                null : maturityPeriodFrequency.getValue());

    }

    public Map<String, Object> update(final JsonCommand command, final DataValidatorBuilder baseDataValidator) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(10);

        if (command.isChangeInBigDecimalParameterNamed(depositAmountParamName, this.depositAmount)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(depositAmountParamName);
            actualChanges.put(depositAmountParamName, newValue);
            this.depositAmount = newValue;
        }

        if (command.isChangeInIntegerParameterNamed(depositPeriodParamName, this.depositPeriod)) {
            final Integer newValue = command.integerValueOfParameterNamed(depositPeriodParamName);
            actualChanges.put(depositPeriodParamName, newValue);
            this.depositPeriod = newValue;
        }

        if (command.isChangeInIntegerParameterNamed(depositPeriodFrequencyIdParamName, this.depositPeriodFrequency)) {
            final Integer newValue = command.integerValueOfParameterNamed(depositPeriodFrequencyIdParamName);
            actualChanges.put(depositPeriodFrequencyIdParamName, SavingsEnumerations.depositTermFrequencyType(newValue));
            this.depositPeriodFrequency = newValue;
        }

        final String localeAsInput = command.locale();
        final String dateFormat = command.dateFormat();
        if (command.isChangeInLocalDateParameterNamed(expectedFirstDepositOnDateParamName, this.getExpectedFirstDepositOnDate())) {
            final LocalDate newValue = command.localDateValueOfParameterNamed(expectedFirstDepositOnDateParamName);
            final String newValueAsString = command.stringValueOfParameterNamed(expectedFirstDepositOnDateParamName);
            actualChanges.put(expectedFirstDepositOnDateParamName, newValueAsString);
            actualChanges.put(localeParamName, localeAsInput);
            actualChanges.put(dateFormatParamName, dateFormat);
            this.expectedFirstDepositOnDate = newValue.toDate();
        }

        if (command.isChangeInBooleanParameterNamed(transferInterestToSavingsParamName, this.transferInterestToLinkedAccount)) {
            final Boolean newValue = command.booleanPrimitiveValueOfParameterNamed(transferInterestToSavingsParamName);
            actualChanges.put(transferInterestToSavingsParamName, newValue);
            this.transferInterestToLinkedAccount = newValue;
        }

        if (this.preClosureDetail != null) {
            actualChanges.putAll(this.preClosureDetail.update(command, baseDataValidator));
        }

        if (this.depositTermDetail != null) {
            actualChanges.putAll(this.depositTermDetail.update(command, baseDataValidator));
        }


        if (command.isChangeInIntegerParameterNamed(notifyMaturityPeriodParamName, this.maturityNotificationPeriod)) {
            final Integer newValue = command.integerValueOfParameterNamed(notifyMaturityPeriodParamName);
            actualChanges.put(notifyMaturityPeriodParamName, newValue);
            this.maturityNotificationPeriod = newValue;
        }
        if (command.isChangeInBooleanParameterNamed(enableMaturitySmsAlertsParamName, this.maturitySmsNotification)) {
            System.out.println("updating maturity notification");
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(enableMaturitySmsAlertsParamName);
            actualChanges.put(enableMaturitySmsAlertsParamName, newValue);
            this.maturitySmsNotification = newValue;
        }

        if (command.isChangeInIntegerParameterNamed(notificationTermIdParamName, this.getMaturityNotificationFrequency())) {
            final Integer newValue = command.integerValueOfParameterNamed(notificationTermIdParamName);
            actualChanges.put(notificationTermIdParamName, SavingsEnumerations.depositTermFrequencyType(newValue));
            this.setMaturityNotificationFrequency(newValue);
        }

        return actualChanges;
    }

    public DepositPreClosureDetail depositPreClosureDetail() {
        return this.preClosureDetail;
    }

    public DepositTermDetail depositTermDetail() {
        return this.depositTermDetail;
    }

    public BigDecimal depositAmount() {
        return this.depositAmount;
    }

    public Integer depositPeriod() {
        return this.depositPeriod;
    }

    public Integer depositPeriodFrequency() {
        return this.depositPeriodFrequency;
    }

    public SavingsPeriodFrequencyType depositPeriodFrequencyType() {
        return SavingsPeriodFrequencyType.fromInt(depositPeriodFrequency);
    }

    public SavingsPeriodFrequencyType maturityNotificationPeriodFrequencyType() {
        return SavingsPeriodFrequencyType.fromInt(getMaturityNotificationFrequency());
    }

    public void updateAccountReference(final SavingsAccount account) {
        this.account = account;
    }

    public void updateMaturityDetails(final BigDecimal maturityAmount, final LocalDate maturityDate) {
        this.maturityAmount = maturityAmount;
        this.maturityDate = maturityDate.toDate();
    }

    public void updateMaturityDetails(final BigDecimal depositAmount, final BigDecimal interestPayable,
                                      final LocalDate maturityDate) {
        this.depositAmount = depositAmount;
        this.maturityAmount = this.depositAmount.add(interestPayable);
        this.maturityDate = maturityDate.toDate();
    }

    public void updateMaturityNotificationDate(final LocalDate nextNotificationDate) {
        this.nextMaturityNotificationDate = nextNotificationDate.toDate();
    }

    public void updateDepositAmount(final BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }


    public LocalDate getMaturityLocalDate() {
        LocalDate maturityLocalDate = null;
        if (this.maturityDate != null) {
            maturityLocalDate = new LocalDate(this.maturityDate);
        }
        return maturityLocalDate;
    }

    public LocalDate getExpectedFirstDepositOnDate() {
        LocalDate expectedFirstDepositOnLocalDate = null;
        if (this.expectedFirstDepositOnDate != null) {
            expectedFirstDepositOnLocalDate = new LocalDate(this.expectedFirstDepositOnDate);
        }
        return expectedFirstDepositOnLocalDate;
    }

    public boolean isPreClosurePenalApplicable() {
        if (this.preClosureDetail != null) {
            return this.preClosureDetail.isPreClosurePenalApplicable();
        }
        return false;
    }


    public Integer getActualDepositPeriod(final LocalDate interestPostingUpToDate, final SavingsPeriodFrequencyType periodFrequencyType) {
        LocalDate depositFromDate = getExpectedFirstDepositOnDate();

        if (depositFromDate == null) depositFromDate = this.account.accountSubmittedOrActivationDate();

        Integer actualDepositPeriod = this.depositPeriod;
        if (depositFromDate == null || getMaturityLocalDate() == null || interestPostingUpToDate.isEqual(getMaturityLocalDate())) {
            return actualDepositPeriod;
        }

        final SavingsPeriodFrequencyType depositPeriodFrequencyType = periodFrequencyType;
        switch (depositPeriodFrequencyType) {
            case DAYS:
                actualDepositPeriod = Days.daysBetween(depositFromDate, interestPostingUpToDate).getDays();
                break;
            case WEEKS:
                actualDepositPeriod = Weeks.weeksBetween(depositFromDate, interestPostingUpToDate).getWeeks();
                break;
            case MONTHS:
                actualDepositPeriod = Months.monthsBetween(depositFromDate, interestPostingUpToDate).getMonths();
                break;
            case YEARS:
                actualDepositPeriod = Years.yearsBetween(depositFromDate, interestPostingUpToDate).getYears();
                break;
            case INVALID:
                actualDepositPeriod = this.depositPeriod;// default value
                break;
        }
        return actualDepositPeriod;
    }

    public BigDecimal maturityAmount() {
        return this.maturityAmount;
    }

    public void updateOnAccountClosureStatus(final DepositAccountOnClosureType onClosureType) {
        this.onAccountClosureType = onClosureType.getValue();
    }

    public boolean isReinvestOnClosure() {
        return DepositAccountOnClosureType.fromInt(this.onAccountClosureType).isReinvest();
    }

    public boolean isTransferToSavingsOnClosure() {
        return DepositAccountOnClosureType.fromInt(this.onAccountClosureType).isTransferToSavings();
    }

    public DepositAccountTermAndPreClosure copy(BigDecimal depositAmount) {
        final SavingsAccount account = null;
        final BigDecimal maturityAmount = null;
        final BigDecimal actualDepositAmount = depositAmount;
        final LocalDate maturityDate = null;
        final Integer depositPeriod = this.depositPeriod;
        final SavingsPeriodFrequencyType depositPeriodFrequency = SavingsPeriodFrequencyType.fromInt(this.depositPeriodFrequency);
        final DepositPreClosureDetail preClosureDetail = this.preClosureDetail.copy();
        final DepositTermDetail depositTermDetail = this.depositTermDetail.copy();
        final LocalDate expectedFirstDepositOnDate = null;
        final Boolean maturitySmsNotification = this.maturitySmsNotification;
        final Integer maturityNotificationPeriod = this.maturityNotificationPeriod;
        final SavingsPeriodFrequencyType maturityNotificationFrequency = SavingsPeriodFrequencyType.fromInt(this.getMaturityNotificationFrequency());

        final DepositAccountOnClosureType accountOnClosureType = null;
        return DepositAccountTermAndPreClosure.createNew(preClosureDetail, depositTermDetail, account, actualDepositAmount, maturityAmount,
                maturityDate, depositPeriod, depositPeriodFrequency, expectedFirstDepositOnDate, accountOnClosureType,
                false, null, maturityNotificationPeriod,maturityNotificationFrequency,maturitySmsNotification);
    }

    public void updateExpectedFirstDepositDate(final LocalDate expectedFirstDepositOnDate) {
        this.expectedFirstDepositOnDate = expectedFirstDepositOnDate.toDate();
    }

    public boolean isTransferInterestToLinkedAccount() {
        return this.transferInterestToLinkedAccount;
    }

    public boolean isAfterExpectedFirstDepositDate(final LocalDate compareDate) {
        boolean isAfterExpectedFirstDepositDate = false;
        if (this.expectedFirstDepositOnDate != null) {
            isAfterExpectedFirstDepositDate = compareDate.isAfter(getExpectedFirstDepositOnDate());
        }
        return isAfterExpectedFirstDepositDate;
    }

    public DepositPreClosureDetail getPreClosureDetail() {
        return preClosureDetail;
    }

    public void updateDepositPeriod(final Integer depositPeriod) {
        this.depositPeriod = depositPeriod;
    }

    public void updateDepositPeriodFrequencyType(final Integer depositPeriodFrequencyType) {
        this.depositPeriodFrequency = depositPeriodFrequencyType;
    }

    public BigDecimal getInterestCarriedForwardOnTopUp() {
        return interestCarriedForwardOnTopUp;
    }

    public void setInterestCarriedForwardOnTopUp(BigDecimal interestCarriedForwardOnTopUp) {
        this.interestCarriedForwardOnTopUp = interestCarriedForwardOnTopUp;
    }

    public void updateTargetAmount(final BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void updateTargetMaturityAmount(final BigDecimal targetMaturityAmount) {
        this.targetMaturityAmount = targetMaturityAmount;
    }

    public BigDecimal getTargetMaturityAmount() {
        return targetMaturityAmount;
    }

    public Integer getMaturityNotificationPeriod() {
        return maturityNotificationPeriod;
    }

    public void setMaturityNotificationPeriod(Integer maturityNotificationPeriod) {
        this.maturityNotificationPeriod = maturityNotificationPeriod;
    }
    public boolean getMaturitySmsNotification() {
        return maturitySmsNotification;
    }

    public void setMaturitySmsNotification(boolean maturitySmsNotification) {
        this.maturitySmsNotification = maturitySmsNotification;
    }

    public Integer getMaturityNotificationFrequency() {
        return maturityNotificationFrequency;
    }

    public Date getNextMaturityNotificationDate() {
        return nextMaturityNotificationDate;
    }

    public void setMaturityNotificationFrequency(Integer maturityNotificationFrequency) {
        this.maturityNotificationFrequency = maturityNotificationFrequency;
    }
}
