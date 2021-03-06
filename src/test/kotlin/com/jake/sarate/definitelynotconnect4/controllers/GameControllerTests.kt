package com.jake.sarate.definitelynotconnect4.controllers

import com.jake.sarate.definitelynotconnect4.models.constants.GameState
import com.jake.sarate.definitelynotconnect4.models.requests.GameRequest
import com.jake.sarate.definitelynotconnect4.models.requests.PostMoveRequest
import com.jake.sarate.definitelynotconnect4.models.responses.CreateGameResponse
import com.jake.sarate.definitelynotconnect4.models.responses.GetGameResponse
import com.jake.sarate.definitelynotconnect4.models.responses.GetGamesResponse
import com.jake.sarate.definitelynotconnect4.models.responses.PostMoveResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

import org.springframework.http.ResponseEntity




@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameControllerTests {

    @LocalServerPort
    var port: Int? = null

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun creatingAGameShouldReturn200() {
        val request = HttpEntity(GameRequest(listOf("player 1", "player 2"), 4, 4))
        val response = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , request, CreateGameResponse::class.java)
        val body = response.body
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(body)
    }

    @Test
    fun gettingAGameShouldReturn200() {
        val request = HttpEntity(GameRequest(listOf("player 1", "player 2"), 4, 4))
        val response = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , request, CreateGameResponse::class.java)
        val body = response.body
        assertEquals(HttpStatus.OK, response.statusCode)
        val getResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/${body?.gameId}", String::class.java)
        assertEquals(HttpStatus.OK, getResponse.statusCode)
    }

    @Test
    fun returnA404WhenAGameIsNotFound() {
        val getResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/fakeId", String::class.java)
        assertEquals(HttpStatus.NOT_FOUND, getResponse.statusCode)
    }

    @Test
    fun returnTheCurrentListOfGames() {
        val response = restTemplate.getForEntity("http://localhost:$port/drop_token", GetGamesResponse::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        assert(response.body?.games?.size!! > 0)
    }

    @Test
    fun postAValidMoveReturnsA200() {
        val createGameRequest = HttpEntity(GameRequest(listOf("player1", "player2"), 4, 4))
        val createGameResponse = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , createGameRequest, CreateGameResponse::class.java)
        assertEquals(HttpStatus.OK, createGameResponse.statusCode)
        createGameResponse.body?.let {
            val gameId = it.gameId
            val getGameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
            assertEquals(HttpStatus.OK, getGameResponse.statusCode)
            getGameResponse.body?.let { gameData ->
                val player1 = gameData.players[0]
                val postMoveRequest = HttpEntity(PostMoveRequest(0))
                val postMoveResponse = restTemplate.postForEntity("http://localhost:$port/drop_token/$gameId/$player1", postMoveRequest, PostMoveResponse::class.java)
                assertEquals(HttpStatus.OK, postMoveResponse.statusCode)
                postMoveResponse.body?. let { postMoveData ->
                    assertEquals("$gameId/moves/0", postMoveData.move)
                } ?: run {
                    throw RuntimeException("Test failed to return a response")
                }
            }
        }
    }

    @Test
    fun postAnInvalidMoveReturns400() {
        val createGameRequest = HttpEntity(GameRequest(listOf("player1", "player2"), 4, 4))
        val createGameResponse = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , createGameRequest, CreateGameResponse::class.java)
        assertEquals(HttpStatus.OK, createGameResponse.statusCode)
        createGameResponse.body?.let {
            val gameId = it.gameId
            val getGameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
            assertEquals(HttpStatus.OK, getGameResponse.statusCode)
            getGameResponse.body?.let { gameData ->
                val player1 = gameData.players[0]
                val postMoveRequest = HttpEntity(PostMoveRequest(10))
                val postMoveResponse = restTemplate.postForEntity("http://localhost:$port/drop_token/$gameId/$player1", postMoveRequest, String::class.java)
                assertEquals(HttpStatus.BAD_REQUEST, postMoveResponse.statusCode)
            }
        }
    }

    @Test
    fun postAMoveForAPlayerWhoIsNotPartOfTheGameReturns404() {
        val createGameRequest = HttpEntity(GameRequest(listOf("player1", "player2"), 4, 4))
        val createGameResponse = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , createGameRequest, CreateGameResponse::class.java)
        assertEquals(HttpStatus.OK, createGameResponse.statusCode)
        createGameResponse.body?.let {
            val gameId = it.gameId
            val getGameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
            assertEquals(HttpStatus.OK, getGameResponse.statusCode)
            getGameResponse.body?.let {
                val postMoveRequest = HttpEntity(PostMoveRequest(10))
                val postMoveResponse = restTemplate.postForEntity("http://localhost:$port/drop_token/$gameId/notReallyAPlayer", postMoveRequest, String::class.java)
                assertEquals(HttpStatus.NOT_FOUND, postMoveResponse.statusCode)
            }
        }
    }

    @Test
    fun postingAMoveForAGameThatDoesNotExist() {
        val postMoveRequest = HttpEntity(PostMoveRequest(10))
        val postMoveResponse = restTemplate.postForEntity("http://localhost:$port/drop_token/someFakeGame/notReallyAPlayer", postMoveRequest, String::class.java)
        assertEquals(HttpStatus.NOT_FOUND, postMoveResponse.statusCode)
    }

    @Test
    fun postingAMoveOutOfTurnReturnsA409() {
        val createGameRequest = HttpEntity(GameRequest(listOf("player1", "player2"), 4, 4))
        val createGameResponse = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , createGameRequest, CreateGameResponse::class.java)
        assertEquals(HttpStatus.OK, createGameResponse.statusCode)
        createGameResponse.body?.let {
            val gameId = it.gameId
            val getGameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
            assertEquals(HttpStatus.OK, getGameResponse.statusCode)
            getGameResponse.body?.let { gameData ->
                val player1 = gameData.players[0]
                val postMoveRequest = HttpEntity(PostMoveRequest(0))
                val postMoveResponse = restTemplate.postForEntity("http://localhost:$port/drop_token/$gameId/$player1", postMoveRequest, PostMoveResponse::class.java)
                assertEquals(HttpStatus.OK, postMoveResponse.statusCode)
                val secondPostMoveResponse = restTemplate.postForEntity("http://localhost:$port/drop_token/$gameId/$player1", postMoveRequest, String::class.java)
                assertEquals(HttpStatus.CONFLICT, secondPostMoveResponse.statusCode)
            }
        }
    }

    @Test
    fun getAListOfMovesForASpecificGameShouldReturnA200() {
        val createGameRequest = HttpEntity(GameRequest(listOf("player1", "player2"), 4, 4))
        val createGameResponse = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , createGameRequest, CreateGameResponse::class.java)
        assertEquals(HttpStatus.OK, createGameResponse.statusCode)
        createGameResponse.body?.let {
            val gameId = it.gameId
            val getGameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
            assertEquals(HttpStatus.OK, getGameResponse.statusCode)
            getGameResponse.body?.let { gameData ->
                val player1 = gameData.players[0]
                val player2 = gameData.players[1]
                val postMoveRequest = HttpEntity(PostMoveRequest(0))
                val postMoveResponse = restTemplate.postForEntity("http://localhost:$port/drop_token/$gameId/$player1", postMoveRequest, PostMoveResponse::class.java)
                assertEquals(HttpStatus.OK, postMoveResponse.statusCode)
                val secondPostMoveResponse = restTemplate.postForEntity("http://localhost:$port/drop_token/$gameId/$player2", postMoveRequest, String::class.java)
                assertEquals(HttpStatus.OK, secondPostMoveResponse.statusCode)
                val getMovesResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId/moves", String::class.java)
                assertEquals(HttpStatus.OK, getMovesResponse.statusCode)
            }
        }
    }

    @Test
    fun gettingAListOfMovesForAGameThatDoesNotExistShouldReturnA404() {
        val getMovesResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/notReal/moves", String::class.java)
        assertEquals(HttpStatus.NOT_FOUND, getMovesResponse.statusCode)
    }

    @Test
    fun gettingAListOfMovesWhenSpecifyingBadQueryParamsShouldReturnA400() {
        val createGameRequest = HttpEntity(GameRequest(listOf("player1", "player2"), 4, 4))
        val createGameResponse = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , createGameRequest, CreateGameResponse::class.java)
        assertEquals(HttpStatus.OK, createGameResponse.statusCode)
        createGameResponse.body?.let {
            val gameId = it.gameId
            val getMovesResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId/moves?start=a", String::class.java)
            assertEquals(HttpStatus.BAD_REQUEST, getMovesResponse.statusCode)
        }
    }

    @Test
    fun gettingAValidMoveForAGameThatExistsShouldReturn200() {
        val createGameRequest = HttpEntity(GameRequest(listOf("player1", "player2"), 4, 4))
        val createGameResponse = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , createGameRequest, CreateGameResponse::class.java)
        assertEquals(HttpStatus.OK, createGameResponse.statusCode)
        createGameResponse.body?.let {
            val gameId = it.gameId
            val getGameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
            assertEquals(HttpStatus.OK, getGameResponse.statusCode)
            getGameResponse.body?.let { gameData ->
                val player1 = gameData.players[0]
                val postMoveRequest = HttpEntity(PostMoveRequest(0))
                val postMoveResponse = restTemplate.postForEntity("http://localhost:$port/drop_token/$gameId/$player1", postMoveRequest, PostMoveResponse::class.java)
                assertEquals(HttpStatus.OK, postMoveResponse.statusCode)
                val getMoveResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/${postMoveResponse.body?.move}", String::class.java)
                assertEquals(HttpStatus.OK, getMoveResponse.statusCode)
            }
        }
    }

    @Test
    fun gettingAMoveThatDoesNotExistShouldReturnA404() {
        val createGameRequest = HttpEntity(GameRequest(listOf("player1", "player2"), 4, 4))
        val createGameResponse = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , createGameRequest, CreateGameResponse::class.java)
        assertEquals(HttpStatus.OK, createGameResponse.statusCode)
        createGameResponse.body?.let {
            val gameId = it.gameId
            val getGameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
            assertEquals(HttpStatus.OK, getGameResponse.statusCode)
            getGameResponse.body?.let { gameData ->
                val player1 = gameData.players[0]
                val postMoveRequest = HttpEntity(PostMoveRequest(0))
                val postMoveResponse = restTemplate.postForEntity("http://localhost:$port/drop_token/$gameId/$player1", postMoveRequest, PostMoveResponse::class.java)
                assertEquals(HttpStatus.OK, postMoveResponse.statusCode)
                val getMoveResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId/moves/1234", String::class.java)
                assertEquals(HttpStatus.NOT_FOUND, getMoveResponse.statusCode)
            }
        }
    }

    @Test
    fun deletingAPlayerFromTheGameShouldReturnA202() {
        val createGameRequest = HttpEntity(GameRequest(listOf("player1", "player2"), 4, 4))
        val createGameResponse = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , createGameRequest, CreateGameResponse::class.java)
        assertEquals(HttpStatus.OK, createGameResponse.statusCode)
        createGameResponse.body?.let {
            val gameId = it.gameId
            val getGameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
            assertEquals(HttpStatus.OK, getGameResponse.statusCode)
            getGameResponse.body?.let { gameData ->
                val player1 = gameData.players[0]
                val response: ResponseEntity<String> = restTemplate.exchange(
                    "http://localhost:$port/drop_token/$gameId/$player1",
                    HttpMethod.DELETE,
                    null,
                    String::class.java
                )
                assertEquals(HttpStatus.ACCEPTED, response.statusCode)
            }
        }
    }

    @Test
    fun deletingAPlayerThatDoesNotExistReturnsA404() {
        val createGameRequest = HttpEntity(GameRequest(listOf("player1", "player2"), 4, 4))
        val createGameResponse = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , createGameRequest, CreateGameResponse::class.java)
        assertEquals(HttpStatus.OK, createGameResponse.statusCode)
        createGameResponse.body?.let {
            val gameId = it.gameId
            val getGameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
            assertEquals(HttpStatus.OK, getGameResponse.statusCode)
            getGameResponse.body?.let {
                val result = restTemplate.exchange(
                    "http://localhost:$port/drop_token/$gameId/NotReal",
                    HttpMethod.DELETE,
                    null,
                    String::class.java
                )
                assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
            }
        }
    }

    @Test
    fun deletingAPlayerFromAGameThatIsAlreadyDONEReturnsA410() {
        val createGameRequest = HttpEntity(GameRequest(listOf("player1", "player2"), 4, 4))
        val createGameResponse = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , createGameRequest, CreateGameResponse::class.java)
        assertEquals(HttpStatus.OK, createGameResponse.statusCode)
        createGameResponse.body?.let {
            val gameId = it.gameId
            val getGameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
            assertEquals(HttpStatus.OK, getGameResponse.statusCode)
            getGameResponse.body?.let { gameData ->
                val result = restTemplate.exchange(
                    "http://localhost:$port/drop_token/$gameId/${gameData.players[0]}",
                    HttpMethod.DELETE,
                    null,
                    String::class.java
                )
                assertEquals(HttpStatus.ACCEPTED, result.statusCode)
                val secondResult = restTemplate.exchange(
                    "http://localhost:$port/drop_token/$gameId/${gameData.players[1]}",
                    HttpMethod.DELETE,
                    null,
                    String::class.java
                )
                assertEquals(HttpStatus.GONE, secondResult.statusCode)
            }
        }
    }

    @Test
    fun whenAPlayerFillsAColumnWithTheirTokensTheyWinTheGame() {
        val createGameRequest = HttpEntity(GameRequest(listOf("player1", "player2"), 4, 4))
        val createGameResponse = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , createGameRequest, CreateGameResponse::class.java)
        assertEquals(HttpStatus.OK, createGameResponse.statusCode)
        createGameResponse.body?.let {
            val gameId = it.gameId
            val getGameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
            assertEquals(HttpStatus.OK, getGameResponse.statusCode)
            getGameResponse.body?.let { gameData ->
                val player1 = gameData.players[0]
                val player2 = gameData.players[1]
                listOf(
                    player1 to PostMoveRequest(0),
                    player2 to PostMoveRequest(1),
                    player1 to PostMoveRequest(0),
                    player2 to PostMoveRequest(1),
                    player1 to PostMoveRequest(0),
                    player2 to PostMoveRequest(1),
                    player1 to PostMoveRequest(0),
                ).forEach { postMove ->
                    val postMoveRequest = HttpEntity(postMove.second)
                    val postMoveResponse = restTemplate.postForEntity("http://localhost:$port/drop_token/$gameId/${postMove.first}", postMoveRequest, PostMoveResponse::class.java)
                    assertEquals(HttpStatus.OK, postMoveResponse.statusCode)
                    println(postMoveResponse.body)
                }
                val gameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
                assertEquals(HttpStatus.OK, gameResponse.statusCode)
                assertEquals(player1, gameResponse!!.body!!.winner)
            }
        }
    }

    @Test
    fun whenAPlayerFillsARowWithTheirTokensTheyWinTheGame() {
        val createGameRequest = HttpEntity(GameRequest(listOf("player1", "player2"), 4, 4))
        val createGameResponse = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , createGameRequest, CreateGameResponse::class.java)
        assertEquals(HttpStatus.OK, createGameResponse.statusCode)
        createGameResponse.body?.let {
            val gameId = it.gameId
            val getGameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
            assertEquals(HttpStatus.OK, getGameResponse.statusCode)
            getGameResponse.body?.let { gameData ->
                val player1 = gameData.players[0]
                val player2 = gameData.players[1]
                listOf(
                    player1 to PostMoveRequest(0),
                    player2 to PostMoveRequest(0),
                    player1 to PostMoveRequest(1),
                    player2 to PostMoveRequest(1),
                    player1 to PostMoveRequest(2),
                    player2 to PostMoveRequest(2),
                    player1 to PostMoveRequest(3),
                ).forEach { postMove ->
                    val postMoveRequest = HttpEntity(postMove.second)
                    val postMoveResponse = restTemplate.postForEntity("http://localhost:$port/drop_token/$gameId/${postMove.first}", postMoveRequest, PostMoveResponse::class.java)
                    assertEquals(HttpStatus.OK, postMoveResponse.statusCode)
                    println(postMoveResponse.body)
                }
                val gameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
                assertEquals(HttpStatus.OK, gameResponse.statusCode)
                assertEquals(player1, gameResponse!!.body!!.winner)
            }
        }
    }

    @Test
    fun whenAPlayerFillsADiagonalRowWithTheirTokensTheyWinTheGame() {
        val createGameRequest = HttpEntity(GameRequest(listOf("player1", "player2"), 4, 4))
        val createGameResponse = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , createGameRequest, CreateGameResponse::class.java)
        assertEquals(HttpStatus.OK, createGameResponse.statusCode)
        createGameResponse.body?.let {
            val gameId = it.gameId
            val getGameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
            assertEquals(HttpStatus.OK, getGameResponse.statusCode)
            getGameResponse.body?.let { gameData ->
                val player1 = gameData.players[0]
                val player2 = gameData.players[1]
                listOf(
                    player1 to PostMoveRequest(0),
                    player2 to PostMoveRequest(0),
                    player1 to PostMoveRequest(1),
                    player2 to PostMoveRequest(1),
                    player1 to PostMoveRequest(2),
                    player2 to PostMoveRequest(2),
                    player1 to PostMoveRequest(3),
                ).forEach { postMove ->
                    val postMoveRequest = HttpEntity(postMove.second)
                    val postMoveResponse = restTemplate.postForEntity("http://localhost:$port/drop_token/$gameId/${postMove.first}", postMoveRequest, PostMoveResponse::class.java)
                    assertEquals(HttpStatus.OK, postMoveResponse.statusCode)
                    println(postMoveResponse.body)
                }
                val gameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
                assertEquals(HttpStatus.OK, gameResponse.statusCode)
                assertEquals(player1, gameResponse!!.body!!.winner)
            }
        }
    }

    @Test
    fun whenAllSlotsAreFilledNoOneWins() {
        val createGameRequest = HttpEntity(GameRequest(listOf("player1", "player2"), 4, 4))
        val createGameResponse = restTemplate.exchange("http://localhost:$port/drop_token", HttpMethod.POST , createGameRequest, CreateGameResponse::class.java)
        assertEquals(HttpStatus.OK, createGameResponse.statusCode)
        createGameResponse.body?.let {
            val gameId = it.gameId
            val getGameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
            assertEquals(HttpStatus.OK, getGameResponse.statusCode)
            getGameResponse.body?.let { gameData ->
                val player1 = gameData.players[0]
                val player2 = gameData.players[1]
                listOf(
                    player1 to PostMoveRequest(0),
                    player2 to PostMoveRequest(1),
                    player1 to PostMoveRequest(0),
                    player2 to PostMoveRequest(1),
                    player1 to PostMoveRequest(1),
                    player2 to PostMoveRequest(1),
                    player1 to PostMoveRequest(2),
                    player2 to PostMoveRequest(3),
                    player1 to PostMoveRequest(0),
                    player2 to PostMoveRequest(3),
                    player1 to PostMoveRequest(2),
                    player2 to PostMoveRequest(3),
                    player1 to PostMoveRequest(3),
                    player2 to PostMoveRequest(2),
                    player1 to PostMoveRequest(2),
                    player2 to PostMoveRequest(0),
                ).forEach { postMove ->
                    val postMoveRequest = HttpEntity(postMove.second)
                    val postMoveResponse = restTemplate.postForEntity("http://localhost:$port/drop_token/$gameId/${postMove.first}", postMoveRequest, PostMoveResponse::class.java)
                    assertEquals(HttpStatus.OK, postMoveResponse.statusCode)
                    println(postMoveResponse.body)
                }
                val gameResponse = restTemplate.getForEntity("http://localhost:$port/drop_token/$gameId", GetGameResponse::class.java)
                assertEquals(HttpStatus.OK, gameResponse.statusCode)
                assertEquals(null, gameResponse!!.body!!.winner)
                assertEquals(GameState.DONE, gameResponse.body!!.state)
            }
        }
    }

}