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

package org.apache.fineract.portfolio.loanaccount.domain;

import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "loan_balance_history")
public class LoanBalanceHistory extends AbstractPersistableCustom<Long>  {

    @OneToOne(optional = false)
    @JoinColumn(name = "loan_id", referencedColumnName = "id", nullable = false)
    private Loan loan;

    @Column(name = "balance", scale = 6, precision = 19, nullable = false)
    private BigDecimal balance;

    @Column(name = "derived_balance", scale = 6, precision = 19)
    private BigDecimal derivedBalance;

    @Temporal(TemporalType.DATE)
    @Column(name = "balance_date", nullable = false)
    private Date balanceDate;

    @Column
    private Boolean valid;
}
