package com.github.ckaag.liferay.file.quota.control.menu;

import com.github.ckaag.liferay.file.quota.control.menu.dto.QuotaType;

public interface QuotaConfigurationService {

  String CONF_ID = "com.github.ckaag.liferay.file.quota.FileQuotaConfiguration";

  Long getQuotaOrNullForEntityId(QuotaType quotaType, long entityId);
}