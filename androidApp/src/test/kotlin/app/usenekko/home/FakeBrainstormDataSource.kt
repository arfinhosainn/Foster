package app.usenekko.home

import app.usenekko.home.domain.BrainstormDataSource
import app.usenekko.home.domain.BrainstormError
import app.usenekko.home.domain.BrainstormGeneration
import app.usenekko.home.domain.BrainstormSession
import app.usenekko.shared.domain.Result

class FakeBrainstormDataSource : BrainstormDataSource {

    var generateResult: Result<BrainstormGeneration, BrainstormError> =
        Result.Error(BrainstormError.Unknown("not set"))
    var history: List<BrainstormSession> = emptyList()
    var historyError: BrainstormError? = null

    val generateCalls = mutableListOf<String>()

    override suspend fun generate(contactId: String): Result<BrainstormGeneration, BrainstormError> {
        generateCalls += contactId
        return generateResult
    }

    override suspend fun getHistory(contactId: String): Result<List<BrainstormSession>, BrainstormError> {
        return historyError?.let { Result.Error(it) } ?: Result.Success(history)
    }

    var monthlyGenerationCount: Int = 0

    override suspend fun getMonthlyGenerationCount(): Result<Int, BrainstormError> =
        Result.Success(monthlyGenerationCount)
}
