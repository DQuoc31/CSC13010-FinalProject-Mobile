package com.example.ticketboxmobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ticketboxmobile.data.TicketDao
import com.example.ticketboxmobile.data.TicketEntity
import com.example.ticketboxmobile.network.RetrofitClient
import com.example.ticketboxmobile.network.SyncTicketRequest
import com.example.ticketboxmobile.network.TicketTypeResponse
import com.example.ticketboxmobile.p2p.P2PManager
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(
    p2pManager: P2PManager,
    ticketDao: TicketDao,
    token: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val logs by p2pManager.logs.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    var showTypeDialog by remember { mutableStateOf(false) }
    var ticketTypes by remember { mutableStateOf<List<TicketTypeResponse>>(emptyList()) }
    var isFetching by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var typeToDownload by remember { mutableStateOf<TicketTypeResponse?>(null) }

    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Xác nhận thoát", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn sắp thoát khỏi chế độ Máy Trưởng. Toàn bộ dữ liệu vé cũ và nhật ký (logs) sẽ bị xoá.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        coroutineScope.launch(Dispatchers.IO) {
                            ticketDao.deleteAll()
                            p2pManager.clearLogs()
                            withContext(Dispatchers.Main) {
                                onBack()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Thoát và Xoá dữ liệu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (typeToDownload != null) {
        AlertDialog(
            onDismissRequest = { typeToDownload = null },
            title = { Text("Cảnh báo ghi đè dữ liệu", fontWeight = FontWeight.Bold) },
            text = { Text("Tải danh sách vé mới sẽ xoá toàn bộ danh sách vé và lịch sử soát vé hiện tại trên máy này (Chỉ được phép tải 1 loại vé mỗi lần).\n\nBạn có chắc chắn muốn tải loại vé: ${typeToDownload?.title}?") },
            confirmButton = {
                Button(
                    onClick = {
                        val type = typeToDownload!!
                        typeToDownload = null
                        showTypeDialog = false

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                p2pManager.addLog("Đang tải danh sách vé loại ${type.title}...")
                                val response = RetrofitClient.mobileApi.getTickets("Bearer $token", type.event_id, type.id)
                                if (response.isSuccessful) {
                                    val serverTickets = response.body() ?: emptyList()
                                    val entities = serverTickets.map { t ->
                                        TicketEntity(
                                            qrHash = t.qr_code_hash,
                                            ticketType = type.title,
                                            status = t.status,
                                            isCheckedIn = t.is_checked_in,
                                            checkInTime = null,
                                            deviceId = t.device_id
                                        )
                                    }
                                    ticketDao.deleteAll()
                                    p2pManager.addLog("Đã xoá danh sách vé cũ.")
                                    ticketDao.insertAll(entities)
                                    p2pManager.addLog("Đã đồng bộ ${entities.size} vé loại ${type.title} thành công!")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Tải vé thành công!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    p2pManager.addLog("Lỗi tải vé: ${response.code()}")
                                }
                            } catch (e: Exception) {
                                p2pManager.addLog("Lỗi mạng: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Đồng ý tải")
                }
            },
            dismissButton = {
                TextButton(onClick = { typeToDownload = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showTypeDialog) {
        AlertDialog(
            onDismissRequest = { showTypeDialog = false },
            title = { Text("Chọn sự kiện và loại vé", fontWeight = FontWeight.Bold) },
            text = {
                if (isFetching) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn {
                        items(ticketTypes) { type ->
                            ElevatedCard(
                                onClick = { typeToDownload = type },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = type.title,
                                    modifier = Modifier.padding(16.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTypeDialog = false }) { Text("Đóng") }
            }
        )
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Máy Trưởng (Local Hub)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quản lý dữ liệu vé",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Button(
                        onClick = {
                            showTypeDialog = true
                            isFetching = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val response = RetrofitClient.mobileApi.getTicketTypes("Bearer $token")
                                    if (response.isSuccessful) {
                                        val types = response.body() ?: emptyList()
                                        withContext(Dispatchers.Main) {
                                            ticketTypes = types
                                            isFetching = false
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            isFetching = false
                                            Toast.makeText(context, "Lỗi lấy DS Loại vé", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isFetching = false
                                        Toast.makeText(context, "Lỗi mạng", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tải dữ liệu từ Server")
                    }

                    Button(
                        onClick = {
                            if (isSyncing) return@Button
                            isSyncing = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val checkedInTickets = ticketDao.getCheckedInTickets()
                                    if (checkedInTickets.isEmpty()) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Không có vé nào mới để đồng bộ", Toast.LENGTH_SHORT).show()
                                            isSyncing = false
                                        }
                                        return@launch
                                    }

                                    p2pManager.addLog("Đang đồng bộ ${checkedInTickets.size} vé lên server...")
                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                                    val syncRequests = checkedInTickets.map { t ->
                                        SyncTicketRequest(
                                            qr_code_hash = t.qrHash,
                                            check_in_time = t.checkInTime?.let { dateFormat.format(Date(it)) } ?: dateFormat.format(Date()),
                                            device_id = t.deviceId ?: "UNKNOWN"
                                        )
                                    }

                                    val response = RetrofitClient.mobileApi.syncTickets("Bearer $token", syncRequests)
                                    if (response.isSuccessful) {
                                        val syncRes = response.body()
                                        p2pManager.addLog("Đã đồng bộ thành công ${syncRes?.syncedCount ?: 0} vé.")
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Đồng bộ thành công!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        p2pManager.addLog("Lỗi đồng bộ: ${response.code()}")
                                    }
                                } catch (e: Exception) {
                                    p2pManager.addLog("Lỗi mạng khi đồng bộ: ${e.message}")
                                } finally {
                                    withContext(Dispatchers.Main) {
                                        isSyncing = false
                                    }
                                }
                            }
                        },
                        enabled = !isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSecondary)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đồng bộ lên Server")
                        }
                    }
                }
            }

            Text(
                text = "Logs hoạt động",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(logs) { log ->
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}
