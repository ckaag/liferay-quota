package com.github.ckaag.liferay.file.quota.control.menu.impl;

import com.github.ckaag.liferay.file.quota.control.menu.QuotaCacheService;
import com.github.ckaag.liferay.file.quota.control.menu.QuotaConfigurationService;
import com.github.ckaag.liferay.file.quota.control.menu.QuotaRetrievalService;
import com.github.ckaag.liferay.file.quota.control.menu.dto.QuotaType;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
    immediate = true,
    service = QuotaCacheService.class
)
public class QuotaCacheServiceImpl implements QuotaCacheService {


  @Activate
  private void activate() {
    System.out.println(this.getClass().getName() + " started");
  }

  private final Map<QuotaType, Map<Long, Long>> _cache = new HashMap<>();


  @Reference
  private QuotaRetrievalService quotaRetrievalService;

  @Reference
  private QuotaConfigurationService quotaConfigurationService;


  @Override
  public AbstractMap.SimpleEntry<Long, Long> getForEntityCached(QuotaType quotaType,
      long entityId) {
    return new AbstractMap.SimpleEntry<>(
        this._cache.computeIfAbsent(quotaType, k -> new HashMap<>()).computeIfAbsent(entityId,
            id -> this.quotaRetrievalService.computeEntity(quotaType, entityId)),
        this.getEntityConfiguredLimit(quotaType, entityId));
  }

  @Override
  public Long computeEntityUsage(QuotaType quotaType, long entityId) {
    switch (quotaType) {
      case User:
        this.quotaRetrievalService.computeUser(entityId);
      case Site:
        this.quotaRetrievalService.computeGroup(entityId);
      case Instance:
        this.quotaRetrievalService.computeCompany(entityId);
      case Organization:
        this.quotaRetrievalService.computeOrganization(entityId);
      default:
        throw new UnsupportedOperationException("not yet implemented");
    }
  }

  @Override
  public Long getEntityConfiguredLimit(QuotaType quotaType, long entityId) {
    return this.quotaConfigurationService.getQuotaOrNullForEntityId(quotaType, entityId);
  }

  @Override
  public void updateEntity(QuotaType quotaType, long entityId) {
    this._cache.computeIfAbsent(quotaType, k -> new HashMap<>())
        .put(entityId, this.computeEntityUsage(quotaType, entityId));
  }


}
