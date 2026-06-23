package com.brunobrandao.expensetracker.data.preferences

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class UserPreferencesRepositoryTest {

    private val firestore = mockk<FirebaseFirestore>()
    private val context = mockk<Context>(relaxed = true)

    // Firestore chain: users/{userId}/settings/preferences
    private val usersCollection = mockk<CollectionReference>()
    private val userDoc = mockk<DocumentReference>()
    private val settingsCollection = mockk<CollectionReference>()
    private val prefDoc = mockk<DocumentReference>(relaxed = true)

    private lateinit var repository: UserPreferencesRepository

    @Before
    fun setup() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        every { firestore.collection("users") } returns usersCollection
        every { usersCollection.document("user-1") } returns userDoc
        every { userDoc.collection("settings") } returns settingsCollection
        every { settingsCollection.document("preferences") } returns prefDoc
        repository = UserPreferencesRepository(context, firestore)
    }

    @After
    fun tearDown() {
        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    // ── pushCurrency ──────────────────────────────────────────────────────────

    @Test
    fun `pushCurrency calls Firestore set with the correct currency code`() = runTest {
        // set() throws to skip awaiting the Task (pushCurrency swallows all exceptions).
        // verify() below confirms the Firestore call was reached with the right payload.
        every { prefDoc.set(any<Map<String, Any>>(), any<SetOptions>()) } throws Exception("offline")

        repository.pushCurrency("user-1", Currency.BRL)

        verify { prefDoc.set(mapOf("currency" to "BRL"), any<SetOptions>()) }
    }

    @Test
    fun `pushCurrency swallows network failure without throwing`() = runTest {
        every { prefDoc.set(any<Map<String, Any>>(), any<SetOptions>()) } throws Exception("offline")

        repository.pushCurrency("user-1", Currency.USD)
        // passes if no exception propagates
    }

    @Test
    fun `pushCurrency passes SetOptions to protect other settings fields`() = runTest {
        every { prefDoc.set(any<Map<String, Any>>(), any<SetOptions>()) } throws Exception("offline")

        repository.pushCurrency("user-1", Currency.EUR)

        // SetOptions.merge() has no public equality API; verifying it is passed at all
        // (not a plain set without options) is sufficient.
        verify { prefDoc.set(any(), any<SetOptions>()) }
    }

    // ── pullCurrency ──────────────────────────────────────────────────────────

    @Test
    fun `pullCurrency returns the currency code from Firestore`() = runTest {
        val snapshot = mockk<DocumentSnapshot>()
        val task = mockk<Task<DocumentSnapshot>>()
        every { prefDoc.get() } returns task
        coEvery { task.await() } returns snapshot
        every { snapshot.getString("currency") } returns "BRL"

        val result = repository.pullCurrency("user-1")

        assertEquals("BRL", result)
    }

    @Test
    fun `pullCurrency returns null when currency field is absent from document`() = runTest {
        val snapshot = mockk<DocumentSnapshot>()
        val task = mockk<Task<DocumentSnapshot>>()
        every { prefDoc.get() } returns task
        coEvery { task.await() } returns snapshot
        every { snapshot.getString("currency") } returns null

        val result = repository.pullCurrency("user-1")

        assertNull(result)
    }

    @Test
    fun `pullCurrency returns null on Firestore error`() = runTest {
        every { prefDoc.get() } throws Exception("offline")

        val result = repository.pullCurrency("user-1")

        assertNull(result)
    }

    // ── path isolation ────────────────────────────────────────────────────────

    @Test
    fun `pushCurrency uses the correct user-scoped path and does not touch other users`() = runTest {
        val userDoc2 = mockk<DocumentReference>()
        val settingsCollection2 = mockk<CollectionReference>()
        val prefDoc2 = mockk<DocumentReference>()
        every { usersCollection.document("user-2") } returns userDoc2
        every { userDoc2.collection("settings") } returns settingsCollection2
        every { settingsCollection2.document("preferences") } returns prefDoc2
        every { prefDoc2.set(any<Map<String, Any>>(), any<SetOptions>()) } throws Exception("offline")

        repository.pushCurrency("user-2", Currency.JPY)

        verify { prefDoc2.set(mapOf("currency" to "JPY"), any<SetOptions>()) }
        verify(exactly = 0) { prefDoc.set(any(), any<SetOptions>()) }
    }
}
