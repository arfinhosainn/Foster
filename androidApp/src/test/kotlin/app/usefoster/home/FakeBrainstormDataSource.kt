package app.usefoster.home

import app.usefoster.home.domain.BrainstormDataSource
import app.usefoster.home.domain.BrainstormError
import app.usefoster.home.domain.BrainstormGeneration
import app.usefoster.home.domain.BrainstormSession
import app.usefoster.shared.domain.Result
import kotlinx.coroutines.CompletableDeferred

class FakeBrainstormDataSource : BrainstormDataSource {

    var generateResult: Result<BrainstormGeneration, BrainstormError> =
        Result.Error(BrainstormError.Unknown("not set"))
    var history: List<BrainstormSession> = emptyList()
    var historyError: BrainstormError? = null
    var historyGate: CompletableDeferred<Unit>? = null

    val generateCalls = mutableListOf<String>()
    var getHistoryCalls: Int = 0
        private set

    override suspend fun generate(contactId: String): Result<BrainstormGeneration, BrainstormError> {
        generateCalls += contactId
        return generateResult
    }

    override suspend fun getHistory(contactId: String): Result<List<BrainstormSession>, BrainstormError> {
        getHistoryCalls++
        historyGate?.await()
        return historyError?.let { Result.Error(it) } ?: Result.Success(history)
    }

    var monthlyGenerationCount: Int = 0

    override suspend fun getMonthlyGenerationCount(): Result<Int, BrainstormError> =
        Result.Success(monthlyGenerationCount)
}
