package com.github.ckaag.liferay.file.quota.control.menu.impl;

import com.github.ckaag.liferay.file.quota.control.menu.QuotaCacheService;
import com.github.ckaag.liferay.file.quota.control.menu.QuotaRetrievalService;
import com.github.ckaag.liferay.file.quota.control.menu.dto.QuotaType;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryLocalServiceWrapper;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceWrapper;
import com.liferay.portal.kernel.service.UserLocalService;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
    immediate = true,
    service = ServiceWrapper.class
)
public class QuotaServiceOverride extends DLFileEntryLocalServiceWrapper {
  //check that throws exception if added / updated file entry is outside limits

  //TODO: implement the other versions to also catch all other types of inserts (currently does not work on simple file drop)

  @Activate
  private void activate() {
    System.out.println(this.getClass().getName() + " started");
  }

  @Reference
  private QuotaCacheService quotaCacheService;

  @Reference
  private QuotaRetrievalService quotaRetrievalService;

  @Reference
  private UserLocalService userLocalService;

  public QuotaServiceOverride() {
    super(null);
  }

  public QuotaServiceOverride(DLFileEntryLocalService dlFileEntryLocalService) {
    super(dlFileEntryLocalService);
  }

  private Map<QuotaType, Set<Long>> getChangingQuotasTo(DLFileEntry dlFileEntry) {
    //in this method we extract all quotatypes we will touch in the other methods
    //this can be considered a logical constant based on Liferay's datastructure for asset attributes
    Map<QuotaType, Set<Long>> res = new HashMap<>();
    res.put(QuotaType.Instance, singleSet(dlFileEntry.getCompanyId()));
    res.put(QuotaType.Site, singleSet(dlFileEntry.getGroupId()));
    res.put(QuotaType.User, singleSet(dlFileEntry.getUserId()));
    User user = userLocalService.fetchUser(dlFileEntry.getUserId());
    if (user != null) {
      try {
        HashSet<Long> orgIds = new HashSet<>();
        for (int i = 0; i < user.getOrganizationIds().length; i++) {
          orgIds.add(user.getOrganizationIds()[i]);
        }
        res.put(QuotaType.Organization, orgIds);
      } catch (PortalException e) {
        e.printStackTrace();
      }
    }
    return res;
  }

  private Set<Long> singleSet(long id) {
    HashSet<Long> h = new HashSet<>();
    h.add(id);
    return h;
  }

  private Long getRemaining(QuotaType quotaType, long entityId) {
    //query cache service and return the remaining
    SimpleEntry<Long, Long> q = quotaCacheService.getForEntityCached(quotaType, entityId);
    if (q != null) {
      if (q.getKey() != null) {
        if (q.getValue() != null) {
          return q.getValue() - q.getKey();
        }
      }
    }
    return null;
  }

  private boolean willQuotasBeFulfilledAfterUpdate(DLFileEntry newDlFileEntry, boolean isDelete) {
    if (isDelete) {
      return true;
    }
    if (newDlFileEntry == null) {
      return true;
    }
    final DLFileEntry oldDlFileEntry = super.fetchDLFileEntry(newDlFileEntry.getFileEntryId());
    final Map<QuotaType, Set<Long>> newQuotas = getChangingQuotasTo(newDlFileEntry);
    final Map<QuotaType, Set<Long>> oldQuotas =
        oldDlFileEntry != null ? getChangingQuotasTo(newDlFileEntry) : new HashMap<>();
    //we check for every quota that the change that will haven on add / update does not cause us to exceed any of the given quotas
    for (Entry<QuotaType, Set<Long>> newQuotaPair : newQuotas.entrySet()) {
      for (Long id : newQuotaPair.getValue()) {
        if (id != null) {
          boolean existsBefore = (oldQuotas.getOrDefault(newQuotaPair.getKey(), new HashSet<>())
              .contains(id));
          long addIncrement = existsBefore ? (newDlFileEntry.getSize() - oldDlFileEntry.getSize())
              : newDlFileEntry.getSize();
          Long currentQuotaLimit = getRemaining(newQuotaPair.getKey(), id);
          if (currentQuotaLimit != null && addIncrement > currentQuotaLimit) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private DLFileEntry updateCache(Function<DLFileEntry, DLFileEntry> func,
      DLFileEntry updatingdlFileEntry, boolean isDelete) {
    if (willQuotasBeFulfilledAfterUpdate(updatingdlFileEntry, isDelete)) {
      DLFileEntry previous =
          updatingdlFileEntry != null ? this.fetchDLFileEntry(updatingdlFileEntry.getFileEntryId())
              : null;
      DLFileEntry dlFileEntry = func.apply(updatingdlFileEntry);
      this.quotaCacheService.update(dlFileEntry);
      this.quotaCacheService.update(previous);
      return dlFileEntry;
    } else {
      throw new RuntimeException("Update to FileEntry " + updatingdlFileEntry.getFileEntryId()
          + " would exceed current file quotas");
    }
  }

  @Override
  public DLFileEntry addDLFileEntry(DLFileEntry dlFileEntry) {
    return updateCache(f -> super.addDLFileEntry(f), dlFileEntry, false);
  }

  @Override
  public DLFileEntry deleteDLFileEntry(DLFileEntry dlFileEntry) {
    return updateCache(f -> super.deleteDLFileEntry(f), dlFileEntry, true);
  }


  @Override
  public DLFileEntry deleteFileEntry(DLFileEntry dlFileEntry) throws PortalException {
    return updateCache(f -> {
      try {
        return super.deleteFileEntry(f);
      } catch (PortalException e) {
        e.printStackTrace();
        return null;
      }
    }, dlFileEntry, true);
  }


  @Override
  public DLFileEntry updateDLFileEntry(DLFileEntry dlFileEntry) {
    return updateCache(f -> super.updateDLFileEntry(f), dlFileEntry, false);
  }


}
