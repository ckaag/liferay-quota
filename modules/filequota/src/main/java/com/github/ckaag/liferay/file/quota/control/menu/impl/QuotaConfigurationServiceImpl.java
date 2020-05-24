package com.github.ckaag.liferay.file.quota.control.menu.impl;

import com.github.ckaag.liferay.file.quota.control.menu.FileQuotaConfiguration;
import com.github.ckaag.liferay.file.quota.control.menu.QuotaConfigurationService;
import com.github.ckaag.liferay.file.quota.control.menu.dto.QuotaType;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import java.util.Arrays;
import java.util.Map;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

@Component(configurationPid = QuotaConfigurationService.CONF_ID, immediate = true, service = QuotaConfigurationService.class)
public class QuotaConfigurationServiceImpl implements QuotaConfigurationService {

  @Activate
  @Modified
  protected void activate(Map<String, Object> properties) {
    System.out.println(this.getClass().getName() + " started");
    quotaConfiguration = ConfigurableUtil.createConfigurable(
        FileQuotaConfiguration.class, properties);
  }


  private volatile FileQuotaConfiguration quotaConfiguration;


  private static Long parseLongOnly(String nr) {
    if (nr == null || nr.isBlank()) {
      return null;
    }
    try {
      return Long.valueOf(nr.trim());
    } catch (Exception e) {
      return null;
    }
  }

  private static Long parseLongAfterPrefix(String prefix, String nr) {
    return parseLongOnly(nr.substring(prefix.length()));
  }

  @Override
  public Long getQuotaOrNullForEntityId(QuotaType quotaType, long entityId) {
    String prefix = "" + entityId + ":";
    switch (quotaType) {
      case User:
        return Arrays.stream(quotaConfiguration.userSpecificByteQuotas())
            .filter(t -> t.startsWith(prefix)).map(t -> parseLongAfterPrefix(prefix, t)).findFirst()
            .orElse(parseLongOnly(quotaConfiguration.userGeneralByteQuota()));
      case Site:
        return Arrays.stream(quotaConfiguration.siteSpecificByteQuotas())
            .filter(t -> t.startsWith(prefix)).map(t -> parseLongAfterPrefix(prefix, t)).findFirst()
            .orElse(parseLongOnly(quotaConfiguration.siteGeneralByteQuota()));
      case Organization:
        return Arrays.stream(quotaConfiguration.organizationSpecificByteQuotas())
            .filter(t -> t.startsWith(prefix)).map(t -> parseLongAfterPrefix(prefix, t)).findFirst()
            .orElse(parseLongOnly(quotaConfiguration.organizationGeneralByteQuota()));
      case Instance:
        return Arrays.stream(quotaConfiguration.instanceSpecificByteQuotas())
            .filter(t -> t.startsWith(prefix)).map(t -> parseLongAfterPrefix(prefix, t)).findFirst()
            .orElse(parseLongOnly(quotaConfiguration.instanceGeneralByteQuota()));
      default:
        throw new UnsupportedOperationException("not yet implemented");
    }
  }
}
