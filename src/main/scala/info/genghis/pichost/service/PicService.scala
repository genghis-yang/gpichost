package info.genghis.pichost.service

import cats.effect.Sync
import cats.implicits._
import fs2.Chunk
import info.genghis.pichost.config.AppConfigLoader
import info.genghis.pichost.error.GithubRequestError
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.headers.{`Content-Type`, Authorization}

import java.util.Base64

trait PicService[M[_]] {
  def getPic(repo: String, identity: String): M[Chunk[Byte]]
}

object PicService {
  val rawType = "application/vnd.github.VERSION.raw"

  def apply[M[_]: Sync: AppConfigLoader](httpClient: Client[M]): PicService[M] = new PicService[M] {
    override def getPic(repo: String, identity: String): M[Chunk[Byte]] = for {
      appConfig <- implicitly[AppConfigLoader[M]].load
      uri <- Uri
        .fromString(s"https://api.github.com/repos/${appConfig.username}/$repo/contents/pictures/$identity")
        .fold(err => Sync[M].raiseError(GithubRequestError(err.message)), uri => Sync[M].delay(uri))
      response <- httpClient.expect[GithubResponse](
        Request[M]()
          .withUri(uri)
          .withHeaders(
            Header(Authorization.name.value, "token " + appConfig.accessToken),
            Header(`Content-Type`.name.value, rawType)
          )
      )
      decodedContent <- Sync[M].delay(Base64.getDecoder.decode(response.content.replace("\n", "")))
    } yield Chunk.bytes(decodedContent)
  }
}
