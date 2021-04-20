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
package org.apache.fineract.scheduledjobs.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.fineract.accounting.journalentry.exception.JournalEntryInvalidException;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.RoutingDataSourceServiceFactory;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.notification.config.MessagingConfiguration;
import org.apache.fineract.notification.config.VfdServiceApi;
import org.apache.fineract.notification.domain.VfdTransferNotification;
import org.apache.fineract.portfolio.account.service.AccountTransfersWritePlatformService;
import org.apache.fineract.portfolio.charge.domain.ChargeCalculationType;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.DepositAccountUtils;
import org.apache.fineract.portfolio.savings.classification.data.TransactionClassificationData;
import org.apache.fineract.portfolio.savings.classification.service.TransactionClassificationReadPlatformService;
import org.apache.fineract.portfolio.savings.data.DepositAccountData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountAnnualFeeData;
import org.apache.fineract.portfolio.savings.domain.*;
import org.apache.fineract.portfolio.savings.exception.InsufficientAccountBalanceException;
import org.apache.fineract.portfolio.savings.service.DepositAccountReadPlatformService;
import org.apache.fineract.portfolio.savings.service.DepositAccountWritePlatformService;
import org.apache.fineract.portfolio.savings.service.SavingsAccountChargeReadPlatformService;
import org.apache.fineract.portfolio.savings.service.SavingsAccountReadPlatformService;
import org.apache.fineract.portfolio.savings.service.SavingsAccountWritePlatformService;
import org.apache.fineract.portfolio.shareaccounts.service.ShareAccountDividendReadPlatformService;
import org.apache.fineract.portfolio.shareaccounts.service.ShareAccountSchedularService;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.google.gson.JsonElement;

@Service(value = "scheduledJobRunnerService")
public class ScheduledJobRunnerServiceImpl implements ScheduledJobRunnerService {

    private final static Logger logger = LoggerFactory.getLogger(ScheduledJobRunnerServiceImpl.class);
    private final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    private final DateTimeFormatter formatterWithTime = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    private final RoutingDataSourceServiceFactory dataSourceServiceFactory;
    private final SavingsAccountWritePlatformService savingsAccountWritePlatformService;
    private final SavingsAccountChargeReadPlatformService savingsAccountChargeReadPlatformService;
    private final DepositAccountReadPlatformService depositAccountReadPlatformService;
    private final DepositAccountWritePlatformService depositAccountWritePlatformService;
    private final ShareAccountDividendReadPlatformService shareAccountDividendReadPlatformService;
    private final ShareAccountSchedularService shareAccountSchedularService;
    private final SavingsAccountReadPlatformService savingsAccountReadPlatformService;
    private final SavingsTransactionRequestRepository savingsTransactionRequestRepository;
    private final TransactionClassificationReadPlatformService transactionClassificationReadPlatformService;

    private final MessagingConfiguration messagingConfiguration;
    private final AccountTransfersWritePlatformService accountTransfersWritePlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final VfdServiceApi vfdServiceApi;
    private final SavingsAccountRepositoryWrapper savingAccountRepositoryWrapper;

    @Autowired
    public ScheduledJobRunnerServiceImpl(final RoutingDataSourceServiceFactory dataSourceServiceFactory,
                                         final SavingsAccountWritePlatformService savingsAccountWritePlatformService,
                                         final SavingsAccountChargeReadPlatformService savingsAccountChargeReadPlatformService,
                                         final DepositAccountReadPlatformService depositAccountReadPlatformService,
                                         final DepositAccountWritePlatformService depositAccountWritePlatformService,
                                         final ShareAccountDividendReadPlatformService shareAccountDividendReadPlatformService,
                                         final ShareAccountSchedularService shareAccountSchedularService,
                                         final SavingsAccountReadPlatformService savingsAccountReadPlatformService,
                                         final SavingsTransactionRequestRepository savingsTransactionRequestRepository,
                                         final TransactionClassificationReadPlatformService transactionClassificationReadPlatformService,
                                         final MessagingConfiguration messagingConfiguration,
                                         final AccountTransfersWritePlatformService accountTransfersWritePlatformService, final FromJsonHelper fromApiJsonHelper,
                                         final VfdServiceApi vfdServiceApi, final SavingsAccountRepositoryWrapper savingAccountRepositoryWrapper) {
        this.dataSourceServiceFactory = dataSourceServiceFactory;
        this.savingsAccountWritePlatformService = savingsAccountWritePlatformService;
        this.savingsAccountChargeReadPlatformService = savingsAccountChargeReadPlatformService;
        this.depositAccountReadPlatformService = depositAccountReadPlatformService;
        this.depositAccountWritePlatformService = depositAccountWritePlatformService;
        this.shareAccountDividendReadPlatformService = shareAccountDividendReadPlatformService;
        this.shareAccountSchedularService = shareAccountSchedularService;
        this.savingsAccountReadPlatformService = savingsAccountReadPlatformService;
        this.savingsTransactionRequestRepository = savingsTransactionRequestRepository;
        this.transactionClassificationReadPlatformService = transactionClassificationReadPlatformService;
        this.messagingConfiguration = messagingConfiguration;
        this.accountTransfersWritePlatformService = accountTransfersWritePlatformService;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.vfdServiceApi = vfdServiceApi;
        this.savingAccountRepositoryWrapper = savingAccountRepositoryWrapper;
    }

