# NoRestart

一个很小的 Bukkit 插件：**禁用 Spigot 内置的 `/restart`**，因为它在 Arclight 上会死锁。

A tiny Bukkit plugin that disables Spigot's built-in `/restart` command because it deadlocks on Arclight.

---

## 问题

Arclight 自带 Spigot 的 `org.spigotmc.RestartCommand`，它在服务端主线程中执行：

```
Server thread
  → RestartCommand.restart()
  → System.exit(0)
  → Shutdown.runHooks()
  → Thread.join(Main$1)      ← 等待关闭钩子
```

而 `Main$1` 这个关闭钩子又在等待 Server thread 结束，于是自己等自己，进程永久卡死。表现是：

1. 执行 `/restart`
2. 日志打印 `Startup script './start.sh' does not exist! Stopping server.`
3. 世界存档正常完成
4. 之后**再也没有输出**，进程不退出（spark 会持续报 `Timed out waiting for world statistics`）

这是 Arclight 的实现问题，与插件、Sable、存档无关；`stop` / `restartnow` 走正常停服流程则不会触发。

相关 issue：[#1148 Server shutdown never actually exits the JVM](https://github.com/IzzelAliz/Arclight/issues/1148)、[#1327 /restart 需要用两次](https://github.com/IzzelAliz/Arclight/issues/1327)。

## 这个插件做什么

- 监听 `PlayerCommandPreprocessEvent` / `ServerCommandEvent`，取消所有 `/restart`（含 `bukkit:restart`、`spigot:restart` 等前缀），并提示玩家改用其他方式
- 启动时从 Bukkit 命令表里注销 `restart`，即使有路径绕过事件也不会真正执行
- 不改动任何其他命令

## 安装

1. 需要 Java 21 的 Spigot/Paper/Arclight 系服务端（在 Arclight 1.0.2 / MC 1.21.1 上测试）
2. 把 `NoRestart-1.0.0.jar` 放进 `plugins/`
3. 重启服务端

启动日志应出现：

```
[NoRestart] Enabling NoRestart v1.0.0
[NoRestart] Unregistered command: /restart
[NoRestart] Blocked /restart; use /restartnow + /restartconfirm.
```

## 构建

需要 JDK 21，以及一个包含 Bukkit API 的服务端 jar（Arclight 的 jar 可以直接用）：

```powershell
.\build.ps1 -ServerJar "D:\path\to\arclight-1.21.1.jar"
```

产物在 `build/NoRestart-1.0.0.jar`。

## 替代方案

如果你只是想安全重启，直接用正常停服流程即可（`stop`、重启脚本或 KubeJS 定时器）。
本插件只负责拦住那个必然卡死的命令。

## License

MIT
