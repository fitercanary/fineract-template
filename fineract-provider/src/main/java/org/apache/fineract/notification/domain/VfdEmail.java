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
package org.apache.fineract.notification.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayOutputStream;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VfdEmail {

    private ByteArrayOutputStream file;
    private String attachmentType;
    private Date startDate;
    private Date endDate;
    private Integer clientId;
    private String accountNo;

    public VfdEmail(ByteArrayOutputStream file, String attachmentType, Date startDate, Date endDate, Integer clientId, String accountNo) {
        this.file = file;
        this.attachmentType = attachmentType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.clientId = clientId;
        this.accountNo = accountNo;
    }

    public ByteArrayOutputStream getFile() {
        return file;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Integer getClientId() {
        return clientId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    @Override
    public String toString() {
        return "VfdEmail{" +
                "file=" + file +
                ", attachmentType='" + attachmentType + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", clientId=" + clientId +
                ", accountNo='" + accountNo + '\'' +
                '}';
    }

    public static MultiValueMap<String, Object> toMultiValueMap(VfdEmail email, String fileName){

        // This nested HttpEntiy is important to create the correct
        // Content-Disposition entry with metadata "name" and "filename"
        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<String, String>();
        fileMap.add("Content-Disposition", String.format("form-data; name=\"file\"; filename=\"%s\"", fileName));
        //"attachment; filename=" + "statement"
        //String.format("form-data; name=\"file\"; filename=\"%s\"", theMultipartFile.getName())

        HttpEntity<byte[]> fileEntity = new HttpEntity<>(email.getFile().toByteArray(), fileMap);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("file", fileEntity);
        body.add("attachmentType", email.getAttachmentType());
        body.add("startDate", email.getStartDate());
        body.add("endDate", email.getEndDate());
        body.add("clientId", email.getClientId());
        body.add("accountNo", email.getAccountNo());

        return body;
    }
}
