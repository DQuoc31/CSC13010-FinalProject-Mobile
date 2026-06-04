package com.example.ticketboxmobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey
    val qrHash: String,
    val ticketType: String,
    val status: String = "VALID", // VALID, USED, REFUNDED
    val isCheckedIn: Boolean = false,
    val checkInTime: Long? = null,
    val deviceId: String? = null
)
