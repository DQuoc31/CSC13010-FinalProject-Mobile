package com.example.ticketboxmobile.network

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Path

data class TicketTypeResponse(
    val id: Int,
    val event_id: Int,
    val title: String,
    val name: String?,
    val event_title: String?
)

data class TicketResponse(
    val id: Int,
    val order_id: Int?,
    val ticket_type_id: Int,
    val user_id: Int,
    val qr_code_hash: String,
    val status: String,
    val is_checked_in: Boolean,
    val check_in_time: String?,
    val offline_checked_at: String?,
    val device_id: String?
)

data class SyncTicketRequest(
    val qr_code_hash: String,
    val check_in_time: String,
    val device_id: String
)

data class SyncResponse(
    val message: String,
    val syncedCount: Int
)

interface MobileApi {
    @GET("/api/mobile/ticket-types")
    suspend fun getTicketTypes(@Header("Authorization") token: String): retrofit2.Response<List<TicketTypeResponse>>

    @GET("/api/mobile/tickets/{eventId}/{ticketTypeId}")
    suspend fun getTickets(
        @Header("Authorization") token: String,
        @Path("eventId") eventId: Int,
        @Path("ticketTypeId") ticketTypeId: Int
    ): retrofit2.Response<List<TicketResponse>>

    @POST("/api/mobile/sync")
    suspend fun syncTickets(
        @Header("Authorization") token: String,
        @Body tickets: List<SyncTicketRequest>
    ): retrofit2.Response<SyncResponse>
}