    @Transactional
    @Override
    @CronTarget(jobName = JobName.UPDATE_LOAN_SUMMARY)
    public void updateLoanSummaryDetails() {

        final JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSourceServiceFactory.determineDataSourceService().retrieveDataSource());

        final StringBuilder updateSqlBuilder = new StringBuilder(900);
        updateSqlBuilder.append("update m_loan ");
        updateSqlBuilder.append("join (");
        updateSqlBuilder.append("SELECT ml.id AS loanId,");
        updateSqlBuilder.append("SUM(mr.principal_amount) as principal_disbursed_derived, ");
        updateSqlBuilder.append("SUM(IFNULL(mr.principal_completed_derived,0)) as principal_repaid_derived, ");
        updateSqlBuilder.append("SUM(IFNULL(mr.principal_writtenoff_derived,0)) as principal_writtenoff_derived,");
        updateSqlBuilder.append("SUM(IFNULL(mr.interest_amount,0)) as interest_charged_derived,");
        updateSqlBuilder.append("SUM(IFNULL(mr.interest_completed_derived,0)) as interest_repaid_derived,");
        updateSqlBuilder.append("SUM(IFNULL(mr.interest_waived_derived,0)) as interest_waived_derived,");
        updateSqlBuilder.append("SUM(IFNULL(mr.interest_writtenoff_derived,0)) as interest_writtenoff_derived,");
        updateSqlBuilder.append(
                "SUM(IFNULL(mr.fee_charges_amount,0)) + IFNULL((select SUM(lc.amount) from  m_loan_charge lc where lc.loan_id=ml.id and lc.is_active=1 and lc.charge_time_enum=1),0) as fee_charges_charged_derived,");
        updateSqlBuilder.append(
                "SUM(IFNULL(mr.fee_charges_completed_derived,0)) + IFNULL((select SUM(lc.amount_paid_derived) from  m_loan_charge lc where lc.loan_id=ml.id and lc.is_active=1 and lc.charge_time_enum=1),0) as fee_charges_repaid_derived,");
        updateSqlBuilder.append("SUM(IFNULL(mr.fee_charges_waived_derived,0)) as fee_charges_waived_derived,");
        updateSqlBuilder.append("SUM(IFNULL(mr.fee_charges_writtenoff_derived,0)) as fee_charges_writtenoff_derived,");
        updateSqlBuilder.append("SUM(IFNULL(mr.penalty_charges_amount,0)) as penalty_charges_charged_derived,");
        updateSqlBuilder.append("SUM(IFNULL(mr.penalty_charges_completed_derived,0)) as penalty_charges_repaid_derived,");
        updateSqlBuilder.append("SUM(IFNULL(mr.penalty_charges_waived_derived,0)) as penalty_charges_waived_derived,");
        updateSqlBuilder.append("SUM(IFNULL(mr.penalty_charges_writtenoff_derived,0)) as penalty_charges_writtenoff_derived ");
        updateSqlBuilder.append(" FROM m_loan ml ");
        updateSqlBuilder.append("INNER JOIN m_loan_repayment_schedule mr on mr.loan_id = ml.id ");
        updateSqlBuilder.append("WHERE ml.disbursedon_date is not null ");
        updateSqlBuilder.append("GROUP BY ml.id ");
        updateSqlBuilder.append(") x on x.loanId = m_loan.id ");

