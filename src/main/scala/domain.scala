package settings

import java.util.UUID
import java.time.Instant

sealed trait SettingsCommand

sealed trait SettingsEvent

case class UpdateDisplaySettings(
  userId: UUID,
  ip: String,
  tenantId: UUID,
  name: String,
  description: Option[String],
) extends SettingsCommand

case class DisplaySettingsUpdated(
  eventId: UUID,
  timestamp: Instant,
  userId: UUID,
  ip: String,
  tenantId: UUID,
  name: String,
  description: Option[String],
) extends SettingsEvent

object DomainLogic {

  def handle(command: UpdateDisplaySettings): Either[String, DisplaySettingsUpdated] = 
    if (command.name.isEmpty)
      Left("name must not be empty")
    else if (!command.description.exists(_.nonEmpty))
      Left("description must not be empty, if specified")
    else
      //TODO effects for uuid & now
      Right(DisplaySettingsUpdated(
        UUID.randomUUID(),
        Instant.now(),
        command.userId,
        command.ip,
        command.tenantId,
        command.name,
        command.description,
      ))
}

case class DisplaySettings(
  tenantId: UUID,
  name: String,
  description: String,
)

object DisplaySettings {

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

  // def create(events: NonEmptyList[SettingsEvent]): Option[DisplaySettings] = ???
}

// case class CreateListItem()

// case class ListItemCreated()

// case class RenameListItem()

// case class ListItemRenamed()

// case class DeleteListItem()

// case class ListItemDeleted()

// case class ListItem(
//   tenantId: UUID,
//   itemId: UUID,
//   order: Int,
//   name: String,
// )
