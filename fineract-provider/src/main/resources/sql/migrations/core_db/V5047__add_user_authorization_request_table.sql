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

CREATE TABLE `m_user_client_authorization_request` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `client_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `status_enum` smallint(5) NOT NULL,
  `requestedon_date` datetime NOT NULL,
  `aprovedon_date` datetime NULL,
  `approvedon_userid` bigint(20) NULL,
  `rejectedon_date` datetime NULL,
  `rejectedon_userid` bigint(20) NULL,
  `comment` varchar(500) NULL,
  KEY `fk_m_client_authorization_request` (`client_id`),
  KEY `fk_m_appuser_authorization_request` (`user_id`),
  KEY `fk_m_appuser_approvedon_userid` (`approvedon_userid`),
  KEY `fk_m_appuser_rejectedon_userid` (`rejectedon_userid`),
  CONSTRAINT `fk_m_client_authorization_request` FOREIGN KEY (`client_id`) REFERENCES `m_client` (`id`),
  CONSTRAINT `fk_m_appuser_authorization_request` FOREIGN KEY (`user_id`) REFERENCES `m_appuser` (`id`),
  CONSTRAINT `fk_m_appuser_approvedon_userid` FOREIGN KEY (`approvedon_userid`) REFERENCES `m_appuser` (`id`),
  CONSTRAINT `fk_m_appuser_rejectedon_userid` FOREIGN KEY (`rejectedon_userid`) REFERENCES `m_appuser` (`id`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;