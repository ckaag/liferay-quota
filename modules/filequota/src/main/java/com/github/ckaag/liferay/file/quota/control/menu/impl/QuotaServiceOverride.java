package com.github.ckaag.liferay.file.quota.control.menu.impl;

import com.github.ckaag.liferay.file.quota.control.menu.QuotaCacheService;
import com.github.ckaag.liferay.file.quota.control.menu.QuotaRetrievalService;
import com.github.ckaag.liferay.file.quota.control.menu.dto.QuotaType;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.model.DLVersionNumberIncrease;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryLocalServiceWrapper;
import com.liferay.dynamic.data.mapping.kernel.DDMFormValues;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceWrapper;
import com.liferay.portal.kernel.service.UserLocalService;
import java.io.File;
import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
    immediate = true,
    service = ServiceWrapper.class
)
public class QuotaServiceOverride extends DLFileEntryLocalServiceWrapper {
  //check that throws exception if added / updated file entry is outside limits

  // not yet testing the other versions to also catch all other types of inserts (currently does not work on simple file drop)

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

  private Map<QuotaType, Set<Long>> getChangingQuotasTo(DLFileEntry dlFileEntry)
      throws PortalException {
    //in this method we extract all quotatypes we will touch in the other methods
    //this can be considered a logical constant based on Liferay's datastructure for asset attributes
    Map<QuotaType, Set<Long>> res = new HashMap<>();
    res.put(QuotaType.Instance, singleSet(dlFileEntry.getCompanyId()));
    res.put(QuotaType.Site, singleSet(dlFileEntry.getGroupId()));
    res.put(QuotaType.User, singleSet(dlFileEntry.getUserId()));
    User user = userLocalService.fetchUser(dlFileEntry.getUserId());
    if (user != null) {
      HashSet<Long> orgIds = new HashSet<>();
      for (int i = 0; i < user.getOrganizationIds().length; i++) {
        orgIds.add(user.getOrganizationIds()[i]);
      }
      res.put(QuotaType.Organization, orgIds);
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
    try {
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
    } catch (PortalException e) {
      throw new RuntimeException(e);
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
      throw new RuntimeException(
          new PortalException("Update to FileEntry " + updatingdlFileEntry.getFileEntryId()
              + " would exceed current file quotas"));
    }
  }

  private DLFileEntry buildSimpleMockFileEntry(long size, long groupId,
      long userId) {
    DLFileEntry dl = this.createDLFileEntry(-1);
    dl.setGroupId(groupId);
    dl.setSize(size);
    dl.setUserId(userId);
    dl.setCompanyId(GroupLocalServiceUtil.fetchGroup(groupId).getCompanyId());
    return dl;
  }

  private DLFileEntry updateCacheOnPlainAdd(Supplier<DLFileEntry> supplier, long groupId,
      long userId, long size) {
    DLFileEntry updatingdlFileEntry = buildSimpleMockFileEntry(size, groupId, userId);
    if (willQuotasBeFulfilledAfterUpdate(updatingdlFileEntry, false)) {
      DLFileEntry dlFileEntry = supplier.get();
      this.quotaCacheService.update(dlFileEntry);
      return dlFileEntry;
    } else {
      throw new RuntimeException(
          new PortalException("Update to FileEntry " + updatingdlFileEntry.getFileEntryId()
              + " would exceed current file quotas"));
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
    try {
      return updateCache(f -> {
        try {
          return super.deleteFileEntry(f);
        } catch (PortalException e) {
          throw new RuntimeException(e);
        }
      }, dlFileEntry, true);
    } catch (RuntimeException e) {
      if (e.getCause() instanceof PortalException) {
        throw (PortalException) e.getCause();
      } else {
        throw e;
      }
    }
  }


  @Override
  public DLFileEntry updateDLFileEntry(DLFileEntry dlFileEntry) {
    return updateCache(f -> super.updateDLFileEntry(f), dlFileEntry, false);
  }


  @Override
  public DLFileEntry addFileEntry(long userId, long groupId, long repositoryId, long folderId,
      String sourceFileName, String mimeType, String title, String description, String changeLog,
      long fileEntryTypeId, Map<String, DDMFormValues> ddmFormValuesMap, File file, InputStream is,
      long size, ServiceContext serviceContext) throws PortalException { //triggers on file drop
    try {
      return updateCacheOnPlainAdd(() -> {
        try {
          return super
              .addFileEntry(userId, groupId, repositoryId, folderId, sourceFileName, mimeType,
                  title,
                  description, changeLog, fileEntryTypeId, ddmFormValuesMap, file, is, size,
                  serviceContext);
        } catch (PortalException e) {
          throw new RuntimeException(e);
        }
      }, groupId, userId, size);
    } catch (RuntimeException e) {
      if (e.getCause() instanceof PortalException) {
        throw (PortalException) (e.getCause());
      } else {
        throw e;
      }
    }
  }

  @Override
  public void deleteFileEntries(long groupId, long folderId) throws PortalException {
    super.deleteFileEntries(groupId, folderId);
  }

  @Override
  public void deleteFileEntries(long groupId, long folderId, boolean includeTrashedEntries)
      throws PortalException {
    super.deleteFileEntries(groupId, folderId, includeTrashedEntries);
  }

  @Override
  public DLFileEntry deleteFileEntry(long fileEntryId) throws PortalException {
    return super.deleteFileEntry(fileEntryId);
  }

  @Override
  public DLFileEntry deleteFileEntry(long userId, long fileEntryId) throws PortalException {
    return super.deleteFileEntry(userId, fileEntryId);
  }

  @Override
  public DLFileEntry deleteFileVersion(long userId, long fileEntryId, String version)
      throws PortalException {
    return super.deleteFileVersion(userId, fileEntryId, version);
  }

  @Override
  public DLFileEntry updateFileEntry(long userId, long fileEntryId, String sourceFileName,
      String mimeType, String title, String description, String changeLog,
      DLVersionNumberIncrease dlVersionNumberIncrease, long fileEntryTypeId,
      Map<String, DDMFormValues> ddmFormValuesMap, File file, InputStream is, long size,
      ServiceContext serviceContext) throws PortalException {
    return super.updateFileEntry(userId, fileEntryId, sourceFileName, mimeType, title, description,
        changeLog, dlVersionNumberIncrease, fileEntryTypeId, ddmFormValuesMap, file, is, size,
        serviceContext);
  }

  @Override
  public DLFileEntry updateFileEntryType(long userId, long fileEntryId, long fileEntryTypeId,
      ServiceContext serviceContext) throws PortalException {
    return super.updateFileEntryType(userId, fileEntryId, fileEntryTypeId, serviceContext);
  }
}
