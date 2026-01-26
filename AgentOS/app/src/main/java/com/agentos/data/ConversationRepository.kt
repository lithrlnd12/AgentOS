package com.agentos.data

import android.content.Context
import androidx.room.*
import com.agentos.claude.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for persisting conversation history.
 * Uses Room database for local storage.
 */
class ConversationRepository(context: Context) {

    private val database = Room.databaseBuilder(
        context.applicationContext,
        ConversationDatabase::class.java,
        "conversation_db"
    ).build()

    private val messageDao = database.messageDao()

    /**
     * Save a message to the database.
     */
    suspend fun saveMessage(message: Message, conversationId: String = "default") {
        val entity = MessageEntity(
            conversationId = conversationId,
            role = message.role,
            content = message.content,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insert(entity)
    }

    /**
     * Get recent messages for a conversation.
     */
    suspend fun getRecentMessages(
        count: Int = 20,
        conversationId: String = "default"
    ): List<Message> {
        return messageDao.getRecentMessages(conversationId, count)
            .map { entity ->
                Message(role = entity.role, content = entity.content)
            }
    }

    /**
     * Observe messages as a Flow.
     */
    fun observeMessages(conversationId: String = "default"): Flow<List<Message>> {
        return messageDao.observeMessages(conversationId).map { entities ->
            entities.map { entity ->
                Message(role = entity.role, content = entity.content)
            }
        }
    }

    /**
     * Clear all messages for a conversation.
     */
    suspend fun clearConversation(conversationId: String = "default") {
        messageDao.clearConversation(conversationId)
    }

    /**
     * Clear all conversations.
     */
    suspend fun clearAll() {
        messageDao.clearAll()
    }

    /**
     * Get message count for a conversation.
     */
    suspend fun getMessageCount(conversationId: String = "default"): Int {
        return messageDao.getMessageCount(conversationId)
    }

    /**
     * Delete old messages to maintain storage limits.
     */
    suspend fun pruneOldMessages(
        keepCount: Int = 100,
        conversationId: String = "default"
    ) {
        val count = messageDao.getMessageCount(conversationId)
        if (count > keepCount) {
            val toDelete = count - keepCount
            messageDao.deleteOldestMessages(conversationId, toDelete)
        }
    }
}

/**
 * Room database for conversation storage.
 */
@Database(entities = [MessageEntity::class], version = 1, exportSchema = false)
abstract class ConversationDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}

/**
 * Entity representing a stored message.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: String,
    val role: String,
    val content: String,
    val timestamp: Long
)

/**
 * DAO for message operations.
 */
@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("""
        SELECT * FROM messages
        WHERE conversationId = :conversationId
        ORDER BY timestamp DESC
        LIMIT :count
    """)
    suspend fun getRecentMessages(conversationId: String, count: Int): List<MessageEntity>

    @Query("""
        SELECT * FROM messages
        WHERE conversationId = :conversationId
        ORDER BY timestamp ASC
    """)
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearConversation(conversationId: String)

    @Query("DELETE FROM messages")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: String): Int

    @Query("""
        DELETE FROM messages
        WHERE id IN (
            SELECT id FROM messages
            WHERE conversationId = :conversationId
            ORDER BY timestamp ASC
            LIMIT :count
        )
    """)
    suspend fun deleteOldestMessages(conversationId: String, count: Int)
}
