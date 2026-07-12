package com.example.ticketboxmobile.p2p

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

class P2PManager(private val context: Context) {
    private val SERVICE_ID = "com.example.ticketboxmobile.P2P_SERVICE"
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private var connectedEndpointId: String? = null

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _validationResult = MutableStateFlow<String?>(null)
    val validationResult: StateFlow<String?> = _validationResult.asStateFlow()

    var isHub: Boolean = false
    var onTicketReceived: ((String, String) -> Unit)? = null

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val message = String(payload.asBytes()!!, StandardCharsets.UTF_8)
                if (isHub) {
                    // Hub receives QR Hash
                    onTicketReceived?.invoke(endpointId, message)
                } else {
                    // Scanner receives Validation Result
                    _validationResult.value = message
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            addLog("Yêu cầu kết nối từ: ${info.endpointName}")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    addLog("Đã kết nối với $endpointId")
                    connectedEndpointId = endpointId
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> addLog("Kết nối bị từ chối: $endpointId")
                else -> addLog("Lỗi kết nối: ${result.status.statusCode}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            addLog("Ngắt kết nối từ $endpointId")
            if (connectedEndpointId == endpointId) {
                connectedEndpointId = null
            }
        }
    }

    fun startAdvertising(hubName: String) {
        isHub = true
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        connectionsClient.startAdvertising(
            hubName, SERVICE_ID, connectionLifecycleCallback, options
        ).addOnSuccessListener {
            addLog("Đang phát sóng (Advertising) tên $hubName...")
        }.addOnFailureListener { e ->
            addLog("Phát sóng thất bại: ${e.message}")
        }
    }

    fun startDiscovery(scannerName: String) {
        isHub = false
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        connectionsClient.startDiscovery(
            SERVICE_ID,
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    addLog("Tìm thấy Hub: ${info.endpointName}. Đang kết nối...")
                    connectionsClient.requestConnection(
                        scannerName, endpointId, connectionLifecycleCallback
                    ).addOnFailureListener { e ->
                        addLog("Yêu cầu kết nối thất bại: ${e.message}")
                    }
                }

                override fun onEndpointLost(endpointId: String) {
                    addLog("Mất tín hiệu Hub: $endpointId")
                }
            },
            options
        ).addOnSuccessListener {
            addLog("Đang tìm kiếm (Discovery)...")
        }.addOnFailureListener { e ->
            addLog("Tìm kiếm thất bại: ${e.message}")
        }
    }

    fun sendValidationResult(endpointId: String, result: String) {
        val payload = Payload.fromBytes(result.toByteArray(StandardCharsets.UTF_8))
        connectionsClient.sendPayload(endpointId, payload)
        addLog("Đã trả kết quả '$result' cho $endpointId")
    }

    fun sendQrHashToHub(qrHash: String) {
        connectedEndpointId?.let { endpointId ->
            val payload = Payload.fromBytes(qrHash.toByteArray(StandardCharsets.UTF_8))
            connectionsClient.sendPayload(endpointId, payload)
            _validationResult.value = null // reset waiting for new result
            addLog("Đã gửi mã $qrHash tới Hub")
        } ?: run {
            addLog("Lỗi: Chưa kết nối với Máy trưởng")
            _validationResult.value = "DISCONNECTED"
        }
    }

    fun clearValidationResult() {
        _validationResult.value = null
    }

    fun addLog(msg: String) {
        Log.d("P2PManager", msg)
        _logs.value = listOf(msg) + _logs.value
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun stop() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpointId = null
        addLog("Đã ngắt P2P")
    }
}
