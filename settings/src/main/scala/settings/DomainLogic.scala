package settings

import cats.implicits._
import cats.data.EitherT
import cats.effect.{Sync, Clock}
import java.util.UUID
import java.time.Instant

sealed trait SettingsCommand

sealed trait SettingsEvent {
  def eventId: UUID
  def timestamp: Instant
}

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

/*
- process command
  - two possible outcomes:
    1. failure
      - something prevents command from succeeding
      - could be invalid data in command
      - could be that command is invalid according to existing state 
      - describe failure
      - return to client
      - do not change any state
    2. success
      - state changes due to command are recorded in events
      - update database in single transaction:
        - insert events into events table
        - update any other view tables due to events
          - these view changes should *only* be calculated from:
            - current view tables
            - state change events (due to commands)
*/

trait DomainLogic[F[_]] {
  def process(command: UpdateDisplaySettings): EitherT[F, String, DisplaySettingsUpdated]
}

object DomainLogic {

  //TODO not sure where to put this...
  def validate[F[_]: Sync: Clock](command: UpdateDisplaySettings): EitherT[F, String, DisplaySettingsUpdated] = 
    if (command.name.isEmpty)
      EitherT.leftT("name must not be empty")
    else if (!command.description.exists(_.nonEmpty))
      EitherT.leftT("description must not be empty, if specified")
    else
      EitherT.right(
        for {
          eventId <- randomUuid[F]
          timestamp <- now[F]
        } yield 
          DisplaySettingsUpdated(
            eventId,
            timestamp,
            command.userId,
            command.ip,
            command.tenantId,
            command.name,
            command.description,
          )
      )

  def impl[F[_]: Sync: Clock](
    displaySettingsRepository: DisplaySettingsRepository[F],
    writeService: WriteService[F],
  ): DomainLogic[F] = 
    new DomainLogic[F] {

      override def process(command: UpdateDisplaySettings): EitherT[F, String, DisplaySettingsUpdated] = 
        for {
          event <- DomainLogic.validate[F](command)
          oldView <- EitherT.right[String](displaySettingsRepository.getDisplaySettings(command.tenantId))
          newView = DisplaySettings.createOrUpdate(oldView, event)
          () <- EitherT.right[String](writeService.write(event, newView))
        } yield event
    }
}
