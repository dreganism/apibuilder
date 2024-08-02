package controllers

import db.SubscriptionsDao
import io.apibuilder.api.v0.models.json._
import io.apibuilder.api.v0.models.{Publication, SubscriptionForm}
import lib.Validation
import models.SubscriptionModel
import play.api.libs.json._
import play.api.mvc._

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class Subscriptions @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  subscriptionsDao: SubscriptionsDao,
  model: SubscriptionModel,
) extends ApiBuilderController {

  def get(
    guid: Option[UUID],
    organizationKey: Option[String],
    userGuid: Option[java.util.UUID],
    publication: Option[Publication],
    limit: Long = 25,
    offset: Long = 0
  ): Action[AnyContent] = Identified { request =>
    val subscriptions = subscriptionsDao.findAll(
      request.authorization,
      guid = guid,
      organizationKey = organizationKey,
      userGuid = userGuid,
      publication = publication,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(model.toModels(subscriptions)))
  }

  def getByGuid(guid: UUID): Action[AnyContent] = Identified { request =>
    subscriptionsDao.findByGuid(request.authorization, guid).flatMap(model.toModel) match {
      case None => NotFound
      case Some(subscription) => Ok(Json.toJson(subscription))
    }
  }

  def post(): Action[JsValue] = Identified(parse.json) { request =>
    request.body.validate[SubscriptionForm] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[SubscriptionForm] => {
        val form = s.get
        subscriptionsDao.validate(request.user, form) match {
          case Nil => {
            val subscription = subscriptionsDao.create(request.user, form)
            Created(Json.toJson(
              model.toModel(subscription).getOrElse {
                sys.error("Failed to create subscription")
              }
            ))
          }
          case errors => {
            Conflict(Json.toJson(errors))
          }
        }
      }
    }
  }

  def deleteByGuid(guid: UUID): Action[AnyContent] = Identified { request =>
    subscriptionsDao.findByGuid(request.authorization, guid) match {
      case None => NotFound
      case Some(subscription) => {
        subscriptionsDao.softDelete(request.user.reference, subscription)
        NoContent
      }
    }
  }

}
