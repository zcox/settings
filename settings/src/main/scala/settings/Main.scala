package settings

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]) =
    SettingsServer.stream[IO].compile.drain.as(ExitCode.Success)
}
