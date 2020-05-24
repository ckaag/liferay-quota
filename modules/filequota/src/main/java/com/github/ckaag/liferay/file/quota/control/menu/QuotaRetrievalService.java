package com.github.ckaag.liferay.file.quota.control.menu;

import com.github.ckaag.liferay.file.quota.control.menu.dto.QuotaType;

public interface QuotaRetrievalService {

  Long computeUser(long userId);

  Long computeGroup(long groupId);

  Long computeCompany(long companyId);

  Long computeOrganization(long organizationId);

  Long computeEntity(QuotaType quotaType, long entityId);
}
