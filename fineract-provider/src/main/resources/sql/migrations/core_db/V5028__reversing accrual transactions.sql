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

DELETE FROM acc_gl_journal_entry WHERE savings_transaction_id IN (SELECT msat.id FROM m_savings_account_transaction msat WHERE msat.transaction_type_enum IN (25,22) AND msat.is_reversed = 0 AND msat.transaction_date > '2019-09-30');

UPDATE m_savings_account_transaction SET is_reversed = 1 WHERE transaction_type_enum IN (25,22) AND is_reversed = 0 AND transaction_date > '2019-09-30';