package com.brunobrandao.expensetracker.data.sync

import com.brunobrandao.expensetracker.data.local.dao.RecurringTransactionDao
import com.brunobrandao.expensetracker.data.local.dao.TransactionDao
import com.brunobrandao.expensetracker.data.local.entity.RecurringTransactionEntity
import com.brunobrandao.expensetracker.data.local.entity.TransactionEntity
import com.brunobrandao.expensetracker.domain.model.RecurringFrequency
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
    private val dao: TransactionDao,
    private val recurringDao: RecurringTransactionDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listenerRegistration: ListenerRegistration? = null
    private var recurringListenerRegistration: ListenerRegistration? = null
    private var syncJob: Job? = null

    /**
     * Called when the user logs in or when the app starts with an active session.
     * For both transactions and recurring rules:
     * a) Backfill: assigns UUIDs to rows that were created before Firebase.
     * b) Push: uploads all unsynced rows to Firestore.
     * c) Listen: attaches real-time listeners to reflect remote changes in Room.
     */
    fun startSync(userId: String) {
        syncJob?.cancel()
        listenerRegistration?.remove()
        listenerRegistration = null
        recurringListenerRegistration?.remove()
        recurringListenerRegistration = null

        syncJob = scope.launch {
            // ── Transactions ─────────────────────────────────────────────────
            dao.getTransactionsWithoutRemoteId().forEach { entity ->
                dao.updateRemoteId(
                    id = entity.id,
                    remoteId = UUID.randomUUID().toString(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            dao.getUnsyncedTransactions().forEach { entity ->
                val current = dao.getTransactionById(entity.id) ?: return@forEach
                if (current.remoteId.isEmpty()) return@forEach
                try { pushToFirestore(current, userId) } catch (_: Exception) {}
            }

            // ── Recurring rules ───────────────────────────────────────────────
            recurringDao.getRecurringWithoutRemoteId().forEach { entity ->
                recurringDao.updateRemoteId(
                    id = entity.id,
                    remoteId = UUID.randomUUID().toString(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            recurringDao.getUnsyncedTransactions().forEach { entity ->
                val current = recurringDao.getById(entity.id) ?: return@forEach
                if (current.remoteId.isEmpty()) return@forEach
                try { pushRecurringToFirestore(current, userId) } catch (_: Exception) {}
            }
        }

        attachListener(userId)
        attachRecurringListener(userId)
    }

    /** Removes both Firestore real-time listeners. */
    fun stopSync() {
        listenerRegistration?.remove()
        listenerRegistration = null
        recurringListenerRegistration?.remove()
        recurringListenerRegistration = null
    }

    /**
     * Attempts to push all unsynced rows before logout.
     * Clears each table only when all its rows are confirmed synced.
     * If offline or any push fails, Room is left intact.
     */
    suspend fun pushPendingAndClear(userId: String) {
        // ── Transactions ─────────────────────────────────────────────────────
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
        if (allSynced) dao.deleteAll()

        // ── Recurring rules ───────────────────────────────────────────────────
        val pendingRules = recurringDao.getUnsyncedTransactions()
        var allRulesSynced = true
        pendingRules.forEach { entity ->
            var current = recurringDao.getById(entity.id) ?: return@forEach
            if (current.remoteId.isEmpty()) {
                val newId = UUID.randomUUID().toString()
                recurringDao.updateRemoteId(current.id, newId, System.currentTimeMillis())
                current = current.copy(remoteId = newId)
            }
            try {
                pushRecurringToFirestore(current, userId)
            } catch (_: Exception) {
                allRulesSynced = false
            }
        }
        if (allRulesSynced) recurringDao.deleteAll()
    }

    /**
     * Write-through: called after a local transaction insert or update.
     * Generates a remoteId if the row doesn't have one, then pushes to Firestore.
     */
    suspend fun syncWrite(localId: Long, userId: String) {
        var entity = dao.getTransactionById(localId) ?: return
        if (entity.remoteId.isEmpty()) {
            val remoteId = UUID.randomUUID().toString()
            dao.updateRemoteId(entity.id, remoteId, entity.updatedAt)
            entity = entity.copy(remoteId = remoteId)
        }
        try { pushToFirestore(entity, userId) } catch (_: Exception) {}
    }

    /**
     * Write-through delete for transactions: deletes locally and removes the Firestore document.
     * Local deletion is guaranteed; Firestore deletion is best-effort.
     */
    suspend fun syncDelete(localId: Long, userId: String) {
        val entity = dao.getTransactionById(localId) ?: return
        dao.deleteById(localId)
        if (entity.remoteId.isNotEmpty()) {
            try {
                firestore.collection("users").document(userId)
                    .collection("transactions").document(entity.remoteId)
                    .delete().await()
            } catch (_: Exception) {}
        }
    }

    /**
     * Write-through: called after a local recurring rule insert or update.
     * Generates a remoteId if the row doesn't have one, then pushes to Firestore.
     */
    suspend fun syncWriteRecurring(localId: Long, userId: String) {
        var entity = recurringDao.getById(localId) ?: return
        if (entity.remoteId.isEmpty()) {
            val remoteId = UUID.randomUUID().toString()
            recurringDao.updateRemoteId(entity.id, remoteId, entity.updatedAt)
            entity = entity.copy(remoteId = remoteId)
        }
        try { pushRecurringToFirestore(entity, userId) } catch (_: Exception) {}
    }

    /**
     * Write-through delete for recurring rules: deletes locally and removes the Firestore document.
     * Local deletion is guaranteed; Firestore deletion is best-effort.
     */
    suspend fun syncDeleteRecurring(localId: Long, userId: String) {
        val entity = recurringDao.getById(localId) ?: return
        recurringDao.deleteById(localId)
        if (entity.remoteId.isNotEmpty()) {
            try {
                firestore.collection("users").document(userId)
                    .collection("recurring_transactions").document(entity.remoteId)
                    .delete().await()
            } catch (_: Exception) {}
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
            "category" to entity.category,
            "date" to entity.date,
            "note" to entity.note,
            "createdAt" to entity.createdAt,
            "updatedAt" to entity.updatedAt,
            "recurringId" to entity.recurringId
        )
        firestore.collection("users").document(userId)
            .collection("transactions").document(entity.remoteId)
            .set(data).await()
        dao.markSynced(entity.id)
    }

    private suspend fun pushRecurringToFirestore(entity: RecurringTransactionEntity, userId: String) {
        require(entity.remoteId.isNotEmpty())
        val data = mapOf(
            "remoteId" to entity.remoteId,
            "description" to entity.description,
            "amount" to entity.amount,
            "type" to entity.type.name,
            "category" to entity.category,
            "note" to entity.note,
            "frequency" to entity.frequency.name,
            "startDate" to entity.startDate,
            "nextDueDate" to entity.nextDueDate,
            "active" to entity.active,
            "updatedAt" to entity.updatedAt
        )
        firestore.collection("users").document(userId)
            .collection("recurring_transactions").document(entity.remoteId)
            .set(data).await()
        recurringDao.markSynced(entity.id)
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

    private fun attachRecurringListener(userId: String) {
        recurringListenerRegistration = firestore
            .collection("users").document(userId)
            .collection("recurring_transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch {
                    for (change in snapshot.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val remote = change.document.toRecurringTransactionEntity() ?: continue
                                val existing = recurringDao.getByRemoteId(remote.remoteId)
                                if (existing != null) {
                                    if (!existing.synced || existing.updatedAt < remote.updatedAt) {
                                        recurringDao.update(remote.copy(id = existing.id))
                                    }
                                } else {
                                    recurringDao.insert(remote)
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                recurringDao.deleteByRemoteId(change.document.id)
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
                category = getString("category") ?: "OTHER",
                date = getLong("date") ?: return null,
                note = getString("note") ?: "",
                createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
                recurringId = getLong("recurringId"),
                synced = true
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.toRecurringTransactionEntity(): RecurringTransactionEntity? {
        return try {
            RecurringTransactionEntity(
                id = 0,
                remoteId = id,
                description = getString("description") ?: return null,
                amount = getDouble("amount") ?: return null,
                type = TransactionType.valueOf(getString("type") ?: return null),
                category = getString("category") ?: "OTHER",
                note = getString("note") ?: "",
                frequency = RecurringFrequency.valueOf(getString("frequency") ?: return null),
                startDate = getLong("startDate") ?: return null,
                nextDueDate = getLong("nextDueDate") ?: return null,
                active = getBoolean("active") ?: true,
                updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
                synced = true
            )
        } catch (_: Exception) {
            null
        }
    }
}
