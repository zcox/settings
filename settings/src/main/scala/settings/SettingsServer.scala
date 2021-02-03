package settings

import cats.effect.{ConcurrentEffect, Timer, Blocker, ContextShift}
import fs2.Stream
import org.http4s.server._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import scala.concurrent.ExecutionContext.global
import doobie._
import doobie.util.ExecutionContexts

object SettingsServer {

  def stream[F[_]: ConcurrentEffect: ContextShift](implicit T: Timer[F]): Stream[F, Nothing] = {

    val xa = Transactor.fromDriverManager[F](
      "org.postgresql.Driver",
      "jdbc:postgresql:postgres",
      "postgres",
      "postgres",
      Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
    )
    val displaySettingsRepository = DisplaySettingsRepository.db
    val displaySettingsRepositoryTx = DisplaySettingsRepository.dbTx[F](xa)
    val eventRepository = EventRepository.db
    val writeService = WriteService.impl[F](displaySettingsRepository, eventRepository, xa)
    val domainLogic = DomainLogic.impl[F](displaySettingsRepositoryTx, writeService)
    val authMiddleware = AuthMiddleware(DumbAuth.impl[F])
    val routes = SettingsRoutes.settingsRoutes[F](displaySettingsRepositoryTx, domainLogic)
    val httpApp = authMiddleware(routes).orNotFound
    val finalHttpApp = Logger.httpApp(true, true)(httpApp)

    for {
      exitCode <- BlazeServerBuilder[F](global)
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
  }.drain
}
