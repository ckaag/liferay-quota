package com.github.ckaag.liferay.file.quota.control.menu;

import aQute.bnd.annotation.metatype.Meta;

@Meta.OCD(id = QuotaConfigurationService.CONF_ID)
public interface FileQuotaConfiguration {


  @Meta.AD(
      deflt = "5368709120",
      required = false
  )
  public String userGeneralByteQuota();

  @Meta.AD(
      deflt = "",
      required = false
  )
  public String instanceGeneralByteQuota();

  @Meta.AD(
      deflt = "21474836480",
      required = false
  )
  public String siteGeneralByteQuota();

  @Meta.AD(
      deflt = "",
      required = false
  )
  public String organizationGeneralByteQuota();

  @Meta.AD(
      deflt = "",
      required = false,
      description = "<id>:<numberOfBytes> per line"
  )
  public String[] userSpecificByteQuotas();

  @Meta.AD(
      deflt = "",
      required = false,
      description = "<id>:<numberOfBytes> per line"
  )
  public String[] instanceSpecificByteQuotas();

  @Meta.AD(
      deflt = "",
      required = false,
      description = "<id>:<numberOfBytes> per line"
  )
  public String[] siteSpecificByteQuotas();

  @Meta.AD(
      deflt = "",
      required = false,
      description = "<id>:<numberOfBytes> per line"
  )
  public String[] organizationSpecificByteQuotas();


}
