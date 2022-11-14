/**
 * Generated by API Builder - https://www.apibuilder.io
 * Service version: 0.15.77
 * apibuilder 0.15.33 localhost 9000/apicollective/apibuilder-internal/latest/anorm_2_8_parsers
 */
import anorm._

package io.apibuilder.internal.v0.anorm.parsers {

  import io.apibuilder.internal.v0.anorm.conversions.Standard._

  import io.apibuilder.internal.v0.anorm.conversions.Types._

  object Task {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.apibuilder.internal.v0.models.Task] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      guid: String = "guid",
      dataPrefix: String = "data",
      numberAttempts: String = "number_attempts",
      lastError: String = "last_error",
      prefixOpt: Option[String] = None
    ): RowParser[io.apibuilder.internal.v0.models.Task] = {
      SqlParser.get[_root_.java.util.UUID](prefixOpt.getOrElse("") + guid) ~
      io.apibuilder.internal.v0.anorm.parsers.TaskData.parserWithPrefix(prefixOpt.getOrElse("") + dataPrefix) ~
      SqlParser.long(prefixOpt.getOrElse("") + numberAttempts) ~
      SqlParser.str(prefixOpt.getOrElse("") + lastError).? map {
        case guid ~ data ~ numberAttempts ~ lastError => {
          io.apibuilder.internal.v0.models.Task(
            guid = guid,
            data = data,
            numberAttempts = numberAttempts,
            lastError = lastError
          )
        }
      }
    }

  }

  object TaskDataDiffVersion {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.apibuilder.internal.v0.models.TaskDataDiffVersion] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      oldVersionGuid: String = "old_version_guid",
      newVersionGuid: String = "new_version_guid",
      prefixOpt: Option[String] = None
    ): RowParser[io.apibuilder.internal.v0.models.TaskDataDiffVersion] = {
      SqlParser.get[_root_.java.util.UUID](prefixOpt.getOrElse("") + oldVersionGuid) ~
      SqlParser.get[_root_.java.util.UUID](prefixOpt.getOrElse("") + newVersionGuid) map {
        case oldVersionGuid ~ newVersionGuid => {
          io.apibuilder.internal.v0.models.TaskDataDiffVersion(
            oldVersionGuid = oldVersionGuid,
            newVersionGuid = newVersionGuid
          )
        }
      }
    }

  }

  object TaskDataIndexApplication {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.apibuilder.internal.v0.models.TaskDataIndexApplication] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      applicationGuid: String = "application_guid",
      prefixOpt: Option[String] = None
    ): RowParser[io.apibuilder.internal.v0.models.TaskDataIndexApplication] = {
      SqlParser.get[_root_.java.util.UUID](prefixOpt.getOrElse("") + applicationGuid) map {
        case applicationGuid => {
          io.apibuilder.internal.v0.models.TaskDataIndexApplication(
            applicationGuid = applicationGuid
          )
        }
      }
    }

  }

  object TaskData {

    def parserWithPrefix(prefix: String, sep: String = "_") = {
      io.apibuilder.internal.v0.anorm.parsers.TaskDataIndexApplication.parser(prefixOpt = Some(s"$prefix$sep")) |
      io.apibuilder.internal.v0.anorm.parsers.TaskDataDiffVersion.parser(prefixOpt = Some(s"$prefix$sep"))
    }

    def parser() = {
      io.apibuilder.internal.v0.anorm.parsers.TaskDataIndexApplication.parser() |
      io.apibuilder.internal.v0.anorm.parsers.TaskDataDiffVersion.parser()
    }

  }

}