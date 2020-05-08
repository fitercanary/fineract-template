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


INSERT INTO `m_code` ( `code_name`, `is_system_defined`) VALUES ('SavingsAccountBlockUnblockNarration', 1);

INSERT INTO `m_code_value` ( `code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES ((select code.id from m_code code where code.code_name = 'SavingsAccountBlockUnblockNarration'), 'Compliance', 'Compliance', 1, null, 1, 0);
INSERT INTO `m_code_value` ( `code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES ((select code.id from m_code code where code.code_name = 'SavingsAccountBlockUnblockNarration'), 'Fraud', 'Fraud', 2, null, 1, 0);
INSERT INTO `m_code_value` ( `code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES ((select code.id from m_code code where code.code_name = 'SavingsAccountBlockUnblockNarration'), 'Legal', 'Legal', 3, null, 1, 0);