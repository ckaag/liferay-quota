package com.github.ckaag.liferay.file.quota.control.menu;

import com.github.ckaag.liferay.file.quota.control.menu.dto.QuotaType;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import java.util.AbstractMap;

public interface QuotaCacheService {

  AbstractMap.SimpleEntry<Long, Long> getForEntityCached(QuotaType quotaType, long entityId);

  Long computeEntityUsage(QuotaType quotaType, long entityId);

  Long getEntityConfiguredLimit(QuotaType quotaType, long entityId);

  void updateEntity(QuotaType quotaType, long entityId);


  default void update(DLFileEntry dlFileEntry) {
    if (dlFileEntry != null) {
      User user = UserLocalServiceUtil.fetchUser(dlFileEntry.getUserId());
      if (user != null) {
        try {
          for (long organizationId : user.getOrganizationIds()) {
            this.updateEntity(QuotaType.Organization, organizationId);
          }
        } catch (PortalException e) {
          e.printStackTrace();
        }
      }
      this.updateEntity(QuotaType.Instance, dlFileEntry.getCompanyId());
      this.updateEntity(QuotaType.Site, dlFileEntry.getGroupId());
      this.updateEntity(QuotaType.User, dlFileEntry.getUserId());
    }
  }
}
