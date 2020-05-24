package com.github.ckaag.liferay.file.quota.control.menu.impl;

import com.github.ckaag.liferay.file.quota.control.menu.QuotaRetrievalService;
import com.github.ckaag.liferay.file.quota.control.menu.dto.QuotaType;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.Projection;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import java.util.HashSet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
    immediate = true,
    service = QuotaRetrievalService.class
)
public class QuotaRetrievalServiceImpl implements QuotaRetrievalService {

  public static final String SIZE = "size";


  @Activate
  private void activate() {
    System.out.println(this.getClass().getName() + " started");
  }

  @Reference
  private DLFileEntryLocalService dlFileEntryLocalService;

  @Reference
  private OrganizationLocalService organizationLocalService;

  @Override
  public Long computeUser(long userId) {
    return sumSizes(RestrictionsFactoryUtil.eq("userId", userId));
  }

  private Long sumSizes(Criterion... criteria) {
    try {
      DynamicQuery dq = dlFileEntryLocalService.dynamicQuery();
      for (Criterion criterion : criteria) {
        dq.add(criterion);
      }
      Projection projection = ProjectionFactoryUtil.sum(SIZE);
      dq.setProjection(projection);
      return (Long) dlFileEntryLocalService.dynamicQuery(dq).get(0);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Long computeGroup(long groupId) {
    return sumSizes(RestrictionsFactoryUtil.eq("groupId", groupId));
  }

  @Override
  public Long computeCompany(long companyId) {
    return sumSizes(RestrictionsFactoryUtil.eq("companyId", companyId));
  }

  private void listSites(Organization o, HashSet<Long> sites) {
    sites.add(o.getGroupId());
    for (Organization suborganization : o.getSuborganizations()) {
      listSites(suborganization, sites);
    }
  }

  @Override
  public Long computeOrganization(long organizationId) {
    try {
      HashSet<Long> gids = new HashSet<>();
      Organization o = this.organizationLocalService.fetchOrganization(organizationId);
      listSites(o, gids);
      return sumSizes(RestrictionsFactoryUtil.in("groupId", gids));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Long computeEntity(QuotaType quotaType, long entityId) {
    switch (quotaType) {
      case Organization:
        return this.computeOrganization(entityId);
      case Instance:
        return this.computeCompany(entityId);
      case Site:
        return this.computeGroup(entityId);
      case User:
        return this.computeUser(entityId);
      default:
        throw new UnsupportedOperationException("not yet implemented");
    }
  }
}
