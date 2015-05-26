package actors

import lib.{Pager, ServiceDiff, Text}
import com.gilt.apidoc.api.v0.models.{Application, Diff, DiffBreaking, DiffNonBreaking, DiffUndefinedType, Publication, Version}
import com.gilt.apidoc.internal.v0.models.{Task, TaskDataDiffVersion, TaskDataIndexApplication, TaskDataUndefinedType}
import db.{ApplicationsDao, Authorization, ChangesDao, OrganizationsDao, TasksDao, UsersDao, VersionsDao}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID
import scala.util.{Failure, Success, Try}

object TaskActor {

  object Messages {
    case class TaskCreated(guid: UUID)
    case object RestartDroppedTasks
    case object PurgeOldTasks
    case object NotifyFailed
  }

}

class TaskActor extends Actor {

  private val NumberDaysBeforePurge = 90

  def receive = {

    case TaskActor.Messages.TaskCreated(guid) => Util.withVerboseErrorHandler(
      s"TaskActor.Messages.TaskCreated($guid)", {
        TasksDao.findByGuid(guid).map { task =>
          TasksDao.incrementNumberAttempts(UsersDao.AdminUser, task)

          task.data match {
            case TaskDataDiffVersion(oldVersionGuid, newVersionGuid) => {
              processTask(task, Try(diffVersion(oldVersionGuid, newVersionGuid)))
            }

            case TaskDataIndexApplication(applicationGuid) => {
              processTask(task, Try(Search.indexApplication(applicationGuid)))
            }

            case TaskDataUndefinedType(desc) => {
              TasksDao.recordError(UsersDao.AdminUser, task, "Task actor got an undefined data type: " + desc)
            }
          }
        }
      }
    )

    case TaskActor.Messages.RestartDroppedTasks => Util.withVerboseErrorHandler(
      "TaskActor.Messages.RestartDroppedTasks", {
        TasksDao.findAll(
          nOrFewerAttempts = Some(2),
          nOrMoreMinutesOld = Some(1)
        ).foreach { task =>
          global.Actors.mainActor ! actors.MainActor.Messages.TaskCreated(task.guid)
        }
      }
    )

    case TaskActor.Messages.NotifyFailed => Util.withVerboseErrorHandler(
      "TaskActor.Messages.NotifyFailed", {
        val errors = TasksDao.findAll(
          nOrMoreAttempts = Some(2)
        ).map { task =>
          val errorType = task.data match {
            case TaskDataDiffVersion(a, b) => s"TaskDataDiffVersion($a, $b)"
            case TaskDataIndexApplication(guid) => s"TaskDataIndexApplication($guid)"
            case TaskDataUndefinedType(desc) => s"TaskDataUndefinedType($desc)"
          }

          val errorMsg = Text.truncate(task.lastError.getOrElse("No information on error"))
          s"$errorType task ${task.guid}: $errorMsg"
        }
        Emails.sendErrors(
          subject = "One or more tasks failed",
          errors = errors
        )
      }
    )

    case TaskActor.Messages.PurgeOldTasks => Util.withVerboseErrorHandler(
      "TaskActor.Messages.PurgeOldTasks", {
        TasksDao.findAll(
          isDeleted = Some(true),
          deletedAtLeastNDaysAgo = Some(NumberDaysBeforePurge)
        ).foreach { task =>
          TasksDao.purge(UsersDao.AdminUser, task)
        }
      }
    )

    case m: Any => {
      Logger.error("Task actor got an unhandled message: " + m)
    }

  }

  private def diffVersion(oldVersionGuid: UUID, newVersionGuid: UUID) {
    VersionsDao.findByGuid(Authorization.All, oldVersionGuid, isDeleted = None).map { oldVersion =>
      VersionsDao.findByGuid(Authorization.All, newVersionGuid, isDeleted = None).map { newVersion =>
        ServiceDiff(oldVersion.service, newVersion.service).differences match {
          case Nil => {
            // No-op
          }
          case diffs => {
            ChangesDao.upsert(
              createdBy = UsersDao.AdminUser,
              fromVersion = oldVersion,
              toVersion = newVersion,
              differences = diffs
            )
            versionUpdated(oldVersion, newVersion, diffs)
          }
        }
      }
    }
  }

  private def versionUpdated(
    oldVersion: Version,
    newVersion: Version,
    diff: Seq[Diff]
  ) {
    ApplicationsDao.findAll(Authorization.All, version = Some(newVersion), limit = 1).headOption.map { application =>
      OrganizationsDao.findAll(Authorization.All, application = Some(application), limit = 1).headOption.map { org =>
        Emails.deliver(
          org = org,
          publication = Publication.VersionsCreate,
          subject = s"${org.name}/${application.name}: New Version Uploaded (${newVersion.version}) ",
          body = views.html.emails.versionCreated(
            org,
            application,
            newVersion,
            oldVersion = Some(oldVersion),
            breakingDiffs = diff.filter { d =>
              d match {
                case DiffBreaking(desc) => true
                case DiffNonBreaking(desc) => false
                case DiffUndefinedType(desc) => false
              }
            }.map(_.asInstanceOf[DiffBreaking]),
            nonDiffBreakings = diff.filter { d =>
              d match {
                case DiffBreaking(desc) => false
                case DiffNonBreaking(desc) => true
                case DiffUndefinedType(desc) => false
              }
            }.map(_.asInstanceOf[DiffNonBreaking])
          ).toString
        )
      }
    }
  }

  def processTask[T](task: Task, attempt: Try[T]) {
    attempt match {
      case Success(_) => {
        TasksDao.softDelete(UsersDao.AdminUser, task)
      }
      case Failure(ex) => {
        TasksDao.recordError(UsersDao.AdminUser, task, ex)
      }
    }
  }

}
