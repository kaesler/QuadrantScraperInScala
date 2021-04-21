import mill._, scalalib._

object foo extends SbtModule {
  def scalaVersion = "2.13.5"
  def ivyDeps = Agg(
    ivy"com.softwaremill.sttp.client3::core:3.3.0-RC1",
    ivy"co.fs2::fs2-core:3.0.0",
    ivy"com.softwaremill.sttp.client3::async-http-client-backend-cats:3.3.0-RC1",
    ivy"org.jsoup:jsoup:1.13.1"
  )
}
