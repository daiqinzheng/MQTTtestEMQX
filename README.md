# MQTT Test EMQX

一个基于 Android 平台的 MQTT 客户端测试应用，使用 Jetpack Compose 和 HiveMQ 客户端库构建。

## 📱 功能特性

- ✅ **可配置的 MQTT 连接参数**
  - 自定义服务器地址
  - 自定义端口号
  - 自定义订阅主题
  - 用户名密码认证

- 🔄 **动态 UI 切换**
  - 未连接时显示配置输入界面
  - 连接后显示当前配置信息
  - 实时连接状态指示

- 💬 **消息收发功能**
  - 发送 MQTT 消息到指定主题
  - 实时接收订阅主题的消息
  - 消息历史记录显示

- 🎨 **现代化 UI 设计**
  - Material Design 3 设计规范
  - Jetpack Compose 声明式 UI
  - 响应式布局设计
  - 多个预览函数支持设计时查看

## 🛠️ 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **MQTT 客户端**: HiveMQ Client (v1.3.3)
- **最低 SDK 版本**: Android 8.0 (API 26)
- **目标 SDK 版本**: Android 14 (API 34)

## 📦 依赖库

```kotlin
// MQTT 客户端
implementation("com.hivemq:hivemq-mqtt-client:1.3.3")

// Jetpack Compose
implementation(platform("androidx.compose:compose-bom:2024.04.01"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")

// Kotlin 协程
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

## 🚀 快速开始

### 前置要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17 或更高版本
- Android SDK 34

### 安装步骤

1. **克隆仓库**
   ```bash
   git clone https://github.com/daiqinzheng/MQTTtestEMQX.git
   cd MQTTtestEMQX
   ```

2. **打开项目**
   - 使用 Android Studio 打开项目
   - 等待 Gradle 同步完成

3. **运行应用**
   - 连接 Android 设备或启动模拟器
   - 点击 Run 按钮 (或按 Shift+F10)

### 编译 APK

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本
./gradlew assembleRelease
```

生成的 APK 位于：`app/build/outputs/apk/`

## 📖 使用说明

### 1. 配置 MQTT 连接

应用启动后，在配置界面输入以下信息：

- **服务器地址**: MQTT Broker 的地址（默认：a11331ff.ala.cn-hangzhou.emqxsl.cn）
- **端口**: MQTT Broker 的端口（默认：8883，SSL/TLS 端口）
- **订阅主题**: 要订阅的 MQTT 主题（默认：test/topic/from/android）
- **用户名**: MQTT 认证用户名（默认：kukudai）
- **密码**: MQTT 认证密码（默认：123456）

### 2. 连接到服务器

1. 填写完配置信息后，点击 **"连接"** 按钮
2. 等待连接状态变为 **"已连接"**（绿色）
3. 配置界面将切换为显示模式，展示当前连接信息

### 3. 发送消息

1. 确保已成功连接到 MQTT Broker
2. 在 **"要发送的消息"** 输入框中输入消息内容
3. 点击 **"发送"** 按钮
4. 消息将发送到配置的主题

### 4. 接收消息

- 应用自动订阅配置的主题
- 接收到的消息会实时显示在 **"接收到的消息"** 列表中
- 消息格式：`收到消息: [消息内容] (来自: [主题名称])`

### 5. 断开连接

- 点击 **"断开"** 按钮
- 连接状态变为 **"已断开"**（红色）
- 界面切换回配置输入模式，可重新配置参数

## 🏗️ 项目结构

```
MQTTtestEMQX/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/mqtttestemqx/
│   │   │   │   └── MainActivity.kt          # 主活动和 MQTT 管理器
│   │   │   ├── res/
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml         # 字符串资源
│   │   │   │   │   └── themes.xml          # 主题配置
│   │   │   │   └── mipmap-*/               # 应用图标
│   │   │   └── AndroidManifest.xml         # 应用清单
│   │   └── test/                            # 单元测试
│   └── build.gradle.kts                     # 应用级构建配置
├── gradle/                                  # Gradle 配置
├── build.gradle.kts                         # 项目级构建配置
└── README.md                                # 项目说明文档
```

## 🎨 UI 预览

应用包含以下可视化预览组件：

- 主界面预览
- 连接状态预览（已连接/连接中/已断开）
- 配置输入区域预览
- 配置显示区域预览
- 发送消息区域预览
- 接收消息列表预览

在 Android Studio 中打开 `MainActivity.kt`，点击右上角的 **"Split"** 或 **"Design"** 按钮即可查看预览。

## 🔐 权限说明

应用需要以下权限：

- `INTERNET` - 网络访问，用于 MQTT 连接
- `WAKE_LOCK` - 保持设备唤醒，维持 MQTT 连接
- `ACCESS_NETWORK_STATE` - 检查网络状态

## 📝 核心类说明

### MqttManager

MQTT 连接管理器，负责：

- 连接和断开 MQTT Broker
- 发布消息到指定主题
- 订阅主题并接收消息
- 管理连接状态和消息流

**主要方法**：

```kotlin
fun connect()                                     // 连接到 MQTT Broker
fun disconnect()                                  // 断开连接
fun publishMessage(message: String)               // 发布消息
fun updateConfig(...)                            // 更新连接配置
fun getConfig(): Map<String, String>             // 获取当前配置
```

**状态枚举**：

```kotlin
enum class ConnectionState {
    DISCONNECTED,    // 已断开
    CONNECTING,      // 连接中
    CONNECTED,       // 已连接
    FAILED           // 连接失败
}
```

## 🐛 故障排查

### 连接失败

1. **检查网络连接**：确保设备已连接到互联网
2. **验证服务器地址和端口**：确认 MQTT Broker 地址和端口正确
3. **检查用户名密码**：确认认证信息正确
4. **查看日志**：在 Logcat 中搜索 `MqttManager_HiveMQ` 标签查看详细错误信息

### 无法接收消息

1. **确认已连接**：检查连接状态是否为"已连接"
2. **验证主题**：确认订阅的主题正确
3. **检查消息来源**：确保有其他客户端向该主题发送消息

### APK 安装失败

1. **允许未知来源**：在设备设置中允许安装来自未知来源的应用
2. **检查签名**：确保使用正确的签名密钥
3. **空间不足**：确保设备有足够的存储空间

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

## 👨‍💻 作者

- GitHub: [@daiqinzheng](https://github.com/daiqinzheng)

## 🔗 相关链接

- [HiveMQ MQTT Client](https://github.com/hivemq/hivemq-mqtt-client)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [MQTT Protocol](https://mqtt.org/)
- [EMQX Broker](https://www.emqx.io/)

## 📊 版本历史

### v1.0.0 (2025-10-05)

- ✨ 初始版本发布
- ✅ 实现可配置的 MQTT 连接
- ✅ 支持消息发送和接收
- ✅ Material Design 3 UI
- ✅ 动态配置界面切换

---

**注意**: 本应用仅用于测试和学习目的。在生产环境中使用时，请确保适当的安全措施和错误处理。