        updateSqlBuilder.append("SET m_loan.principal_disbursed_derived = x.principal_disbursed_derived,");
        updateSqlBuilder.append("m_loan.principal_repaid_derived = x.principal_repaid_derived,");
        updateSqlBuilder.append("m_loan.principal_writtenoff_derived = x.principal_writtenoff_derived,");
        updateSqlBuilder.append(
                "m_loan.principal_outstanding_derived = (x.principal_disbursed_derived - (x.principal_repaid_derived + x.principal_writtenoff_derived)),");
        updateSqlBuilder.append("m_loan.interest_charged_derived = x.interest_charged_derived,");
        updateSqlBuilder.append("m_loan.interest_repaid_derived = x.interest_repaid_derived,");
        updateSqlBuilder.append("m_loan.interest_waived_derived = x.interest_waived_derived,");
        updateSqlBuilder.append("m_loan.interest_writtenoff_derived = x.interest_writtenoff_derived,");
        updateSqlBuilder.append(
                "m_loan.interest_outstanding_derived = (x.interest_charged_derived - (x.interest_repaid_derived + x.interest_waived_derived + x.interest_writtenoff_derived)),");
        updateSqlBuilder.append("m_loan.fee_charges_charged_derived = x.fee_charges_charged_derived,");
        updateSqlBuilder.append("m_loan.fee_charges_repaid_derived = x.fee_charges_repaid_derived,");
        updateSqlBuilder.append("m_loan.fee_charges_waived_derived = x.fee_charges_waived_derived,");
        updateSqlBuilder.append("m_loan.fee_charges_writtenoff_derived = x.fee_charges_writtenoff_derived,");
        updateSqlBuilder.append(
                "m_loan.fee_charges_outstanding_derived = (x.fee_charges_charged_derived - (x.fee_charges_repaid_derived + x.fee_charges_waived_derived + x.fee_charges_writtenoff_derived)),");
        updateSqlBuilder.append("m_loan.penalty_charges_charged_derived = x.penalty_charges_charged_derived,");
        updateSqlBuilder.append("m_loan.penalty_charges_repaid_derived = x.penalty_charges_repaid_derived,");
        updateSqlBuilder.append("m_loan.penalty_charges_waived_derived = x.penalty_charges_waived_derived,");
        updateSqlBuilder.append("m_loan.penalty_charges_writtenoff_derived = x.penalty_charges_writtenoff_derived,");
        updateSqlBuilder.append(
                "m_loan.penalty_charges_outstanding_derived = (x.penalty_charges_charged_derived - (x.penalty_charges_repaid_derived + x.penalty_charges_waived_derived + x.penalty_charges_writtenoff_derived)),");
        updateSqlBuilder.append(
                "m_loan.total_expected_repayment_derived = (x.principal_disbursed_derived + x.interest_charged_derived + x.fee_charges_charged_derived + x.penalty_charges_charged_derived),");
        updateSqlBuilder.append(
                "m_loan.total_repayment_derived = (x.principal_repaid_derived + x.interest_repaid_derived + x.fee_charges_repaid_derived + x.penalty_charges_repaid_derived),");
        updateSqlBuilder.append(
                "m_loan.total_expected_costofloan_derived = (x.interest_charged_derived + x.fee_charges_charged_derived + x.penalty_charges_charged_derived),");
        updateSqlBuilder.append(
                "m_loan.total_costofloan_derived = (x.interest_repaid_derived + x.fee_charges_repaid_derived + x.penalty_charges_repaid_derived),");
        updateSqlBuilder.append(
                "m_loan.total_waived_derived = (x.interest_waived_derived + x.fee_charges_waived_derived + x.penalty_charges_waived_derived),");
        updateSqlBuilder.append(
                "m_loan.total_writtenoff_derived = (x.interest_writtenoff_derived +  x.fee_charges_writtenoff_derived + x.penalty_charges_writtenoff_derived),");
        updateSqlBuilder.append("m_loan.total_outstanding_derived=");
        updateSqlBuilder.append(" (x.principal_disbursed_derived - (x.principal_repaid_derived + x.principal_writtenoff_derived)) + ");
        updateSqlBuilder.append(
                " (x.interest_charged_derived - (x.interest_repaid_derived + x.interest_waived_derived + x.interest_writtenoff_derived)) +");
        updateSqlBuilder.append(
                " (x.fee_charges_charged_derived - (x.fee_charges_repaid_derived + x.fee_charges_waived_derived + x.fee_charges_writtenoff_derived)) +");
        updateSqlBuilder.append(
                " (x.penalty_charges_charged_derived - (x.penalty_charges_repaid_derived + x.penalty_charges_waived_derived + x.penalty_charges_writtenoff_derived))");

        final int result = jdbcTemplate.update(updateSqlBuilder.toString());

