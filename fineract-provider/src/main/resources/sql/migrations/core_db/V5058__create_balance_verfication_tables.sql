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

CREATE TABLE loan_balance_history
(
    id           BIGINT(20)     NOT NULL AUTO_INCREMENT,
    loan_id      BIGINT(20)     NOT NULL,
    balance      DECIMAL(19, 6) NOT NULL,
    derived_balance DECIMAL(19, 6),
    balance_date date           NOT NULL,
    valid BIT DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT m_loan_loan_balance_history FOREIGN KEY (loan_id) REFERENCES m_loan (id)
);

CREATE TABLE savings_balance_history
(
    id                 BIGINT(20)     NOT NULL AUTO_INCREMENT,
    savings_account_id BIGINT(20)     NOT NULL,
    balance            DECIMAL(19, 6) NOT NULL,
    derived_balance DECIMAL(19, 6),
    balance_date       date           NOT NULL,
    valid BIT DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT m_savings_account_savings_balance_history FOREIGN KEY (savings_account_id) REFERENCES m_savings_account (id)
);

INSERT INTO `m_permission` (`grouping`, `code`, `entity_name`, `action_name`, `can_maker_checker`)
VALUES ('portfolio', 'VERIFY_BALANCE', 'BALANCE', 'VERIFY', 0);