package com.brunobrandao.expensetracker.data.sync

import com.brunobrandao.expensetracker.data.local.dao.TransactionDao
import com.brunobrandao.expensetracker.data.local.entity.TransactionEntity
import com.brunobrandao.expensetracker.domain.model.Category
import com.brunobrandao.expensetracker.domain.model.TransactionType
import com.brunobrandao.expensetracker.domain.repository.AuthRepository
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val dao: TransactionDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listenerRegistration: ListenerRegistration? = null
    private var syncJob: Job? = null

    /**
     * Called when the user logs in or when the app starts with an active session.
     * a) Backfill: assigns UUIDs to rows that were created before Firebase.
     * b) Push: uploads all unsynced rows to Firestore.
     * c) Listen: attaches a real-time listener to reflect remote changes in Room.
     */
    fun startSync(userId: String) {
        syncJob?.cancel()
        listenerRegistration?.remove()
        listenerRegistration = null

        syncJob = scope.launch {
            // a) Backfill local rows that have no remoteId yet
            dao.getTransactionsWithoutRemoteId().forEach { entity ->
                dao.updateRemoteId(
                    id = entity.id,
                    remoteId = UUID.randomUUID().toString(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            // b) Push every unsynced row (including those just backfilled)
            dao.getUnsyncedTransactions().forEach { entity ->
                val current = dao.getTransactionById(entity.id) ?: return@forEach
                if (current.remoteId.isEmpty()) return@forEach
                try { pushToFirestore(current, userId) } catch (_: Exception) { /* retry on next sync */ }
            }
        }

        // c) Attach real-time listener immediately (does not wait for job above)
        attachListener(userId)
    }

    /** Removes the Firestore real-time listener. */
    fun stopSync() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    /**
     * Attempts to push all unsynced rows before logout.
     * Clears Room only when all rows are confirmed synced.
     * If offline or any push fails, Room is left intact.
     */
    suspend fun pushPendingAndClear(userId: String) {
        val pending = dao.getUnsyncedTransactions()
        var allSynced = true

        pending.forEach { entity ->
            var current = dao.getTransactionById(entity.id) ?: return@forEach
            if (current.remoteId.isEmpty()) {
                val newId = UUID.randomUUID().toString()
                dao.updateRemoteId(current.id, newId, System.currentTimeMillis())
                current = current.copy(remoteId = newId)
            }
            try {
                pushToFirestore(current, userId)
            } catch (_: Exception) {
                allSynced = false
            }
        }

        if (allSynced) {
            dao.deleteAll()
        }
    }

    /**
     * Write-through: called after a local insert or update.
     * Generates a remoteId if the row doesn't have one, then pushes to Firestore.
     */
    suspend fun syncWrite(localId: Long, userId: String) {
        var entity = dao.getTransactionById(localId) ?: return
        if (entity.remoteId.isEmpty()) {
            val remoteId = UUID.randomUUID().toString()
            dao.updateRemoteId(entity.id, remoteId, entity.updatedAt)
            entity = entity.copy(remoteId = remoteId)
        }
        try { pushToFirestore(entity, userId) } catch (_: Exception) { /* offline - will push on next startSync */ }
    }

    /**
     * Write-through delete: deletes locally and removes the Firestore document.
     * Local deletion is guaranteed; Firestore deletion is best-effort (works offline
     * via Firebase offline persistence).
     */
    suspend fun syncDelete(localId: Long, userId: String) {
        val entity = dao.getTransactionById(localId) ?: return
        dao.deleteById(localId)
        if (entity.remoteId.isNotEmpty()) {
            try {
                firestore.collection("users").document(userId)
                    .collection("transactions").document(entity.remoteId)
                    .delete().await()
            } catch (_: Exception) { /* offline persistence queues the delete */ }
        }
    }

    // --- Private helpers ---

    private suspend fun pushToFirestore(entity: TransactionEntity, userId: String) {
        require(entity.remoteId.isNotEmpty())
        val data = mapOf(
            "remoteId" to entity.remoteId,
            "description" to entity.description,
            "amount" to entity.amount,
            "type" to entity.type.name,
            "category" to entity.category.name,
            "date" to entity.date,
            "note" to entity.note,
            "createdAt" to entity.createdAt,
            "updatedAt" to entity.updatedAt
        )
        firestore.collection("users").document(userId)
            .collection("transactions").document(entity.remoteId)
            .set(data).await()
        dao.markSynced(entity.id)
    }

    private fun attachListener(userId: String) {
        listenerRegistration = firestore
            .collection("users").document(userId)
            .collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch {
                    for (change in snapshot.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val remote = change.document.toTransactionEntity() ?: continue
                                val existing = dao.getByRemoteId(remote.remoteId)
                                if (existing != null) {
                                    // Last-write-wins: only overwrite if remote is newer or local is unsynced
                                    if (!existing.synced || existing.updatedAt < remote.updatedAt) {
                                        dao.update(remote.copy(id = existing.id))
                                    }
                                } else {
                                    dao.insert(remote)
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                dao.deleteByRemoteId(change.document.id)
                            }
                        }
                    }
                }
            }
    }

    private fun DocumentSnapshot.toTransactionEntity(): TransactionEntity? {
        return try {
            TransactionEntity(
                id = 0,
                remoteId = id,
                description = getString("description") ?: return null,
                amount = getDouble("amount") ?: return null,
                type = TransactionType.valueOf(getString("type") ?: return null),
                category = Category.valueOf(getString("category") ?: return null),
                date = getLong("date") ?: return null,
                note = getString("note") ?: "",
                createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
                synced = true
            )
        } catch (_: Exception) {
            null
        }
    }
}
