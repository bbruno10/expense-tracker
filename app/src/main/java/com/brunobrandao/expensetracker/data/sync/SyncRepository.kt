package com.brunobrandao.expensetracker.data.sync

import com.brunobrandao.expensetracker.data.local.dao.CategoryDao
import com.brunobrandao.expensetracker.data.local.dao.RecurringTransactionDao
import com.brunobrandao.expensetracker.data.local.dao.TransactionDao
import com.brunobrandao.expensetracker.data.local.entity.CategoryEntity
import com.brunobrandao.expensetracker.data.local.entity.RecurringTransactionEntity
import com.brunobrandao.expensetracker.data.local.entity.TransactionEntity
import com.brunobrandao.expensetracker.data.preferences.Currency
import com.brunobrandao.expensetracker.data.preferences.UserPreferencesRepository
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
    private val recurringDao: RecurringTransactionDao,
    private val categoryDao: CategoryDao,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listenerRegistration: ListenerRegistration? = null
    private var recurringListenerRegistration: ListenerRegistration? = null
    private var categoryListenerRegistration: ListenerRegistration? = null
    internal var syncJob: Job? = null
    internal var cleanupJob: Job? = null

    /**
     * Called when the user logs in or when the app starts with an active session.
     * For transactions, recurring rules, and categories:
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
        categoryListenerRegistration?.remove()
        categoryListenerRegistration = null

        syncJob = scope.launch {
            // Ensures logout cleanup from the previous session (deleteCustomCategories,
            // clearCurrency, signOut) fully completes before this session's listeners
            // start writing to Room, closing the logout→login race.
            cleanupJob?.join()

            // ── Currency preference ───────────────────────────────────────────
            preferencesRepository.pullCurrency(userId)?.let { code ->
                Currency.entries.find { it.code == code }?.let { currency ->
                    preferencesRepository.updateCurrency(currency)
                }
            }

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

            // ── Categories ────────────────────────────────────────────────────
            pushUnsyncedCategories(userId)

            // ── Listeners (after cleanup is confirmed done) ───────────────────
            attachListener(userId)
            attachRecurringListener(userId)
            attachCategoryListener(userId)
        }
    }

    /** Removes all Firestore real-time listeners. */
    fun stopSync() {
        listenerRegistration?.remove()
        listenerRegistration = null
        recurringListenerRegistration?.remove()
        recurringListenerRegistration = null
        categoryListenerRegistration?.remove()
        categoryListenerRegistration = null
    }

    /**
     * Attempts to push all unsynced rows before logout, then cleans up local state.
     *
     * Categories: push is best-effort; custom categories (isDefault=false) are ALWAYS
     * deleted afterwards, even if the push failed. This unconditional clear is intentional:
     * custom categories are metadata, not financial data, and user isolation takes priority
     * over the rare case of losing an offline-only custom category. Defaults (isDefault=true)
     * are never touched here — they survive the logout and are re-seeded by
     * ensureDefaultCategories() on the next onCreate().
     *
     * Transactions / recurring rules: cleared only when ALL rows confirmed synced (guard).
     * Financial data must never be silently lost.
     */
    suspend fun pushPendingAndClear(userId: String) {
        // ── Categories: best-effort push, then unconditional custom clear ─────
        try { pushUnsyncedCategories(userId) } catch (_: Exception) {}
        categoryDao.deleteCustomCategories()

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
     * Fire-and-forget logout cleanup that runs in the @Singleton scope, surviving ViewModel
     * destruction. Sequence: push pending data → delete custom categories → clear currency →
     * Firebase signOut. signOut is unconditionally last; prior failures are swallowed.
     *
     * startSync() joins this job before attaching listeners, preventing the logout→login race
     * where deleteCustomCategories() would wipe the new user's data.
     */
    fun signOutAndCleanup(userId: String) {
        cleanupJob = scope.launch {
            try { pushPendingAndClear(userId) } catch (_: Exception) {}
            try { preferencesRepository.clearCurrency() } catch (_: Exception) {}
            authRepository.signOut()
        }
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

    // --- Category sync ---

    internal suspend fun pushUnsyncedCategories(userId: String) {
        categoryDao.getUnsynced().forEach { entity ->
            val current = if (entity.remoteId == null) {
                val withId = entity.copy(remoteId = UUID.randomUUID().toString())
                categoryDao.upsert(withId)
                withId
            } else entity
            try { pushCategoryToFirestore(current, userId) } catch (_: Exception) {}
        }
    }

    private suspend fun pushCategoryToFirestore(entity: CategoryEntity, userId: String) {
        val remoteId = entity.remoteId ?: return
        firestore.collection("users").document(userId)
            .collection("categories").document(remoteId)
            .set(entity.toFirestoreMap()).await()
        categoryDao.markSynced(entity.key)
    }

    private fun attachCategoryListener(userId: String) {
        categoryListenerRegistration = firestore
            .collection("users").document(userId)
            .collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch {
                    for (change in snapshot.documentChanges) {
                        processCategoryChange(change.type, change.document)
                    }
                }
            }
    }

    internal suspend fun processCategoryChange(
        changeType: DocumentChange.Type,
        snapshot: DocumentSnapshot
    ) {
        when (changeType) {
            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                val remote = snapshot.toCategoryEntity() ?: return
                val remoteId = remote.remoteId ?: return
                val existing = categoryDao.getByRemoteId(remoteId)
                if (existing != null) {
                    if (!existing.synced || existing.updatedAt < remote.updatedAt) {
                        categoryDao.upsert(remote.copy(key = existing.key))
                    }
                } else {
                    categoryDao.upsert(remote)
                }
            }
            DocumentChange.Type.REMOVED -> {
                // Ignored: categories use soft-delete (archived flag).
                // Deleting the Firestore document does not imply archiving locally —
                // archived is the single source of truth for visibility.
            }
        }
    }

    // --- Account deletion ---

    /**
     * Deletes all Firestore documents belonging to the user, subcollection by subcollection.
     * Must be called while the user is still authenticated (Security Rules require a valid uid).
     */
    suspend fun deleteAllUserDataFromFirestore(userId: String) {
        val userRef = firestore.collection("users").document(userId)
        deleteCollection(userId, "transactions")
        deleteCollection(userId, "recurring_transactions")
        deleteCollection(userId, "categories")
        userRef.collection("settings").document("preferences").delete().await()
    }

    /** Clears all three Room tables. */
    suspend fun deleteAllLocalData() {
        dao.deleteAll()
        recurringDao.deleteAll()
        categoryDao.deleteAll()
    }

    /**
     * Fetches all documents in the given subcollection and deletes them in chunks of 400.
     * Firestore batches are capped at 500 ops; 400 gives headroom.
     */
    private suspend fun deleteCollection(userId: String, collection: String) {
        val docs = firestore.collection("users").document(userId)
            .collection(collection).get().await().documents
        docs.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
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
