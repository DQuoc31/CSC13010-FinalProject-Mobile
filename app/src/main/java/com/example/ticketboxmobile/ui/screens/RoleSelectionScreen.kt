package com.example.ticketboxmobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RoleSelectionScreen(
    onSelectHub: () -> Unit,
    onSelectScanner: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TicketBox Offline Check-in",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onSelectHub,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(text = "Khởi tạo Máy Trưởng (Local Hub)")
        }

        Button(
            onClick = onSelectScanner,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp)
        ) {
            Text(text = "Sử dụng làm Máy Quét (Scanner)")
        }
    }
}
