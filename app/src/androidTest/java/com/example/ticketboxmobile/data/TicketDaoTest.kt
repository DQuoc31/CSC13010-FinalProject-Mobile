package com.example.ticketboxmobile.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TicketDaoTest {

    private lateinit var ticketDatabase: TicketDatabase
    private lateinit var ticketDao: TicketDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Use in-memory database so information is not stored permanently
        ticketDatabase = Room.inMemoryDatabaseBuilder(
            context, TicketDatabase::class.java
        ).allowMainThreadQueries().build()
        ticketDao = ticketDatabase.ticketDao()
    }

    @After
    fun tearDown() {
        ticketDatabase.close()
    }

    @Test
    fun insertAndGetTicketByHash() = runBlocking {
        val ticket = TicketEntity(
            qrHash = "HASH123",
            ticketType = "VIP",
            status = "VALID",
            isCheckedIn = false
        )
        ticketDao.insertAll(listOf(ticket))

        val retrievedTicket = ticketDao.getTicketByHash("HASH123")
        assertNotNull(retrievedTicket)
        assertEquals("HASH123", retrievedTicket?.qrHash)
        assertEquals("VIP", retrievedTicket?.ticketType)
    }

    @Test
    fun getTicketByHash_notFound() = runBlocking {
        val retrievedTicket = ticketDao.getTicketByHash("NON_EXISTENT")
        assertNull(retrievedTicket)
    }

    @Test
    fun updateTicketStatus() = runBlocking {
        val ticket = TicketEntity(
            qrHash = "HASH456",
            ticketType = "GA",
            status = "VALID",
            isCheckedIn = false
        )
        ticketDao.insertAll(listOf(ticket))

        val checkInTime = System.currentTimeMillis()
        ticketDao.updateTicket(
            hash = "HASH456",
            status = "USED",
            isCheckedIn = true,
            time = checkInTime,
            deviceId = "SCANNER_1"
        )

        val updatedTicket = ticketDao.getTicketByHash("HASH456")
        assertEquals("USED", updatedTicket?.status)
        assertTrue(updatedTicket?.isCheckedIn == true)
        assertEquals(checkInTime, updatedTicket?.checkInTime)
        assertEquals("SCANNER_1", updatedTicket?.deviceId)
    }

    @Test
    fun getCheckedInTickets() = runBlocking {
        val ticket1 = TicketEntity(qrHash = "T1", ticketType = "GA", isCheckedIn = true)
        val ticket2 = TicketEntity(qrHash = "T2", ticketType = "VIP", isCheckedIn = false)
        val ticket3 = TicketEntity(qrHash = "T3", ticketType = "SVIP", isCheckedIn = true)
        ticketDao.insertAll(listOf(ticket1, ticket2, ticket3))

        val checkedInTickets = ticketDao.getCheckedInTickets()
        assertEquals(2, checkedInTickets.size)
        assertTrue(checkedInTickets.any { it.qrHash == "T1" })
        assertTrue(checkedInTickets.any { it.qrHash == "T3" })
    }

    @Test
    fun deleteAll() = runBlocking {
        val ticket = TicketEntity(qrHash = "T1", ticketType = "GA")
        ticketDao.insertAll(listOf(ticket))
        
        ticketDao.deleteAll()
        val tickets = ticketDao.getAllTickets()
        assertTrue(tickets.isEmpty())
    }
}
