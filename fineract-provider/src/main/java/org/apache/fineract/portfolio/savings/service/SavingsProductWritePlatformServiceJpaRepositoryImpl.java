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
package org.apache.fineract.portfolio.savings.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.PersistenceException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.fineract.accounting.producttoaccountmapping.service.ProductToGLAccountMappingWritePlatformService;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityAccessType;
import org.apache.fineract.infrastructure.entityaccess.service.FineractEntityAccessUtil;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.data.SavingsProductDataValidator;
import org.apache.fineract.portfolio.savings.domain.SavingsProduct;
import org.apache.fineract.portfolio.savings.domain.SavingsProductAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsProductRepository;
import org.apache.fineract.portfolio.savings.exception.SavingsProductNotFoundException;
import org.apache.fineract.portfolio.tax.domain.TaxGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.apache.fineract.portfolio.savings.SavingsApiConstants.*;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.savingsProductDepositCategoryParamName;

@Service
public class SavingsProductWritePlatformServiceJpaRepositoryImpl implements SavingsProductWritePlatformService {

    private final Logger logger;
    private final PlatformSecurityContext context;
    private final SavingsProductRepository savingProductRepository;
    private final SavingsProductDataValidator fromApiJsonDataValidator;
    private final SavingsProductAssembler savingsProductAssembler;
    private final ProductToGLAccountMappingWritePlatformService accountMappingWritePlatformService;
    private final FineractEntityAccessUtil fineractEntityAccessUtil;
    private final CodeValueRepositoryWrapper codeValueRepositoryWrapper;

