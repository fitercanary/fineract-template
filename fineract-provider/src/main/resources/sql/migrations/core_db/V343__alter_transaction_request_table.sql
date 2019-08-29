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

-- ALTER TABLE m_transaction_request MODIFY COLUMN id bigint(20) NOT NULL AUTO_INCREMENT;
--
-- ALTER TABLE `m_transaction_request` CHANGE transaction_id transaction_id BIGINT(20) NOT NULL;
--
-- ALTER TABLE `m_transaction_request` ADD CONSTRAINT fk_savings_transaction FOREIGN KEY (transaction_id) REFERENCES m_savings_account_transaction (id);

CREATE TABLE `m_transaction_request` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `del_flag` varchar(255) DEFAULT NULL,
  `deleted_on` datetime DEFAULT NULL,
  `version` int(11) NOT NULL,
  `category` varchar(255) DEFAULT NULL,
  `image_tag` longtext,
  `latitude` varchar(255) DEFAULT NULL,
  `longitude` varchar(255) DEFAULT NULL,
  `note_image` longtext,
  `notes` varchar(255) DEFAULT NULL,
  `remarks` varchar(255) DEFAULT NULL,
  `transaction_brand_name` varchar(255) DEFAULT NULL,
  `transaction_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_savings_transaction` (`transaction_id`)
) ENGINE=MyISAM AUTO_INCREMENT=349 DEFAULT CHARSET=latin1;