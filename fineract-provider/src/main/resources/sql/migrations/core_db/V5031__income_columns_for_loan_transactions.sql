--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership. The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License. You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied. See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

ALTER TABLE `m_loan_transaction`
	ADD COLUMN `income_interest_portion_derived` DECIMAL(19,6) NULL AFTER `interest_portion_derived`,
	ADD COLUMN `income_fee_charges_portion_derived` DECIMAL(19,6) NULL AFTER `fee_charges_portion_derived`,
	ADD COLUMN `income_penalty_charges_portion_derived` DECIMAL(19,6) NULL AFTER `penalty_charges_portion_derived`;

UPDATE m_loan_transaction mlt 
JOIN acc_gl_journal_entry je ON je.loan_transaction_id = mlt.id 
AND mlt.transaction_type_enum = 2 AND mlt.is_reversed = 0
JOIN acc_gl_account aga ON aga.id = je.account_id
JOIN acc_product_mapping apm ON apm.gl_account_id = aga.id 
AND apm.financial_account_type = 3
SET mlt.income_interest_portion_derived = je.amount;

UPDATE m_loan_transaction mlt 
JOIN acc_gl_journal_entry je ON je.loan_transaction_id = mlt.id 
AND mlt.transaction_type_enum = 2 AND mlt.is_reversed = 0
JOIN acc_gl_account aga ON aga.id = je.account_id
JOIN acc_product_mapping apm ON apm.gl_account_id = aga.id 
AND apm.financial_account_type = 4
SET mlt.income_fee_charges_portion_derived = je.amount;

UPDATE m_loan_transaction mlt 
JOIN acc_gl_journal_entry je ON je.loan_transaction_id = mlt.id 
AND mlt.transaction_type_enum = 2 AND mlt.is_reversed = 0
JOIN acc_gl_account aga ON aga.id = je.account_id
JOIN acc_product_mapping apm ON apm.gl_account_id = aga.id 
AND apm.financial_account_type = 5
SET mlt.income_penalty_charges_portion_derived = je.amount;