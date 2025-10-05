package com.example.mqtttestemqx

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.mqtttestemqx.ui.theme.MQTTtestEMQXTheme
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MQTTtestEMQXTheme {
                MqttDemoScreen()
            }
        }
    }
}

/**
 * MqttManager 类，使用现代的 HiveMQ 客户端重写
 */
class MqttManager {
    // --- MQTT 连接信息 (现在可以动态配置) ---
    private var serverHost = "a11331ff.ala.cn-hangzhou.emqxsl.cn"
    private var serverPort = 8883
    private var topic = "test/topic/from/android"
    private var username = "kukudai"
    private var password = "123456"

    // --- 状态管理 ---
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _receivedMessages = MutableStateFlow<List<String>>(emptyList())
    val receivedMessages = _receivedMessages.asStateFlow()

    private var client: Mqtt5AsyncClient? = null

    companion object {
        private const val TAG = "MqttManager_HiveMQ"
    }

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, FAILED
    }

    /**
     * 更新连接配置
     */
    fun updateConfig(host: String, port: Int, topic: String, user: String, pass: String) {
        this.serverHost = host
        this.serverPort = port
        this.topic = topic
        this.username = user
        this.password = pass
    }

    /**
     * 获取当前配置
     */
    fun getConfig() = mapOf(
        "host" to serverHost,
        "port" to serverPort.toString(),
        "topic" to topic,
        "username" to username,
        "password" to password
    )

    /**
     * 连接到 MQTT Broker
     */
    fun connect() {
        if (client != null && client?.state?.isConnectedOrReconnect == true) {
            Log.w(TAG, "客户端已经连接，无需重复操作")
            return
        }

        _connectionState.value = ConnectionState.CONNECTING

        // 使用 HiveMQ 客户端构建器
        client = MqttClient.builder()
            .useMqttVersion5()
            .serverHost(serverHost)
            .serverPort(serverPort)
            .sslWithDefaultConfig() // 启用 SSL
            .identifier(UUID.randomUUID().toString())
            .addConnectedListener { Log.d(TAG, "客户端已连接") }
            .addDisconnectedListener { context ->
                // 仅在非用户主动断开时标记为失败
                if (context.cause != null && _connectionState.value != ConnectionState.DISCONNECTED) {
                    _connectionState.value = ConnectionState.FAILED
                }
                Log.e(TAG, "客户端断开连接: ${context.cause}", context.cause)
            }
            .buildAsync() // 异步构建

        // 连接并处理结果
        client?.connectWith()
            ?.simpleAuth()
            ?.username(username)
            ?.password(password.toByteArray())
            ?.applySimpleAuth()
            ?.send()
            ?.whenComplete { _, throwable ->
                if (throwable != null) {
                    _connectionState.value = ConnectionState.FAILED
                    Log.e(TAG, "连接失败: ${throwable.message}", throwable)
                } else {
                    _connectionState.value = ConnectionState.CONNECTED
                    Log.d(TAG, "连接成功!")
                    subscribeToTopic()
                }
            }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED // 立即更新UI状态
        client?.disconnect()?.whenComplete { _, throwable ->
            if (throwable != null) {
                Log.e(TAG, "断开连接时发生错误", throwable)
            } else {
                Log.d(TAG, "成功断开连接")
            }
        }
    }

    /**
     * 发布消息
     */
    fun publishMessage(message: String) {
        if (client?.state?.isConnected != true) {
            Log.e(TAG, "无法发布消息，客户端未连接")
            return
        }
        client?.publish(
            Mqtt5Publish.builder()
                .topic(topic)
                .payload(message.toByteArray())
                .build()
        )?.whenComplete { result, throwable ->
            if (throwable != null) {
                Log.e(TAG, "发布消息失败", throwable)
            } else {
                Log.d(TAG, "消息发布成功: $result")
            }
        }
    }

    /**
     * 订阅主题
     */
    private fun subscribeToTopic() {
        client?.subscribeWith()
            ?.topicFilter(topic)
            ?.callback { publish ->
                val msg = "收到消息: ${String(publish.payloadAsBytes)} (来自: ${publish.topic})"
                Log.d(TAG, msg)
                _receivedMessages.value = listOf(msg) + _receivedMessages.value
            }
            ?.send()
            ?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "订阅主题失败", throwable)
                } else {
                    Log.d(TAG, "成功订阅主题: $topic")
                }
            }
    }
}