    @Autowired
    public SavingsProductWritePlatformServiceJpaRepositoryImpl(final PlatformSecurityContext context,
            final SavingsProductRepository savingProductRepository, final SavingsProductDataValidator fromApiJsonDataValidator,
            final SavingsProductAssembler savingsProductAssembler,
            final ProductToGLAccountMappingWritePlatformService accountMappingWritePlatformService,
            final FineractEntityAccessUtil fineractEntityAccessUtil, final CodeValueRepositoryWrapper codeValueRepositoryWrapper) {
        this.context = context;
        this.savingProductRepository = savingProductRepository;
        this.fromApiJsonDataValidator = fromApiJsonDataValidator;
        this.savingsProductAssembler = savingsProductAssembler;
        this.logger = LoggerFactory.getLogger(SavingsProductWritePlatformServiceJpaRepositoryImpl.class);
        this.accountMappingWritePlatformService = accountMappingWritePlatformService;
        this.fineractEntityAccessUtil = fineractEntityAccessUtil;
        this.codeValueRepositoryWrapper = codeValueRepositoryWrapper;
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dae) {

        if (realCause.getMessage().contains("sp_unq_name")) {

            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.product.savings.duplicate.name", "Savings product with name `" + name
                    + "` already exists", "name", name);
        } else if (realCause.getMessage().contains("sp_unq_short_name")) {

            final String shortName = command.stringValueOfParameterNamed("shortName");
            throw new PlatformDataIntegrityException("error.msg.product.savings.duplicate.short.name", "Savings product with short name `"
                    + shortName + "` already exists", "shortName", shortName);
        }

        logAsErrorUnexpectedDataIntegrityException(dae);
        throw new PlatformDataIntegrityException("error.msg.savingsproduct.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    private void logAsErrorUnexpectedDataIntegrityException(final Exception dae) {
        this.logger.error(dae.getMessage(), dae);
    }

    @Transactional
    @Override
    public CommandProcessingResult create(final JsonCommand command) {

        try {
            this.fromApiJsonDataValidator.validateForCreate(command.json());

            final SavingsProduct product = this.savingsProductAssembler.assemble(command);

            this.savingProductRepository.save(product);

            // save accounting mappings
            this.accountMappingWritePlatformService.createSavingProductToGLAccountMapping(product.getId(), command,
                    DepositAccountType.SAVINGS_DEPOSIT);

            // check if the office specific products are enabled. If yes, then
            // save this savings product against a specific office
            // i.e. this savings product is specific for this office.
            fineractEntityAccessUtil.checkConfigurationAndAddProductResrictionsForUserOffice(
                    FineractEntityAccessType.OFFICE_ACCESS_TO_SAVINGS_PRODUCTS, product.getId());

            return new CommandProcessingResultBuilder() //
                    .withEntityId(product.getId()) //
                    .build();
        } catch (final DataAccessException e) {
            handleDataIntegrityIssues(command, e.getMostSpecificCause(), e);
            return CommandProcessingResult.empty();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleDataIntegrityIssues(command, throwable, dve);
        	return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult update(final Long productId, final JsonCommand command) {

        try {
            this.context.authenticatedUser();
            final SavingsProduct product = this.savingProductRepository.findOne(productId);
            if (product == null) { throw new SavingsProductNotFoundException(productId); }

            this.fromApiJsonDataValidator.validateForUpdate(command.json(), product);

            final Map<String, Object> changes = product.update(command);

            if (changes.containsKey(chargesParamName)) {
                final Set<Charge> savingsProductCharges = this.savingsProductAssembler.assembleListOfSavingsProductCharges(command, product
                        .currency().getCode());
                final boolean updated = product.update(savingsProductCharges);
                if (!updated) {
                    changes.remove(chargesParamName);
                }
            }

            if (changes.containsKey(taxGroupIdParamName)) {
                final TaxGroup taxGroup = this.savingsProductAssembler.assembleTaxGroup(command);
                product.setTaxGroup(taxGroup);
                if (product.withHoldTax() && product.getTaxGroup() == null) {
                    final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
                    final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                            .resource(SAVINGS_PRODUCT_RESOURCE_NAME);
                    final Long taxGroupId = null;
                    baseDataValidator.reset().parameter(taxGroupIdParamName).value(taxGroupId).notBlank();
                    throw new PlatformApiDataValidationException(dataValidationErrors);
                }
            }

            // accounting related changes
            final boolean accountingTypeChanged = changes.containsKey(accountingRuleParamName);
            final Map<String, Object> accountingMappingChanges = this.accountMappingWritePlatformService
                    .updateSavingsProductToGLAccountMapping(product.getId(), command, accountingTypeChanged, product.getAccountingType(),
                            DepositAccountType.SAVINGS_DEPOSIT);
            changes.putAll(accountingMappingChanges);

            /* Savings Product Deposit Category Changes */
            if (changes.containsKey(savingsProductDepositCategoryParamName)) {
                final Long savingsProductDepositCategoryIdLongValue = command.longValueOfParameterNamed(savingsProductDepositCategoryParamName);
                CodeValue savingsProductDepositCategory = null;
                if (savingsProductDepositCategoryIdLongValue != null) {
                    savingsProductDepositCategory = this.codeValueRepositoryWrapper.findOneWithNotFoundDetection(savingsProductDepositCategoryIdLongValue);
                }
                product.setSavingsProductDepositCategory(savingsProductDepositCategory);
            }

            if (!changes.isEmpty()) {
                this.savingProductRepository.saveAndFlush(product);
            }

            return new CommandProcessingResultBuilder() //
                    .withEntityId(product.getId()) //
                    .with(changes).build();
        } catch (final DataAccessException e) {
            handleDataIntegrityIssues(command, e.getMostSpecificCause(), e);
            return CommandProcessingResult.empty();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleDataIntegrityIssues(command, throwable, dve);
        	return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult delete(final Long productId) {

        this.context.authenticatedUser();
        final SavingsProduct product = this.savingProductRepository.findOne(productId);
        if (product == null) { throw new SavingsProductNotFoundException(productId); }

        this.savingProductRepository.delete(product);

        return new CommandProcessingResultBuilder() //
                .withEntityId(product.getId()) //
                .build();
    }

}