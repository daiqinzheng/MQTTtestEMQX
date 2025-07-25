package com.example.mqtttestemqx

import android.content.Context
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mqtttestemqx.ui.theme.MQTTtestEMQXTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage

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
 * MqttManager 类用于封装所有 MQTT 相关的操作
 */
class MqttManager(private val context: Context) {
    // --- MQTT 连接信息 ---
    private val serverUri = "ssl://tf01696e.ala.cn-hangzhou.emqxsl.cn:8883"
    private val clientId = "Android_Client_${System.currentTimeMillis()}"
    private val topic = "test/topic/from/android"

    // =================================================================
    //  重要：请在这里填入您在 EMQX Cloud 控制台获取的用户名和密码
    // =================================================================
    private val username = "kukudai"
    private val password = "123456"
    // =================================================================

    // --- 状态管理 ---
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _receivedMessages = MutableStateFlow<List<String>>(emptyList())
    val receivedMessages = _receivedMessages.asStateFlow()

    private lateinit var mqttClient: MqttAndroidClient

    companion object {
        private const val TAG = "MqttManager"
    }

    // 定义连接状态的枚举
    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, FAILED
    }

    init {
        setupMqttClient()
    }

    /**
     * 初始化 MQTT 客户端并设置回调
     */
    private fun setupMqttClient() {
        mqttClient = MqttAndroidClient(context, serverUri, clientId)
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                _connectionState.value = ConnectionState.CONNECTED
                Log.d(TAG, "连接成功! Server: $serverURI, Reconnect: $reconnect")
                // 连接成功后订阅主题
                subscribeToTopic()
            }

            override fun connectionLost(cause: Throwable?) {
                _connectionState.value = ConnectionState.FAILED
                Log.e(TAG, "连接丢失: ${cause?.message}", cause)
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val msg = "收到消息: ${message.toString()} (来自: $topic)"
                Log.d(TAG, msg)
                val currentMessages = _receivedMessages.value.toMutableList()
                currentMessages.add(0, msg) // 在列表顶部插入新消息
                _receivedMessages.value = currentMessages
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(TAG, "消息发送成功!")
            }
        })
    }

    /**
     * 连接到 MQTT Broker
     */
    fun connect() {
        if (mqttClient.isConnected) {
            Log.w(TAG, "客户端已经连接，无需重复操作")
            return
        }
        _connectionState.value = ConnectionState.CONNECTING

        val connectOptions = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = true
            // --- 添加用户名和密码 ---
            this.userName = this@MqttManager.username
            this.password = this@MqttManager.password.toCharArray()
        }

        try {
            mqttClient.connect(connectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    // 连接成功后的逻辑在 MqttCallbackExtended.connectComplete 中处理
                    Log.d(TAG, "connect() 调用成功，等待回调...")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    _connectionState.value = ConnectionState.FAILED
                    Log.e(TAG, "连接失败: ${exception?.message}", exception)
                }
            })
        } catch (e: MqttException) {
            _connectionState.value = ConnectionState.FAILED
            Log.e(TAG, "连接时发生异常: ${e.message}", e)
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        if (!mqttClient.isConnected) {
            Log.w(TAG, "客户端已经断开，无需重复操作")
            return
        }
        try {
            mqttClient.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    Log.d(TAG, "成功断开连接")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "断开连接失败: ${exception?.message}", exception)
                    // 即使失败，也更新状态为断开
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "断开连接时发生异常: ${e.message}", e)
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    /**
     * 发布消息
     */
    fun publishMessage(message: String) {
        if (!mqttClient.isConnected) {
            Log.e(TAG, "无法发布消息，客户端未连接")
            return
        }
        try {
            val mqttMessage = MqttMessage(message.toByteArray())
            mqttClient.publish(topic, mqttMessage, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    // 消息成功放入发送队列，等待 deliveryComplete 回调
                    Log.d(TAG, "消息 '$message' 已发布到主题 '$topic'")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "发布消息失败: ${exception?.message}", exception)
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "发布消息时发生异常: ${e.message}", e)
        }
    }

    /**
     * 订阅主题
     */
    private fun subscribeToTopic() {
        try {
            mqttClient.subscribe(topic, 1, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "成功订阅主题: $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "订阅主题失败: ${exception?.message}", exception)
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "订阅主题时发生异常: ${e.message}", e)
        }
    }
}


/**
 * 主界面 Composable
 */
@Composable
fun MqttDemoScreen() {
    // 获取当前上下文
    val context = LocalContext.current
    // 使用 remember 创建并保留 MqttManager 实例
    val mqttManager = remember { MqttManager(context) }

    // 收集状态
    val connectionState by mqttManager.connectionState.collectAsState()
    val receivedMessages by mqttManager.receivedMessages.collectAsState()

    // UI 状态
    var messageText by remember { mutableStateOf("") }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Text("MQTT Demo (EMQX)", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // 连接状态
            ConnectionStatus(state = connectionState)
            Spacer(modifier = Modifier.height(16.dp))

            // 连接/断开控制
            ConnectionControls(
                state = connectionState,
                onConnect = { mqttManager.connect() },
                onDisconnect = { mqttManager.disconnect() }
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 消息发布区域
            PublishSection(
                message = messageText,
                onMessageChange = { messageText = it },
                onPublish = {
                    if (messageText.isNotBlank()) {
                        mqttManager.publishMessage(messageText)
                        messageText = "" // 清空输入框
                    }
                },
                isEnabled = connectionState == MqttManager.ConnectionState.CONNECTED
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 接收到的消息列表
            ReceivedMessages(messages = receivedMessages)
        }
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
            enabled = state == MqttManager.ConnectionState.DISCONNECTED || state == MqttManager.ConnectionState.FAILED
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


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MQTTtestEMQXTheme {
        MqttDemoScreen()
    }
}
