package controllers

import io.apibuilder.api.v0.models.json._
import util.Conversions
import db.InternalUsersDao
import db.generated.SessionsDao
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc._

@Singleton
class Authentications @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  sessionsDao: SessionsDao,
  usersDao: InternalUsersDao,
  conversions: Conversions
) extends ApiBuilderController {

  def getSessionById(sessionId: String): Action[AnyContent] = Anonymous { _ =>
    sessionsDao.findById(sessionId) match {
      case None => NotFound
      case Some(session) => {
        if (session.deletedAt.isDefined) {
          NotFound
        } else {
          usersDao.findByGuid(session.userGuid) match {
            case None => NotFound
            case Some(user) => {
              Ok(Json.toJson(conversions.toAuthentication(session, user)))
            }
          }
        }
      }
    }
  }

}
