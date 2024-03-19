import sttp.client4._
import sttp.client4.httpurlconnection.HttpURLConnectionBackend
import sttp.model.HeaderNames

object useAPI {
  val clientId = "86a45dc7aa18450d8990eb1d4a209cc2"
  val clientSecret = "f345e40d40d649cb892951706af4b11d"
  val playlistId = "5Rrf7mqN8uus2AaQQQNdc1"

  implicit val backend = HttpURLConnectionBackend()

  def fetchAccessToken(): String = {
    val authHeader = "Basic " + java.util.Base64.getEncoder.encodeToString((clientId + ":" + clientSecret).getBytes("UTF-8"))
    val request = basicRequest
      .header(HeaderNames.Authorization, authHeader)
      .header(HeaderNames.ContentType, "application/x-www-form-urlencoded")
      .post(uri"https://accounts.spotify.com/api/token")
      .body("grant_type=client_credentials")

    val response = request.send(backend)
    response.body match {
      case Right(body) =>
        val json = ujson.read(body)
        json("access_token").str
      case Left(error) => throw new Exception(s"Failed to fetch Access Token: $error")
    }
  }

  def getPlaylistTracks(accessToken: String): List[(String, Long, List[String])] = {
    var tracks = List.empty[(String, Long, List[String])]
    var offset = 0
    var total = 0
    do {
      val request = basicRequest
        .header("Authorization", s"Bearer $accessToken")
        .get(uri"https://api.spotify.com/v1/playlists/$playlistId/tracks?offset=$offset&limit=100")
      val response = request.send(backend)
      response.body match {
        case Right(body) =>
          val json = ujson.read(body)
          total = json.obj("total").num.toInt
          val items = json.obj("items").arr.toList
          val pageTracks = items.map { item =>
            val track = item.obj("track")
            val name = track.obj("name").str
            val durationMs = track.obj("duration_ms").num.toLong
            val artistIds = track.obj("artists").arr.toList.map(_.obj("id").str)
            (name, durationMs, artistIds)
          }
          tracks = tracks ++ pageTracks
          offset += items.length
        case Left(errorMessage) =>
          println(s"Error fetching playlist tracks: $errorMessage")
          return List.empty
      }
    } while (offset < total)
    tracks.sortBy(-_._2).take(10)
  }


  def getArtistDetails(accessToken: String, artistId: String): (String, Long) = {
    val request = basicRequest
      .header("Authorization", s"Bearer $accessToken")
      .get(uri"https://api.spotify.com/v1/artists/$artistId")

    val response = request.send(backend)
    response.body match {
      case Right(body) =>
        val artistJson = ujson.read(body)
        val name = artistJson.obj("name").str
        val followers = artistJson.obj("followers").obj("total").num.toLong
        (name, followers)

      case Left(errorMessage) =>
        println(s"Error fetching artist details: $errorMessage")
        ("", 0L)
    }
  }

  def main(args: Array[String]): Unit = {
    println("Part 1")
    val accessToken = fetchAccessToken()
    val tracks = getPlaylistTracks(accessToken)
    tracks.foreach { case (name, duration, _) =>
      println(s"$name, $duration")
    }
    println("---------------------")
    println("Part 2")
    val artistDetails = tracks.flatMap(_._3).distinct.map(artistId => getArtistDetails(accessToken, artistId))
    artistDetails.sortBy(-_._2).foreach { case (name, followers) =>
      println(s"$name: $followers")
    }
  }
}