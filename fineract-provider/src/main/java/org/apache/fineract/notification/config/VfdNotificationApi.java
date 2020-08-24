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
package org.apache.fineract.notification.config;

import java.util.Arrays;

import org.apache.fineract.notification.domain.VfdTransferNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class VfdNotificationApi {
    
    private final String DEFAULT_URL = "https://devesb.vfdbank.systems:8263/vfdbank/0.2/webhooks/notificationhook";
    private final String AUTH_TOKEN_FIELD = "VFDBankAuth";

    @Autowired
    private Environment env;

    public ResponseEntity<String> sendNotification(VfdTransferNotification notification) {

            String auth_token = this.env.getProperty("VFD_NOTIFICATION_SERVICE_AUTH_TOKEN");

            String url = this.env.getProperty("VFD_NOTIFICATION_SERVICE_URL");

            url = url == null ? DEFAULT_URL : url;

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.add(AUTH_TOKEN_FIELD, auth_token);
            HttpEntity<VfdTransferNotification> request = new HttpEntity<>(notification, headers);

            StringBuilder builder = new StringBuilder();
            builder.append(url.trim());
            builder.append("?alertType=");
            builder.append(notification.getAlertType());

        return restTemplate.postForEntity(builder.toString(), request, String.class);
    }
}
