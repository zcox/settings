package settings

import cats._
import java.util.UUID
import io.circe.Encoder
import io.circe.generic.semiauto._
import org.http4s.EntityEncoder
import org.http4s.circe._

case class DisplaySettings(
  tenantId: UUID,
  name: String,
  description: String,
)

object DisplaySettings {
  implicit val encoder: Encoder[DisplaySettings] = deriveEncoder[DisplaySettings]
  implicit def entityEncoder[F[_]: Applicative]: EntityEncoder[F, DisplaySettings] = jsonEncoderOf

  object Defaults {
    val Description = ""
  }

  def create(event: DisplaySettingsUpdated): DisplaySettings = 
    DisplaySettings(
      event.tenantId,
      event.name,
      event.description.getOrElse(Defaults.Description)
    )

  def create(event: SettingsEvent): Option[DisplaySettings] = 
    event match {
      case e: DisplaySettingsUpdated => 
        Some(create(e))
    }

  def update(settings: DisplaySettings, event: DisplaySettingsUpdated): DisplaySettings = 
    settings.copy(
      name = event.name,
      description = event.description.getOrElse(Defaults.Description),
    )

  def update(settings: DisplaySettings, event: SettingsEvent): DisplaySettings = 
    event match {
      case e: DisplaySettingsUpdated => 
        update(settings, e)
    }

  def createOrUpdate(oldView: Option[DisplaySettings], event: DisplaySettingsUpdated): DisplaySettings = 
    oldView.fold(DisplaySettings.create(event))(v => DisplaySettings.update(v, event))
}
