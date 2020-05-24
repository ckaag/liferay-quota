package com.github.ckaag.liferay.file.quota.control.menu.impl;

import com.github.ckaag.liferay.file.quota.control.menu.QuotaCacheService;
import com.github.ckaag.liferay.file.quota.control.menu.dto.QuotaType;
import com.github.ckaag.liferay.file.quota.control.menu.dto.StatusLine;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.service.OrganizationLocalServiceUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.product.navigation.control.menu.BaseJSPProductNavigationControlMenuEntry;
import com.liferay.product.navigation.control.menu.ProductNavigationControlMenuEntry;
import com.liferay.product.navigation.control.menu.constants.ProductNavigationControlMenuCategoryKeys;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
    immediate = true,
    property = {
        "product.navigation.control.menu.category.key="
            + ProductNavigationControlMenuCategoryKeys.TOOLS,
        "product.navigation.control.menu.category.order:Integer=100"
    },
    service = ProductNavigationControlMenuEntry.class
)
public class QuotaControlMenuEntry extends BaseJSPProductNavigationControlMenuEntry {


  public static final String DL_ADMIN_PORTLET_NAME = "com_liferay_document_library_web_portlet_DLAdminPortlet";
  public static final String LIFERAY_SHARED_THEME_DISPLAY = "LIFERAY_SHARED_THEME_DISPLAY";
  public static final String CONTENT_LANGUAGE = "content.Language";
  public static final String LINES = "lines";
  public static final String QUOTAINUSE = "quotainuse";
  public static final String QUOTA_IN_USE = "quota.in.use";

  @Activate
  private void activate() {
    System.out.println(this.getClass().getName() + " started");
  }

  @Override
  public String getIconJspPath() {
    return "/html/control_menu_entry.jsp";
  }

  @Reference
  private QuotaCacheService quotaCacheService;

  @Override
  @Reference(
      target = "(osgi.web.symbolicname=filequota)",
      unbind = "-"
  )
  public void setServletContext(ServletContext servletContext) {
    super.setServletContext(servletContext);
  }

  @Override
  protected boolean include(HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse, String jspPath) throws IOException {
    ThemeDisplay td = (ThemeDisplay) httpServletRequest.getAttribute(LIFERAY_SHARED_THEME_DISPLAY);
    httpServletRequest.setAttribute(QUOTAINUSE, translate(httpServletRequest, QUOTA_IN_USE));
    try {
      httpServletRequest.setAttribute(LINES, getAsStatusLine(td));
    } catch (PortalException e) {
      throw new RuntimeException(e);
    }
    return super.include(httpServletRequest, httpServletResponse, jspPath);
  }

  @Override
  public boolean isShow(HttpServletRequest httpServletRequest) {
    String portletName = ((ThemeDisplay) httpServletRequest
        .getAttribute(LIFERAY_SHARED_THEME_DISPLAY)).getPortletDisplay().getPortletName();
    return portletName.equals(DL_ADMIN_PORTLET_NAME);
  }


  private List<StatusLine> getAsStatusLine(ThemeDisplay themeDisplay) throws PortalException {
    List<StatusLine> list = new ArrayList<>();
    list.add(
        new StatusLine(
            quotaCacheService.getForEntityCached(QuotaType.User, themeDisplay.getUserId()),
            themeDisplay.getUser().getFullName())
    );
    list.add(
        new StatusLine(
            quotaCacheService.getForEntityCached(QuotaType.Site, themeDisplay.getScopeGroupId()),
            themeDisplay.getScopeGroup().getDescriptiveName())
    );
    list.add(
        new StatusLine(
            quotaCacheService.getForEntityCached(QuotaType.Instance, themeDisplay.getCompanyId()),
            themeDisplay.getCompany().getName())
    );
    for (long organizationId : themeDisplay.getUser().getOrganizationIds()) {
      list.add(
          new StatusLine(
              quotaCacheService.getForEntityCached(QuotaType.Organization, organizationId),
              OrganizationLocalServiceUtil.fetchOrganization(organizationId).getName())
      );
    }
    return list;
  }

  private String translate(HttpServletRequest request, String key) {
    ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(
        CONTENT_LANGUAGE, request.getLocale(), getClass());
    return LanguageUtil.get(resourceBundle, key);
  }
}
