package com.jake.sarate.definitelynotconnect4.components

import com.jake.sarate.definitelynotconnect4.models.*
import java.util.*

class BasicGame(gameSettings: GameSettings, gameBoardFactory: GameBoardFactory): Game {

    override var currentTurn: String = gameSettings.players[0]
        private set
    override val gameId: String = UUID.randomUUID().toString()
    override val players: List<String> = gameSettings.players
    override val columns: Int = gameSettings.columns
    override val rows: Int = gameSettings.rows

    override var winner: String? = null
        private set
    override var state: GameState = GameState.IN_PROGRESS
        private set
    private val moves: MutableList<PlayerMoveResult> = mutableListOf()
    private val gameBoard = gameBoardFactory.create(gameSettings)

    override fun quit(playerId: String) {
        moves.add(
            PlayerMoveResult(
                playerId,
                moveType = PlayerMoveType.QUIT,
                column = null,
                row = null,
                exception = null,
                status = MoveResultStatus.SUCCESSFUL
            ))
        state = GameState.DONE
    }

    override fun attemptPlayerMove(playerId: String, column: Int): Pair<Int, PlayerMoveResult> {
        if (playerId != currentTurn) {
            val moveResult = PlayerMoveResult(
                playerId = playerId,
                moveType = PlayerMoveType.MOVE,
                status = MoveResultStatus.UNSUCCESSFUL,
                column = column,
                row = null,
                exception = PlayerMoveException.PLAYED_MOVE_OUT_OF_TURN
            )
            moves.add(moveResult)
            return Pair(moves.lastIndex, moveResult)
        }
        try {
            val result = gameBoard.dropToken(column, playerId)
            val moveResult = PlayerMoveResult(
                playerId = result.playerId,
                moveType = PlayerMoveType.MOVE,
                status = MoveResultStatus.SUCCESSFUL,
                column = result.column,
                row = result.row,
                exception = null,
            )
            moves.add(moveResult)
            currentTurn = players.filterNot { it == playerId }[0]
            return Pair(moves.lastIndex, moveResult)
        } catch (e: InvalidColumnSpecificationException) {
            val moveResult = PlayerMoveResult(
                playerId = playerId,
                moveType = PlayerMoveType.MOVE,
                status = MoveResultStatus.UNSUCCESSFUL,
                column = column,
                row = null,
                exception = PlayerMoveException.INVALID_COLUMN_SPECIFICATION
            )
            moves.add(moveResult)
            return Pair(moves.lastIndex, moveResult)
        } catch (e: NoAvailableSpacesException) {
            val moveResult = PlayerMoveResult(
                playerId = playerId,
                moveType = PlayerMoveType.MOVE,
                status = MoveResultStatus.UNSUCCESSFUL,
                column = column,
                row = null,
                exception = PlayerMoveException.NO_AVAILABLE_SPACES
            )
            moves.add(moveResult)
            return Pair(moves.lastIndex, moveResult)
        } catch (e: Exception) {
            println("${e.message}\n${e.stackTrace}")

            val moveResult = PlayerMoveResult(
                playerId = playerId,
                moveType = PlayerMoveType.MOVE,
                status = MoveResultStatus.UNSUCCESSFUL,
                column = column,
                row = null,
                exception = PlayerMoveException.UNKNOWN_PLAYER_MOVE_EXCEPTION
            )
            moves.add(moveResult)
            return Pair(moves.lastIndex, moveResult)
        }
    }

}