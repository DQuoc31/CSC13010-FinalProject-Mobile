package com.example.ticketboxmobile.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MobileApiTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var mobileApi: MobileApi

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        mobileApi = retrofit.create(MobileApi::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getTicketTypes returns correct list`() = runBlocking {
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                [
                    {
                        "id": 1,
                        "event_id": 100,
                        "title": "VIP - Anh Trai Say Hi",
                        "name": "VIP",
                        "event_title": "Anh Trai Say Hi"
                    },
                    {
                        "id": -1,
                        "event_id": 100,
                        "title": "Khách Mời VIP - Anh Trai Say Hi",
                        "name": null,
                        "event_title": null
                    }
                ]
            """.trimIndent())
        mockWebServer.enqueue(mockResponse)

        val response = mobileApi.getTicketTypes("Bearer test_token")

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertEquals(2, body?.size)
        
        val firstItem = body?.get(0)
        assertEquals(1, firstItem?.id)
        assertEquals("VIP - Anh Trai Say Hi", firstItem?.title)
        
        val secondItem = body?.get(1)
        assertEquals(-1, secondItem?.id)
        assertEquals("Khách Mời VIP - Anh Trai Say Hi", secondItem?.title)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/mobile/ticket-types", recordedRequest.path)
        assertEquals("Bearer test_token", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `syncTickets returns success response`() = runBlocking {
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                    "message": "Sync successful",
                    "syncedCount": 5
                }
            """.trimIndent())
        mockWebServer.enqueue(mockResponse)

        val syncRequest = listOf(
            SyncTicketRequest("HASH1", "2024-01-01T10:00:00Z", "DEVICE1")
        )

        val response = mobileApi.syncTickets("Bearer test_token", syncRequest)

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertEquals("Sync successful", body?.message)
        assertEquals(5, body?.syncedCount)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/mobile/sync", recordedRequest.path)
        assertEquals("POST", recordedRequest.method)
    }
}
