package com.example.ticketboxmobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ticketboxmobile.network.LoginRequest
import com.example.ticketboxmobile.network.LoginResponse
import com.example.ticketboxmobile.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.Response

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Đăng nhập TicketBox",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Vui lòng nhập email và mật khẩu"
                    return@Button
                }

                isLoading = true
                errorMessage = null

                coroutineScope.launch {
                    try {
                        val response = RetrofitClient.instance.login(LoginRequest(email, password))
                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body != null) {
                                if (body.role == "staff") {
                                    val sharedPref = context.getSharedPreferences("ticketbox_prefs", android.content.Context.MODE_PRIVATE)
                                    sharedPref.edit().putString("token", body.token).apply()
                                    onLoginSuccess()
                                } else {
                                    errorMessage = "Lỗi: Chỉ tài khoản nhân viên (staff) mới có quyền truy cập ứng dụng soát vé."
                                }
                            } else {
                                errorMessage = "Lỗi phản hồi từ server"
                            }
                        } else {
                            errorMessage = "Đăng nhập thất bại. Kiểm tra lại thông tin."
                        }
                    } catch (e: Exception) {
                        errorMessage = "Không thể kết nối đến server: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Đăng nhập")
            }
        }
    }
}
