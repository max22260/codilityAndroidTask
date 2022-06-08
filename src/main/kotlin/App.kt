package coroutines

import kotlinx.coroutines.*
import java.io.Closeable


class AggregateUserDataUseCase(
    private val resolveCurrentUser: suspend () -> UserEntity,
    private val fetchUserComments: suspend (UserId) -> List<CommentEntity>,
    private val fetchSuggestedFriends: suspend (UserId) -> List<FriendEntity>
) : Closeable {

    val coroutineScope = CoroutineScope(Dispatchers.IO)

    suspend fun aggregateDataForCurrentUser(): AggregatedData {
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable -> throwable.stackTrace }


        val aggregatedData = coroutineScope.async(coroutineExceptionHandler) {
            val user = async { resolveCurrentUser() }


            val comments = async {
                withTimeoutOrNull(2000) { fetchUserComments(user.await().id) } ?: emptyList()
            }

            val friends = async {
                withTimeoutOrNull(2000) { fetchSuggestedFriends(user.await().id) }?: emptyList()
            }

            return@async AggregatedData(
                user.await(), comments.await() , friends.await()
            )
        }

        return aggregatedData.await()

    }

    override fun close() {
        coroutineScope.cancel()
    }
}

/**
 *
 * The following is already available on classpath.
 * Please do not uncomment this code or modify.
 * This is only for your convenience to copy-paste code into the IDE

 **/

data class AggregatedData(
    val user: UserEntity, val comments: List<CommentEntity>, val suggestedFriends: List<FriendEntity>
)

typealias UserId = String

data class UserEntity(val id: UserId, val name: String)

data class CommentEntity(val id: String, val content: String)

data class FriendEntity(val id: String, val name: String)


fun main() {
    val aggregatedData = AggregateUserDataUseCase(suspend {
        UserEntity("12", "nagy")
    },

        { userid: UserId ->
            delay(1000L)
            listOf(CommentEntity("1", "nooooooooo"))
        },
        { userid:

          UserId ->
            delay(1000)
            listOf(FriendEntity("233", "ahmed"))
        })

    runBlocking {
        val user = aggregatedData.aggregateDataForCurrentUser()
        println(aggregatedData.coroutineScope.coroutineContext)
        println(user)
        aggregatedData.close()
        println(aggregatedData.coroutineScope.coroutineContext)


    }
}


