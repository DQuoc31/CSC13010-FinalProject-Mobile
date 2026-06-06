package com.example.ticketboxmobile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets WHERE qrHash = :hash LIMIT 1")
    suspend fun getTicketByHash(hash: String): TicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tickets: List<TicketEntity>)

    @Query("UPDATE tickets SET status = :status, isCheckedIn = :isCheckedIn, checkInTime = :time, deviceId = :deviceId WHERE qrHash = :hash")
    suspend fun updateTicket(
        hash: String,
        status: String,
        isCheckedIn: Boolean,
        time: Long?,
        deviceId: String?
    )

    @Query("SELECT * FROM tickets ORDER BY checkInTime DESC")
    suspend fun getAllTickets(): List<TicketEntity>

    @Query("SELECT * FROM tickets WHERE isCheckedIn = 1")
    suspend fun getCheckedInTickets(): List<TicketEntity>

    @Query("DELETE FROM tickets")
    suspend fun deleteAll()
}
