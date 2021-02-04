package settings

import cats.effect.Sync
import cats.implicits._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import io.circe.Decoder
import io.circe.generic.semiauto._
import org.http4s.EntityDecoder
import org.http4s.circe._

object SettingsRoutes {

  case class UpdateDisplaySettingsRequest(
    name: String,
    description: Option[String],
  )
  object UpdateDisplaySettingsRequest {
    implicit val decoder: Decoder[UpdateDisplaySettingsRequest] = deriveDecoder[UpdateDisplaySettingsRequest]
    implicit def entityDecoder[F[_]: Sync]: EntityDecoder[F, UpdateDisplaySettingsRequest] = jsonOf
  }

  def settingsRoutes[F[_]: Sync](
    displaySettingsRepository: DisplaySettingsRepository[F],
    domainLogic: DomainLogic[F],
  ): AuthedRoutes[User, F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    AuthedRoutes.of[User, F] {

      //TODO is authenticated user authorized to get/put for tenant?

      case GET -> Root / "tenants" / UUIDVar(tenantId) / "settings" / "display" as _ => 
        for {
          displaySettings <- displaySettingsRepository.getDisplaySettings(tenantId)
          resp <- displaySettings.fold(NotFound())(s => Ok(s))
        } yield resp

      case request @ PUT -> Root / "tenants" / UUIDVar(tenantId) / "settings" / "display" as user => 
        for {
          ip <- request.req.from.map(_.toString).getOrElse("Unknown").pure[F]
          r <- request.req.as[UpdateDisplaySettingsRequest]
          command = UpdateDisplaySettings(user.userId, ip, tenantId, r.name, r.description)
          resp <- domainLogic.process(command).foldF(_ => BadRequest(), _ => Ok())
        } yield resp

    }
  }
}