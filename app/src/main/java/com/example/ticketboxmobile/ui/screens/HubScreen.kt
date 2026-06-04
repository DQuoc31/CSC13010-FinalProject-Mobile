package com.example.ticketboxmobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ticketboxmobile.data.TicketDao
import com.example.ticketboxmobile.data.TicketEntity
import com.example.ticketboxmobile.p2p.P2PManager
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun HubScreen(
    p2pManager: P2PManager,
    ticketDao: TicketDao,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val logs by p2pManager.logs.collectAsState()

    DisposableEffect(Unit) {
        p2pManager.startAdvertising("Gate-SVIP-Hub")

        p2pManager.onTicketReceived = { endpointId, qrHash ->
            coroutineScope.launch {
                val ticket = ticketDao.getTicketByHash(qrHash)
                val result = if (ticket == null) {
                    "INVALID"
                } else if (ticket.isCheckedIn) {
                    "USED"
                } else {
                    ticketDao.updateTicket(qrHash, "VALID", true, System.currentTimeMillis(), "HUB_DEVICE")
                    "VALID"
                }
                p2pManager.sendValidationResult(endpointId, result)
            }
        }

        onDispose {
            p2pManager.stop()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Máy Trưởng (Local Hub)",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                coroutineScope.launch {
                    val mockTickets = List(10) { i ->
                        TicketEntity(
                            qrHash = "MOCK_QR_$i",
                            ticketType = "SVIP",
                            status = "VALID",
                            isCheckedIn = false
                        )
                    }
                    ticketDao.insertAll(mockTickets)
                    p2pManager.addLog("Đã đồng bộ 10 vé mẫu (MOCK_QR_0 đến MOCK_QR_9)")
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("Sync Mock Data")
        }

        Text("Logs hoạt động:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(logs) { log ->
                Text(text = log, modifier = Modifier.padding(vertical = 4.dp))
                Divider()
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Quay lại")
        }
    }
}