        logger.info(ThreadLocalContextUtil.getTenant().getName() + ": Results affected by update: " + result);
    }

    @Transactional
    @Override
    @CronTarget(jobName = JobName.UPDATE_LOAN_PAID_IN_ADVANCE)
    public void updateLoanPaidInAdvance() {

        final JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSourceServiceFactory.determineDataSourceService().retrieveDataSource());

        jdbcTemplate.execute("truncate table m_loan_paid_in_advance");

        final StringBuilder updateSqlBuilder = new StringBuilder(900);

        updateSqlBuilder.append(
                "INSERT INTO m_loan_paid_in_advance(loan_id, principal_in_advance_derived, interest_in_advance_derived, fee_charges_in_advance_derived, penalty_charges_in_advance_derived, total_in_advance_derived)");
        updateSqlBuilder.append(" select ml.id as loanId,");
        updateSqlBuilder.append(" SUM(ifnull(mr.principal_completed_derived, 0)) as principal_in_advance_derived,");
        updateSqlBuilder.append(" SUM(ifnull(mr.interest_completed_derived, 0)) as interest_in_advance_derived,");
        updateSqlBuilder.append(" SUM(ifnull(mr.fee_charges_completed_derived, 0)) as fee_charges_in_advance_derived,");
        updateSqlBuilder.append(" SUM(ifnull(mr.penalty_charges_completed_derived, 0)) as penalty_charges_in_advance_derived,");
        updateSqlBuilder.append(
                " (SUM(ifnull(mr.principal_completed_derived, 0)) + SUM(ifnull(mr.interest_completed_derived, 0)) + SUM(ifnull(mr.fee_charges_completed_derived, 0)) + SUM(ifnull(mr.penalty_charges_completed_derived, 0))) as total_in_advance_derived");
        updateSqlBuilder.append(" FROM m_loan ml ");
        updateSqlBuilder.append(" INNER JOIN m_loan_repayment_schedule mr on mr.loan_id = ml.id ");
        updateSqlBuilder.append(" WHERE ml.loan_status_id = 300 ");
        updateSqlBuilder.append(" and mr.duedate >= CURDATE() ");
        updateSqlBuilder.append(" GROUP BY ml.id");
        updateSqlBuilder
                .append(" HAVING (SUM(ifnull(mr.principal_completed_derived, 0)) + SUM(ifnull(mr.interest_completed_derived, 0)) +");
        updateSqlBuilder
                .append(" SUM(ifnull(mr.fee_charges_completed_derived, 0)) + SUM(ifnull(mr.penalty_charges_completed_derived, 0))) > 0.0");

        final int result = jdbcTemplate.update(updateSqlBuilder.toString());

        logger.info(ThreadLocalContextUtil.getTenant().getName() + ": Results affected by update: " + result);
    }

    @Override
    @CronTarget(jobName = JobName.APPLY_ANNUAL_FEE_FOR_SAVINGS)
    public void applyAnnualFeeForSavings() {

        final Collection<SavingsAccountAnnualFeeData> annualFeeData = this.savingsAccountChargeReadPlatformService
                .retrieveChargesWithAnnualFeeDue();

        for (final SavingsAccountAnnualFeeData savingsAccountReference : annualFeeData) {
            try {
                this.savingsAccountWritePlatformService.applyAnnualFee(savingsAccountReference.getId(),
                        savingsAccountReference.getAccountId());
            } catch (final PlatformApiDataValidationException e) {
                final List<ApiParameterError> errors = e.getErrors();
                for (final ApiParameterError error : errors) {
                    logger.error("Apply annual fee failed for account:" + savingsAccountReference.getAccountNo() + " with message "
                            + error.getDeveloperMessage());
                }
            } catch (final Exception ex) {
                // need to handle this scenario
            }
        }

        logger.info(ThreadLocalContextUtil.getTenant().getName() + ": Savings accounts affected by update: " + annualFeeData.size());
    }

    @Override
    @CronTarget(jobName = JobName.PAY_DUE_SAVINGS_CHARGES)
    public void applyDueChargesForSavings() throws JobExecutionException {

        // We first take care of updating monthly withdraw charges by percentage
        // in this case we have % of total withdrawals in a month
        // then call pay charges.

        final StringBuilder errorMsg = new StringBuilder();

       final Collection<SavingsAccountAnnualFeeData> monthlyWithdrawChargesDueData = this.savingsAccountChargeReadPlatformService
                .retrieveAccountsWithChargeByCalculationTypeAndStatus(ChargeCalculationType.PERCENT_OF_TOTAL_WITHDRAWALS.getValue(), new Long(1));
        for (final SavingsAccountAnnualFeeData savingsAccountMonthlyFeeData: monthlyWithdrawChargesDueData){
            try {
                this.savingsAccountWritePlatformService.updateSavingsAccountCharge(savingsAccountMonthlyFeeData.getAccountId(), savingsAccountMonthlyFeeData.getId());
            }catch (final PlatformApiDataValidationException e) {
                final List<ApiParameterError> errors = e.getErrors();
                for (final ApiParameterError error : errors) {
                    logger.error("Generate Monthly withdrawal charges by percentage  for savings failed for account:" + savingsAccountMonthlyFeeData.getAccountNo()
                            + " with message " + error.getDeveloperMessage());
                    errorMsg.append("Generate Monthly total withdrawal charges by percentage  for savings failed for account:").append(savingsAccountMonthlyFeeData.getAccountNo())
                            .append(" with message ").append(error.getDeveloperMessage());
                }
            }
        }


        final Collection<SavingsAccountAnnualFeeData> chargesDueData = this.savingsAccountChargeReadPlatformService
                .retrieveChargesWithDue();

        for (final SavingsAccountAnnualFeeData savingsAccountReference : chargesDueData) {
            try {
                    this.savingsAccountWritePlatformService.applyChargeDue(savingsAccountReference.getId(),
                            savingsAccountReference.getAccountId());
            } catch (final PlatformApiDataValidationException e) {
                final List<ApiParameterError> errors = e.getErrors();
                for (final ApiParameterError error : errors) {
                    logger.error("Apply Charges due for savings failed for account:" + savingsAccountReference.getAccountNo()
                            + " with message " + error.getDeveloperMessage());
                    errorMsg.append("Apply Charges due for savings failed for account:").append(savingsAccountReference.getAccountNo())
                            .append(" with message ").append(error.getDeveloperMessage());
                }
            }
        }



        logger.info(ThreadLocalContextUtil.getTenant().getName() + ": Savings accounts affected by update: " + chargesDueData.size());

        /*
         * throw exception if any charge payment fails.
         */
        if (errorMsg.length() > 0) { throw new JobExecutionException(errorMsg.toString()); }
    }

    @Transactional
    @Override
    @CronTarget(jobName = JobName.UPDATE_NPA)
    public void updateNPA() {

        final JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSourceServiceFactory.determineDataSourceService().retrieveDataSource());

        final StringBuilder resetNPASqlBuilder = new StringBuilder(900);
        resetNPASqlBuilder.append("update m_loan loan ");
        resetNPASqlBuilder.append("left join m_loan_arrears_aging laa on laa.loan_id = loan.id ");
        resetNPASqlBuilder.append("inner join m_product_loan mpl on mpl.id = loan.product_id and mpl.overdue_days_for_npa is not null ");
        resetNPASqlBuilder.append("set loan.is_npa = 0 ");
        resetNPASqlBuilder.append("where  loan.loan_status_id = 300 and mpl.account_moves_out_of_npa_only_on_arrears_completion = 0 ");
        resetNPASqlBuilder
                .append("or (mpl.account_moves_out_of_npa_only_on_arrears_completion = 1 and laa.overdue_since_date_derived is null)");

        jdbcTemplate.update(resetNPASqlBuilder.toString());

        final StringBuilder updateSqlBuilder = new StringBuilder(900);

        updateSqlBuilder.append("UPDATE m_loan as ml,");
        updateSqlBuilder.append(" (select loan.id ");
        updateSqlBuilder.append("from m_loan_arrears_aging laa");
        updateSqlBuilder.append(" INNER JOIN  m_loan loan on laa.loan_id = loan.id ");
        updateSqlBuilder.append(" INNER JOIN m_product_loan mpl on mpl.id = loan.product_id AND mpl.overdue_days_for_npa is not null ");
        updateSqlBuilder.append("WHERE loan.loan_status_id = 300  and ");
        updateSqlBuilder.append("laa.overdue_since_date_derived < SUBDATE(CURDATE(),INTERVAL  ifnull(mpl.overdue_days_for_npa,0) day) ");
        updateSqlBuilder.append("group by loan.id) as sl ");
        updateSqlBuilder.append("SET ml.is_npa=1 where ml.id=sl.id ");

        final int result = jdbcTemplate.update(updateSqlBuilder.toString());

        logger.info(ThreadLocalContextUtil.getTenant().getName() + ": Results affected by update: " + result);
    }

    @Override
    @CronTarget(jobName = JobName.UPDATE_DEPOSITS_ACCOUNT_MATURITY_DETAILS)
    public void updateMaturityDetailsOfDepositAccounts() {

        final Collection<DepositAccountData> depositAccounts = this.depositAccountReadPlatformService.retrieveForMaturityUpdate();

        for (final DepositAccountData depositAccount : depositAccounts) {
            try {
                final DepositAccountType depositAccountType = DepositAccountType.fromInt(depositAccount.depositType().getId().intValue());
                this.depositAccountWritePlatformService.updateMaturityDetails(depositAccount.id(), depositAccountType);
            } catch (final PlatformApiDataValidationException e) {
                final List<ApiParameterError> errors = e.getErrors();
                for (final ApiParameterError error : errors) {
                    logger.error("Update maturity details failed for account:" + depositAccount.accountNo() + " with message "
                            + error.getDeveloperMessage());
                }
            } catch (final Exception ex) {
                // need to handle this scenario
            }
        }

        logger.info(ThreadLocalContextUtil.getTenant().getName() + ": Deposit accounts affected by update: " + depositAccounts.size());
    }

    @Override
    @CronTarget(jobName = JobName.GENERATE_RD_SCEHDULE)
    public void generateRDSchedule() {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSourceServiceFactory.determineDataSourceService().retrieveDataSource());
        final Collection<Map<String, Object>> scheduleDetails = this.depositAccountReadPlatformService.retriveDataForRDScheduleCreation();
        String insertSql = "INSERT INTO `m_mandatory_savings_schedule` (`savings_account_id`, `duedate`, `installment`, `deposit_amount`, `completed_derived`, `created_date`, `lastmodified_date`) VALUES ";
        StringBuilder sb = new StringBuilder();
        String currentDate = formatterWithTime.print(DateUtils.getLocalDateTimeOfTenant());
        int iterations = 0;
        for (Map<String, Object> details : scheduleDetails) {
            Long count = (Long) details.get("futureInstallemts");
            if (count == null) {
                count = 0l;
            }
            final Long savingsId = (Long) details.get("savingsId");
            final BigDecimal amount = (BigDecimal) details.get("amount");
            final String recurrence = (String) details.get("recurrence");
            Date date = (Date) details.get("dueDate");
            LocalDate lastDepositDate = new LocalDate(date);
            Integer installmentNumber = (Integer) details.get("installment");
            while (count < DepositAccountUtils.GENERATE_MINIMUM_NUMBER_OF_FUTURE_INSTALMENTS) {
                count++;
                installmentNumber++;
                lastDepositDate = DepositAccountUtils.calculateNextDepositDate(lastDepositDate, recurrence);

                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("(");
                sb.append(savingsId);
                sb.append(",'");
                sb.append(formatter.print(lastDepositDate));
                sb.append("',");
                sb.append(installmentNumber);
                sb.append(",");
                sb.append(amount);
                sb.append(", b'0','");
                sb.append(currentDate);
                sb.append("','");
                sb.append(currentDate);
                sb.append("')");
                iterations++;
                if (iterations > 200) {
                    jdbcTemplate.update(insertSql + sb.toString());
                    sb = new StringBuilder();
                }

            }
        }

        if (sb.length() > 0) {
            jdbcTemplate.update(insertSql + sb.toString());
        }

    }

    @Override
    @CronTarget(jobName = JobName.POST_DIVIDENTS_FOR_SHARES)
    public void postDividends() throws JobExecutionException {
        List<Map<String, Object>> dividendDetails = this.shareAccountDividendReadPlatformService.retriveDividendDetailsForPostDividents();
        StringBuilder errorMsg = new StringBuilder();
        for (Map<String, Object> dividendMap : dividendDetails) {
            Long id = null;
            Long savingsId = null;
            if (dividendMap.get("id") instanceof BigInteger) { // Drizzle is
                                                               // returning
                                                               // BigInteger
                id = ((BigInteger) dividendMap.get("id")).longValue();
                savingsId = ((BigInteger) dividendMap.get("savingsAccountId")).longValue();
            } else { // MySQL connector is returning Long
                id = (Long) dividendMap.get("id");
                savingsId = (Long) dividendMap.get("savingsAccountId");
            }
            try {
                this.shareAccountSchedularService.postDividend(id, savingsId);
            } catch (final PlatformApiDataValidationException e) {
                final List<ApiParameterError> errors = e.getErrors();
                for (final ApiParameterError error : errors) {
                    logger.error("Post Dividends to savings failed for Divident detail Id:" + id + " and savings Id: " + savingsId
                            + " with message " + error.getDeveloperMessage());
                    errorMsg.append("Post Dividends to savings failed for Divident detail Id:").append(id).append(" and savings Id:")
                            .append(savingsId).append(" with message ").append(error.getDeveloperMessage());
                }
            } catch (final Exception e) {
                logger.error("Post Dividends to savings failed for Divident detail Id:" + id + " and savings Id: " + savingsId
                        + " with message " + e.getLocalizedMessage());
                errorMsg.append("Post Dividends to savings failed for Divident detail Id:").append(id).append(" and savings Id:")
                        .append(savingsId).append(" with message ").append(e.getLocalizedMessage());
            }
        }

        if (errorMsg.length() > 0) { throw new JobExecutionException(errorMsg.toString()); }
    }

    @Override
    @CronTarget(jobName = JobName.POSTACCRUALINTERESTFORSAVINGS)
    public void postAccrualInterestForSavings() throws JobExecutionException {
        StringBuilder errorMsg = new StringBuilder();
        LocalDate currentDate = new LocalDate();
        final List<Long> activeSavingsAccounts = this.savingsAccountReadPlatformService.retriveActiveSavingsAccrualAccounts(100l);
        for (Long savingAccount : activeSavingsAccounts) {
            try {
                this.savingsAccountWritePlatformService.postAccrualInterest(savingAccount, currentDate, false);
            } catch (PlatformApiDataValidationException e) {
                final List<ApiParameterError> errors = e.getErrors();
                for (final ApiParameterError error : errors) {
                    logger.error("Post Accruals to savings failed for savings Id: " + savingAccount + " with message "
                            + error.getDeveloperMessage());
                    errorMsg.append("Post Accruals to savings failed for savings Id: ").append(savingAccount).append(" with message ")
                            .append(error.getDeveloperMessage());
                }
            } catch (JournalEntryInvalidException e) {
                logger.error(
                        "Post Accruals to savings failed for savings Id: " + savingAccount + " with message " + e.getDefaultUserMessage());
                errorMsg.append("Post Accruals to savings failed for savings Id: ").append(savingAccount).append(" with message ")
                        .append(e.getDefaultUserMessage());
            } catch (final Exception e) {
                logger.error(
                        "Post Accruals to savings failed for savings Id: " + savingAccount + " with message " + e.getLocalizedMessage());
                errorMsg.append("Post Accruals to savings failed for savings Id: ").append(savingAccount).append(" with message ")
                        .append(e.getLocalizedMessage());
            }
        }
        final List<Long> activeFixedDepositAccounts = this.savingsAccountReadPlatformService.retriveActiveSavingsAccrualAccounts(200l);
        for (Long activefixedDepositAccount : activeFixedDepositAccounts) {
            try {
                this.depositAccountWritePlatformService.postAccrualInterest(activefixedDepositAccount, DepositAccountType.FIXED_DEPOSIT);
            } catch (PlatformApiDataValidationException e) {
                final List<ApiParameterError> errors = e.getErrors();
                for (final ApiParameterError error : errors) {
                    logger.error("Post Accruals to fixed deposit failed for savings Id: " + activefixedDepositAccount + " with message "
                            + error.getDeveloperMessage());
                    errorMsg.append("Post Accruals to savings failed for fixed deposit Id: ").append(activefixedDepositAccount)
                            .append(" with message ").append(error.getDeveloperMessage());
                }
            } catch (JournalEntryInvalidException e) {
                logger.error("Post Accruals to savings failed for savings Id: " + activefixedDepositAccount + " with message "
                        + e.getDefaultUserMessage());
                errorMsg.append("Post Accruals to savings failed for savings Id: ").append(activefixedDepositAccount)
                        .append(" with message ").append(e.getDefaultUserMessage());
            } catch (final Exception e) {
                logger.error("Post Accruals to savings failed for savings Id: " + activefixedDepositAccount + " with message "
                        + e.getLocalizedMessage());
                errorMsg.append("Post Accruals to savings failed for savings Id: ").append(activefixedDepositAccount)
                        .append(" with message ").append(e.getLocalizedMessage());
            }
        }
        final List<Long> activeRecurringDepositAccounts = this.savingsAccountReadPlatformService.retriveActiveSavingsAccrualAccounts(300l);
        for (Long activeRecurringDepositAccount : activeRecurringDepositAccounts) {
            try {
                this.depositAccountWritePlatformService.postAccrualInterest(activeRecurringDepositAccount,
                        DepositAccountType.RECURRING_DEPOSIT);
            } catch (PlatformApiDataValidationException e) {
                final List<ApiParameterError> errors = e.getErrors();
                for (final ApiParameterError error : errors) {
                    logger.error(
                            "Post Accruals to recurring deposit failed for savings Id: " + activeRecurringDepositAccount + " with message "
                            + error.getDeveloperMessage());
                    errorMsg.append("Post Accruals to savings failed for fixed deposit Id: ").append(activeRecurringDepositAccount)
                            .append(" with message ").append(error.getDeveloperMessage());
                }
            } catch (JournalEntryInvalidException e) {
                logger.error("Post Accruals to savings failed for savings Id: " + activeRecurringDepositAccount + " with message "
                        + e.getDefaultUserMessage());
                errorMsg.append("Post Accruals to savings failed for savings Id: ").append(activeRecurringDepositAccount)
                        .append(" with message ").append(e.getDefaultUserMessage());
            } catch (final Exception e) {
                logger.error("Post Accruals to savings failed for savings Id: " + activeRecurringDepositAccount + " with message "
                        + e.getLocalizedMessage());
                errorMsg.append("Post Accruals to savings failed for savings Id: ").append(activeRecurringDepositAccount)
                        .append(" with message ").append(e.getLocalizedMessage());
            }
        }
        if (errorMsg.length() > 0) { throw new JobExecutionException(errorMsg.toString()); }
    }

    @Transactional
    @Override
    @CronTarget(jobName = JobName.SAVINGSTRANSACTIONCLASSIFICATION)
    public void savingTransactionClassification() throws JobExecutionException {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSourceServiceFactory.determineDataSourceService().retrieveDataSource());
        int result = 0;
        String transactionClassification = null;
        Long accountTransferTransaction = null;
        Collection<TransactionClassificationData> transactionClassifications = transactionClassificationReadPlatformService
                .getTransactionClassification();
        List<Long> unCategorisedTransactions = this.savingsAccountReadPlatformService.retriveUnClassifiedTransactions();
        for (Long unCategorisedTransaction : unCategorisedTransactions) {
            SavingsTransactionRequest request = savingsTransactionRequestRepository.findByTransactionId(unCategorisedTransaction);
            try {
                accountTransferTransaction = savingsAccountReadPlatformService
                        .findSavingsTransactionAccountTransfer(unCategorisedTransaction);
            } catch (EmptyResultDataAccessException ex) {
                accountTransferTransaction = null;
            }
            if (request == null) {
                transactionClassification = "Other";
            } else if (request != null && accountTransferTransaction != null) {
                transactionClassification = "Transfers to VFD";
            } else if (request != null && request.getTransactionBrandName() != null) {
                for (TransactionClassificationData transanctionClassification : transactionClassifications) {
                    if (request.getTransactionBrandName().equalsIgnoreCase(transanctionClassification.getOperator())) {
                        transactionClassification = transanctionClassification.getClassification();
                    }
                }
            } else {
                transactionClassification = "Transfers to other banks";
            }
            result = result + jdbcTemplate.update("update m_savings_account_transaction set transaction_classification = \""
                    + transactionClassification + "\" where id = " + unCategorisedTransaction);
        }
        logger.info(ThreadLocalContextUtil.getTenant().getName() + ": Results affected by update: " + result);
    }

    @SuppressWarnings("rawtypes")
    @Override
    @CronTarget(jobName = JobName.PROCESS_ACCOUNT_TRANSFERS)
    public void processAccountTransfers() throws JobExecutionException {
        StringBuilder errorMsg = new StringBuilder();

        Session session;
        try {
            session = this.messagingConfiguration.connectionFactory().createConnection().createSession(false, Session.CLIENT_ACKNOWLEDGE);
            Queue queue = session.createQueue("AccountTransferQueue");
            QueueBrowser queueBrowser = session.createBrowser(queue);
            Enumeration msgs = queueBrowser.getEnumeration();

            Destination destination = session.createQueue(queue.getQueueName());

            while (msgs.hasMoreElements()) {
                MessageConsumer consumer = session.createConsumer(destination);
                Message message = consumer.receive(10000);

                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    logger.info(ThreadLocalContextUtil.getTenant().getName() + ": Message received from Active MQ, Incoming Message: '"
                            + textMessage.getText() + "'");

                    String json = textMessage.getText();
                    final JsonElement parsedCommand = this.fromApiJsonHelper.parse(json);

                    JsonCommand command = JsonCommand.from(json, parsedCommand, this.fromApiJsonHelper, null, null, null,
                            null, null, null, null, null, null, null, null, null);
                    CommandProcessingResult result;
                    try {
                        result = accountTransfersWritePlatformService.create(command);


                        VfdTransferNotification notification = VfdTransferNotification.fromRequest(json, "both");

                        logger.info(ThreadLocalContextUtil.getTenant().getName()
                                + ": Successfully processed account transfer from sender account id : '" + notification.getSenderAccountId()
                                + " to beneficiary account id : " + notification.getBeneficiaryAccountId() + ", resource id = "
                                + result.resourceId() + "'");

                        notification.setSenderAccountNumber(result.fromSavingsAccountNumber());
                        notification.setBeneficiaryAccountNumber(result.toSavingsAccountNumber());

                        this.sendNotificationToVfdService(notification, errorMsg);


                    } catch (PlatformApiDataValidationException e) {
                        final List<ApiParameterError> errors = e.getErrors();
                        for (final ApiParameterError error : errors) {
                            VfdTransferNotification request = VfdTransferNotification.fromRequest(json, "failed");

                            logger.error("Processing account transfer request failed : " + " with message " + error.getDeveloperMessage());
                            errorMsg.append("Processing account transfer request failed : : ").append(" with message, ")
                                    .append(error.getDeveloperMessage());
                            request.setAlertType("failed");

                            this.sendFailedNotificationToVfdService(json, errorMsg, error.getDeveloperMessage());
                        }
                    } catch ( InsufficientAccountBalanceException e) {
                            VfdTransferNotification request = VfdTransferNotification.fromRequest(json, "failed");

                            logger.error("Processing account transfer request failed : " + " with message " + e.getDefaultUserMessage());
                            errorMsg.append("Processing account transfer request failed : : ").append(" with message, ")
                                    .append(e.getDefaultUserMessage());
                            request.setAlertType("failed");

                            this.sendFailedNotificationToVfdService(json, errorMsg, e.getDefaultUserMessage());
                    }
                    catch (Exception e) {

                        VfdTransferNotification request = VfdTransferNotification.fromRequest(json, "failed");

                        logger.trace(ThreadLocalContextUtil.getTenant().getName()
                                + ": Processing account transfer request failed for sender account id : " + request.getSenderAccountId()
                                + " to beneficiary account id : " + request.getBeneficiaryAccountId(), e);
                        errorMsg.append("Processing account transfer request failed for sender account id : " + request.getSenderAccountId()
                                + " to beneficiary account id : " + request.getBeneficiaryAccountId()).append(" with message, ")
                                .append(e.getMessage());

                        this.sendFailedNotificationToVfdService(json, errorMsg, e.getMessage());

                    }

                }

                if (message != null) {
                    message.acknowledge();
                }
                consumer.close();
            }
            session.close();

        } catch (JMSException e) {

            logger.trace(ThreadLocalContextUtil.getTenant().getName() + "Processing account transfer request failed with Active MQ error: ",
                    e);
            errorMsg.append(
                    ThreadLocalContextUtil.getTenant().getName() + "processing account transfer request failed with Active MQ error: ")
                    .append(" with message ")
                    .append(e.getMessage());
        }

        if (errorMsg.length() > 0) { throw new JobExecutionException(errorMsg.toString()); }


    }

    private void sendNotificationToVfdService(VfdTransferNotification notification, StringBuilder errorMsg) {

        errorMsg = errorMsg == null ? new StringBuilder() : errorMsg;

        try {

            ResponseEntity<String> response = vfdServiceApi.sendNotification(notification);
            logger.info(ThreadLocalContextUtil.getTenant().getName() + ": Notification for successful transfer : Status Code = "
                    + response.getStatusCode() + ", message = " +
                response.getBody());

        } catch (HttpStatusCodeException e) {
            logger.trace(ThreadLocalContextUtil.getTenant().getName() + "VFD Notification service failure: ", e);
            errorMsg.append("VFD Notification service failure: ").append(" with message ").append(e.getMessage());
        } catch (RestClientException e) {
            logger.trace(ThreadLocalContextUtil.getTenant().getName() + "VFD Notification service failure: ", e);
            errorMsg.append("VFD Notification service failure: ").append(" with message ").append(e.getMessage());
        }

    }

    private void sendFailedNotificationToVfdService(String json, StringBuilder errorMsg, String errorLocalizedMessage) {
        errorMsg = errorMsg == null ? new StringBuilder() : errorMsg;

        VfdTransferNotification request = VfdTransferNotification.fromRequest(json, "failed");

        VfdTransferNotification notification = new VfdTransferNotification(request.getSenderClientId(), null,
                request.getSenderNarration(), request.getAlertType(), request.getAmount(), errorLocalizedMessage);

        if (request.getSenderAccountId() != null) {
            SavingsAccount senderAccount = savingAccountRepositoryWrapper
                    .findOneWithNotFoundDetection(request.getSenderAccountId());
            notification.setSenderAccountNumber(senderAccount.getAccountNumber());
            notification.setSenderAccountId(senderAccount.getId());
        }

        if (request.getBeneficiaryAccountId() != null) {
            SavingsAccount beneficiaryAccount = savingAccountRepositoryWrapper
                    .findOneWithNotFoundDetection(request.getBeneficiaryAccountId());
            notification.setBeneficiaryAccountNumber(beneficiaryAccount.getAccountNumber());
            notification.setBeneficiaryAccountId(beneficiaryAccount.getId());
        }

        try {

            ResponseEntity<String> response = vfdServiceApi.sendNotification(notification);
            logger.info(ThreadLocalContextUtil.getTenant().getName() + ": Notification for successful transfer : Status Code = "
                    + response.getStatusCode() + ", message = " +
                    response.getBody());

        } catch (HttpStatusCodeException e) {
            logger.trace(ThreadLocalContextUtil.getTenant().getName() + "VFD Notification service failure: ", e);
            errorMsg.append("VFD Notification service failure: ").append(" with message ").append(e.getMessage());
        } catch (RestClientException e) {
            logger.trace(ThreadLocalContextUtil.getTenant().getName() + "VFD Notification service failure: ", e);
            errorMsg.append("VFD Notification service failure: ").append(" with message ").append(e.getMessage());
        }

    }

}
