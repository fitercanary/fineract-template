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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.notification.domain.VfdTransferNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class VfdServiceApi {
    
    private final String NOTIFICATION_SERVICE_DEFAULT_URL = "https://devesb.vfdbank.systems:8263/vfdbank/0.2/webhooks/notificationhook?alertType=both";
    private final String NOTIFICATION_SERVICE_AUTH_TOKEN_FIELD = "VFDBankAuth";
    private final String NOTIFICATION_SERVICE_DEFAULT_AUTH_TOKEN = "Bearer 73d64eb6-15fd-35df-a543-4a5ae672c455";

    private final String EMAIL_SERVICE_DEFAULT_URL = "http://172.31.11.10:9092/notification/attachment";

    private final String DEFAULT_URL = "https://devesb.vfdbank.systems:8263/vfdbank/0.2/webhooks/notificationhook";
    private final String AUTH_TOKEN_FIELD = "VFDBankAuth";

    @Autowired
    private Environment env;

    private final static Logger logger = LoggerFactory.getLogger(VfdServiceApi.class);

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

    public void sendSavingsAccountStatementEmail( String toAddress, Long clientId, String attachmentName,
                                                                    String contentType,
                                                                    File file){

        RestTemplate restTemplate = new RestTemplate();
        LinkedMultiValueMap<String, Object> valueMap = new LinkedMultiValueMap<>();

        valueMap.add("clientId", String.valueOf(clientId.longValue()));
        valueMap.add("toAddress", toAddress);
        valueMap.add("attachmentName", attachmentName);
        valueMap.add("contentType", contentType);//"contentType": "application/pdf"
        FileSystemResource value = new FileSystemResource(file);
        valueMap.add("file", value);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(valueMap, headers);

        String url = this.env.getProperty("VFD_EMAIL_SERVICE_URL");
        url = url != null ? url : EMAIL_SERVICE_DEFAULT_URL;

        try{

            final ResponseEntity<Void> stringResponseEntity = restTemplate.postForEntity(url, requestEntity, Void.class);
            logger.info("Savings Account Statement has been sent to vfd email service");

        }catch (RestClientException e) {
            if(e instanceof HttpClientErrorException){
                HttpClientErrorException message = (HttpClientErrorException) e;

                logger.error("Rest Client Error sending account statement to email service: Status Code = " + message.getStatusCode().value() +
                        ", Error Message = " + message.getMessage());
            }
            logger.error("Rest Client Error sending account statement to email service: "  , e);
            throw new PlatformDataIntegrityException("error.msg.reporting.error", e.getMessage());
        }catch (Exception e){
            logger.error("General Error sending account statement to email service: " , e);
            throw new PlatformDataIntegrityException("error.msg.reporting.error", e.getMessage());
        }

    }
}
