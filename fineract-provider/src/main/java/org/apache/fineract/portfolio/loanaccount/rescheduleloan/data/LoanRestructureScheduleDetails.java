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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.data;

import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.portfolio.loanaccount.data.DisbursementData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.joda.time.LocalDate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Immutable data object representing loan reschedule request data.
 **/
public class LoanRestructureScheduleDetails implements RowMapper<LoanRestructureScheduleDetails> {

    private final Long loanId;
    private final Long pendingInstallments;
    private final Long totalInstallments;
    private final Long restructureRequestId;
    private final LocalDate rescheduleFromDate;
    private final LocalDate rescheduleToDate;

//    private final Collection<CodeValueData> rescheduleReasons;
//    private final Collection<LoanTermVariationsData> loanTermVariationsData;

    private LoanTransactionData loanTransactionData;

    private LoanRestructureScheduleDetails(Long loanId, Long pendingInstallments,
                                           Long totalInstallments, Long restructureRequestId,
                                           LocalDate rescheduleFromDate, LocalDate rescheduleToDate) {
        this.loanId = loanId;
        this.pendingInstallments = pendingInstallments ;
        this.totalInstallments = totalInstallments;
        this.restructureRequestId = restructureRequestId;
        this.rescheduleFromDate = rescheduleFromDate;
        this.rescheduleToDate = rescheduleToDate;
    }


    /**
     * LoanRescheduleRequestData constructor
     *
     *            TODO
     **/
    public LoanRestructureScheduleDetails(Long loanId) {

        this.loanId = loanId;
        this.pendingInstallments = null;
        this.totalInstallments = null;
        this.restructureRequestId = null;
        this.rescheduleFromDate = null;
        this.rescheduleToDate = null;
    }

    public String schema() {
        return "";
    }

    @Override
    public LoanRestructureScheduleDetails mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
        final Long pendingInstallments = rs.getLong("pending_installments");
        final Long totalInstallments = rs.getLong("total_installments");
        final Long requestId = rs.getLong("restructure_request_id");
        LocalDate rescheduleFromDate =JdbcSupport.getLocalDate( rs, "reschedule_from_date");
        LocalDate rescheduleToDate =JdbcSupport.getLocalDate( rs, "reschedule_to_date");

        final LoanRestructureScheduleDetails scheduleDetails = new LoanRestructureScheduleDetails(loanId,
                pendingInstallments,totalInstallments, requestId,rescheduleFromDate, rescheduleToDate);
        return scheduleDetails;
    }

    /**
     * @return an instance of the LoanRescheduleRequestData class
     **/
    public static LoanRestructureScheduleDetails instance(Long loanId, Long pendingInstallments, Long totalInstallments,
                                                          Long restructureRequestId,LocalDate rescheduleFromDate, LocalDate rescheduleToDate) {

        return new LoanRestructureScheduleDetails(loanId, pendingInstallments,totalInstallments,
                restructureRequestId,rescheduleFromDate, rescheduleToDate);
    }

    /**
     * @return the loanId
     */
    public Long getLoanId() {
        return loanId;
    }

    public Long getPendingInstallments() {
        return pendingInstallments;
    }

    public Long getTotalInstallments() {
        return totalInstallments;
    }

    public Long getRestructureRequestId() {
        return restructureRequestId;
    }
}