@Composable
fun MqttDemoScreen() {
    val mqttManager = remember { MqttManager() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // 使用 DisposableEffect 来管理连接的生命周期，防止内存泄漏
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                mqttManager.disconnect()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mqttManager.disconnect()
        }
    }

    val connectionState by mqttManager.connectionState.collectAsState()
    val receivedMessages by mqttManager.receivedMessages.collectAsState()
    var messageText by remember { mutableStateOf("") }

    // 配置参数状态
    val config = mqttManager.getConfig()
    var serverHost by remember { mutableStateOf(config["host"] ?: "") }
    var serverPort by remember { mutableStateOf(config["port"] ?: "") }
    var topic by remember { mutableStateOf(config["topic"] ?: "") }
    var username by remember { mutableStateOf(config["username"] ?: "") }
    var password by remember { mutableStateOf(config["password"] ?: "") }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("MQTT Demo (HiveMQ)", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // 根据连接状态显示配置输入框或配置信息
            if (connectionState == MqttManager.ConnectionState.CONNECTED ||
                connectionState == MqttManager.ConnectionState.CONNECTING) {
                // 已连接或连接中：显示配置信息
                ConfigDisplaySection(
                    host = serverHost,
                    port = serverPort,
                    topic = topic,
                    username = username
                )
            } else {
                // 未连接：显示配置输入框
                ConfigInputSection(
                    host = serverHost,
                    port = serverPort,
                    topic = topic,
                    username = username,
                    password = password,
                    onHostChange = { serverHost = it },
                    onPortChange = { serverPort = it },
                    onTopicChange = { topic = it },
                    onUsernameChange = { username = it },
                    onPasswordChange = { password = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            ConnectionStatus(state = connectionState)
            Spacer(modifier = Modifier.height(16.dp))
            ConnectionControls(
                state = connectionState,
                onConnect = {
                    // 更新配置并连接
                    val portInt = serverPort.toIntOrNull() ?: 8883
                    mqttManager.updateConfig(serverHost, portInt, topic, username, password)
                    mqttManager.connect()
                },
                onDisconnect = { mqttManager.disconnect() }
            )
            Spacer(modifier = Modifier.height(24.dp))
            PublishSection(
                message = messageText,
                onMessageChange = { messageText = it },
                onPublish = {
                    if (messageText.isNotBlank()) {
                        mqttManager.publishMessage(messageText)
                        messageText = ""
                    }
                },
                isEnabled = connectionState == MqttManager.ConnectionState.CONNECTED
            )
            Spacer(modifier = Modifier.height(24.dp))
            ReceivedMessages(messages = receivedMessages)
        }
    }
}

/**
 * 配置输入区域 - 未连接时显示
 */
@Composable
fun ConfigInputSection(
    host: String,
    port: String,
    topic: String,
    username: String,
    password: String,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onTopicChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("MQTT 配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                label = { Text("服务器地址") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text("端口") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = topic,
                onValueChange = onTopicChange,
                label = { Text("订阅主题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

/**
 * 配置显示区域 - 已连接时显示
 */
@Composable
fun ConfigDisplaySection(
    host: String,
    port: String,
    topic: String,
    username: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("当前配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            ConfigInfoRow("服务器", host)
            ConfigInfoRow("端口", port)
            ConfigInfoRow("主题", topic)
            ConfigInfoRow("用户名", username)
        }
    }
}

@Composable
fun ConfigInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color(0xFF1976D2)
        )
    }
}

@Composable
fun ConnectionStatus(state: MqttManager.ConnectionState) {
    val (statusText, color) = when (state) {
        MqttManager.ConnectionState.CONNECTED -> "已连接" to Color(0xFF4CAF50)
        MqttManager.ConnectionState.CONNECTING -> "连接中..." to Color.Gray
        MqttManager.ConnectionState.DISCONNECTED -> "已断开" to Color.Red
        MqttManager.ConnectionState.FAILED -> "连接失败" to Color.Red
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("状态: ", fontSize = 18.sp)
        Text(
            text = statusText,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        if (state == MqttManager.ConnectionState.CONNECTING) {
            Spacer(modifier = Modifier.width(8.dp))
            CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
fun ConnectionControls(
    state: MqttManager.ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Button(
            onClick = onConnect,
            enabled = state != MqttManager.ConnectionState.CONNECTED && state != MqttManager.ConnectionState.CONNECTING
        ) {
            Text("连 接")
        }
        Button(
            onClick = onDisconnect,
            enabled = state == MqttManager.ConnectionState.CONNECTED
        ) {
            Text("断 开")
        }
    }
}

@Composable
fun PublishSection(
    message: String,
    onMessageChange: (String) -> Unit,
    onPublish: () -> Unit,
    isEnabled: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            label = { Text("要发送的消息") },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEnabled
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onPublish,
            modifier = Modifier.align(Alignment.End),
            enabled = isEnabled
        ) {
            Text("发 送")
        }
    }
}

@Composable
fun ReceivedMessages(messages: List<String>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("接收到的消息:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("暂无消息", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    items(messages) { msg ->
                        Text(msg, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "主界面 - 默认")
@Composable
fun DefaultPreview() {
    MQTTtestEMQXTheme {
        MqttDemoScreen()
    }
}

@Preview(showBackground = true, name = "连接状态 - 已连接")
@Composable
fun PreviewConnected() {
    MQTTtestEMQXTheme {
        ConnectionStatus(state = MqttManager.ConnectionState.CONNECTED)
    }
}

@Preview(showBackground = true, name = "连接状态 - 连接中")
@Composable
fun PreviewConnecting() {
    MQTTtestEMQXTheme {
        ConnectionStatus(state = MqttManager.ConnectionState.CONNECTING)
    }
}

@Preview(showBackground = true, name = "连接状态 - 已断开")
@Composable
fun PreviewDisconnected() {
    MQTTtestEMQXTheme {
        ConnectionStatus(state = MqttManager.ConnectionState.DISCONNECTED)
    }
}

@Preview(showBackground = true, name = "发送消息区域")
@Composable
fun PreviewPublishSection() {
    MQTTtestEMQXTheme {
        PublishSection(
            message = "测试消息",
            onMessageChange = {},
            onPublish = {},
            isEnabled = true
        )
    }
}

@Preview(showBackground = true, name = "接收消息列表")
@Composable
fun PreviewReceivedMessages() {
    MQTTtestEMQXTheme {
        ReceivedMessages(
            messages = listOf(
                "收到消息: Hello World (来自: test/topic)",
                "收到消息: MQTT测试 (来自: test/topic)",
                "收到消息: 第三条消息 (来自: test/topic)"
            )
        )
    }
}

@Preview(showBackground = true, name = "配置输入区域")
@Composable
fun PreviewConfigInput() {
    MQTTtestEMQXTheme {
        ConfigInputSection(
            host = "a11331ff.ala.cn-hangzhou.emqxsl.cn",
            port = "8883",
            topic = "test/topic/from/android",
            username = "kukudai",
            password = "123456",
            onHostChange = {},
            onPortChange = {},
            onTopicChange = {},
            onUsernameChange = {},
            onPasswordChange = {}
        )
    }
}

@Preview(showBackground = true, name = "配置显示区域")
@Composable
fun PreviewConfigDisplay() {
    MQTTtestEMQXTheme {
        ConfigDisplaySection(
            host = "a11331ff.ala.cn-hangzhou.emqxsl.cn",
            port = "8883",
            topic = "test/topic/from/android",
            username = "kukudai"
        )
    }
}
