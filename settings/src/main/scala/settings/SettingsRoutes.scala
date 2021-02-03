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

  /*

  PUT /tenants/{tenantId}/settings/display
  - headers:
    - auth (for simplicity, assume something else validates/provides these)
      - tenantId
      - userId
    - IP
  - body:
    - name: String
    - description: Option[String]
  - create a UpdateDisplaySettings command
  - handle the command:
    - failure: 
      - error in http response
    - success: 
      - create/update the DisplaySettings using event
        - look up DisplaySettings in DB
          - not exists: create
          - exists: update
      - write event and DisplaySettings to db in tx
        - event to settings_events table
        - DisplaySettings to display_settings table

  GET /tenants/{tenantId}/settings/display
  - SELECT * FROM display_settings WHERE tenant_id = ? LIMIT 1
  - convert to DisplaySettings
  - return ^^ in http response, or 404 if not exists

  */

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