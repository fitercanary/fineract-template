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
	ADD COLUMN `is_account_transfer` TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE `m_savings_account_transaction`
	ADD COLUMN `is_account_transfer` TINYINT(1) NOT NULL DEFAULT 0;

update m_loan_transaction set is_account_transfer = 1 where id in (SELECT mtt.from_loan_transaction_id FROM m_account_transfer_transaction mtt);

update m_loan_transaction set is_account_transfer = 1 where id in (SELECT mtt.to_loan_transaction_id FROM m_account_transfer_transaction mtt);

update m_savings_account_transaction set is_account_transfer = 1 where id in (SELECT mtt.to_savings_transaction_id FROM m_account_transfer_transaction mtt);

update m_savings_account_transaction set is_account_transfer = 1 where id in (SELECT mtt.from_savings_transaction_id FROM m_account_transfer_transaction mtt);

