import com.google.api.ads.admanager.axis.factory.AdManagerServices
import com.google.api.ads.admanager.axis.utils.v202308.ReportDownloader
import com.google.api.ads.admanager.axis.v202308.{Column, Date, DateRangeType, Dimension, ExportFormat, ReportDownloadOptions, ReportJob, ReportQuery, ReportServiceInterface, TimeZoneType, UserServiceInterface}
import com.google.api.ads.admanager.lib.client.AdManagerSession
import com.google.api.ads.common.lib.auth.OfflineCredentials
import com.google.api.ads.common.lib.auth.OfflineCredentials.Api
import com.google.api.client.auth.oauth2.Credential

import java.io.File
import java.nio.file.Files
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import scala.sys.process._

object Main extends App {
  private val serviceAccountJsonKey = sys.env("SERVICE_ACCOUNT_JSON_KEY")

  private val client = new GoogleAdManagerClient("ads.properties", serviceAccountJsonKey)

  client.getReport(createReportQuery(new Date(2023, 11, 10), new Date(2023, 11, 10)), createReportDownloadOptions())
  client.getCurrentUser()

  private def createReportQuery(startDate: Date, endDate: Date): ReportQuery = {
    val reportQuery = new ReportQuery()
    reportQuery.setTimeZoneType(TimeZoneType.PUBLISHER)
    reportQuery.setDateRangeType(DateRangeType.CUSTOM_DATE)
    reportQuery.setStartDate(startDate)
    reportQuery.setEndDate(endDate)
    reportQuery.setReportCurrency("JPY")

    reportQuery.setDimensions(Array[Dimension](
      Dimension.DATE,
      Dimension.HOUR,
      Dimension.AD_UNIT_NAME,
    ))
    reportQuery.setColumns(Array[Column](
      Column.AD_EXCHANGE_LINE_ITEM_LEVEL_IMPRESSIONS,
      Column.AD_EXCHANGE_LINE_ITEM_LEVEL_CLICKS,
      Column.AD_EXCHANGE_LINE_ITEM_LEVEL_REVENUE,
      Column.AD_EXCHANGE_TOTAL_REQUESTS,
      Column.AD_EXCHANGE_ACTIVE_VIEW_MEASURABLE_IMPRESSIONS
    ))
    reportQuery
  }

  private def createReportDownloadOptions(): ReportDownloadOptions = {
    val options = new ReportDownloadOptions()
    options.setExportFormat(ExportFormat.CSV_DUMP)
    options.setUseGzipCompression(false)
    options
  }
}

class GoogleAdManagerClient(adsProperties: String, serviceAccountJsonKey: String) {
  // OfflineCredentials.Builder#withJsonKeyFilePathではファイルパスを指定する必要があるため、
  //   一時ファイルを作成し引数で渡されたserviceAccountJsonKey(JSON文字列)の内容を書き込み、そのファイルパスを指定する
  private val tmpFilePath = Files.createTempFile("service-account-json-key-", ".json")
  tmpFilePath.toFile.deleteOnExit()
  private val serviceAccountJsonKeyFilePath = Files.write(tmpFilePath, serviceAccountJsonKey.getBytes)

  private val oAuth2Credential: Credential = new OfflineCredentials.Builder()
    .forApi(Api.AD_MANAGER)
    .fromFile(adsProperties)
    .withJsonKeyFilePath(serviceAccountJsonKeyFilePath.toString)
    .build()
    .generateCredential()
  private val session: AdManagerSession = new AdManagerSession.Builder()
    .fromFile(adsProperties)
    .withOAuth2Credential(oAuth2Credential)
    .build()
  private val adManagerServices: AdManagerServices = new AdManagerServices()

  def getReport(reportQuery: ReportQuery, reportDownloadOptions: ReportDownloadOptions): Either[Exception, String] = {
    val reportService = adManagerServices.get(session, classOf[ReportServiceInterface])
    val reportJob = new ReportJob()
    reportJob.setReportQuery(reportQuery)
    val executedReportJob = reportService.runReportJob(reportJob)
    val reportDownloader = new ReportDownloader(reportService, executedReportJob.getId)

    Try(reportDownloader.waitForReportReady) match {
      case Success(isSuccess) =>
        if (!isSuccess) return Left(new Exception("error"))
        val url = reportDownloader.getDownloadUrl(reportDownloadOptions)
        Right(url #> new File("report-via-api.csv") !!)
      case Failure(exception) => Left(new Exception(exception))
    }
  }

  def getCurrentUser(): Unit = {
    val userService = adManagerServices.get(session, classOf[UserServiceInterface])
    val user = userService.getCurrentUser()
    println(s"${user} is the current user.")
  }
}
