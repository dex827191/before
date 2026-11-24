# SimpleXray

<img src="https://raw.githubusercontent.com/lhear/SimpleXray/main/metadata/en-US/images/icon.png" alt="icon" width="150">

[![GitHub Release](https://img.shields.io/github/v/release/lhear/SimpleXray)](https://github.com/lhear/SimpleXray/releases)
[![FDroid Release](https://img.shields.io/f-droid/v/com.simplexray.an.svg)](https://f-droid.org/packages/com.simplexray.an)
[![GitHub License](https://img.shields.io/github/license/lhear/SimpleXray)](LICENSE)
[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/lhear/SimpleXray/.github%2Fworkflows%2Fbuild.yml)](https://github.com/lhear/SimpleXray/actions)
![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/lhear/SimpleXray/total)

SimpleXray is a high-performance proxy client for Android, **built upon the robust Xray-core ([@XTLS/Xray-core](https://github.com/XTLS/Xray-core))**.

It features an **innovative approach**: **directly executing the official Xray-core binary**, unlike traditional JNI methods. This method isolates core logic from the app layer, boosting stability and maximizing Xray-core's native performance. SimpleXray aims to provide a stable and efficient network experience.

## Key Features

*   **Enhanced Stability**: By running Xray-core as an independent child process, SimpleXray avoids JNI complexities, potential memory issues, and app crashes linked to core library failures. This isolation significantly improves reliability.
*   **High Performance**: Leverages Xray-core's native speed and integrates [@heiher/hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel) for efficient Tun2socks, ensuring low latency and high throughput.
*   **User-Friendly**: Offers a clean, intuitive UI and simplified setup, making it easy for users to configure and manage connections.

## Unique Technical Approach

Most Xray-core Android clients use JNI to call a compiled .so library. While easy to integrate, this can cause stability issues like performance overhead, cross-language complexity, and app crashes if the core library fails.

**SimpleXray's core difference is how it starts and manages the proxy:**

On installation/update, the embedded Xray-core binary (as `libxray.so`) is extracted. When connecting, the app uses standard Android APIs to **run this binary as a separate child process**, not via JNI calls. Communication happens via defined Inter-Process Communication (IPC).

This design preserves the original Xray-core binary's stability and performance while physically isolating the core process from the main app, enhancing reliability and security.

## Data Files (`geoip.dat` / `geosite.dat`)

The project **includes a simplified version** with basic rules (`"geoip:private"`, `"geoip:cn"`, `"geosite:gfw"`) from [@lhear/v2ray-rules-dat](https://github.com/lhear/v2ray-rules-dat).

## Quick Start

1.  **Requirement**: Android 10 or higher.
2.  **Get App**: Download the APK from the [Release Page](https://github.com/lhear/SimpleXray/releases) or get it from [F-Droid](https://f-droid.org/packages/com.simplexray.an).
3.  **Install**: Install the APK on your device.
4.  **Configure**: Launch the app, import or manually add server details.
5.  **Connect**: Select a config and tap connect.

## Build Guide (Developers)

1.  **Environment**: Install [Android Studio](https://developer.android.com/studio) and configure the Android SDK.
2.  **Get Code**: Clone the repo and submodules:
    ```bash
    git clone --recursive https://github.com/lhear/SimpleXray
    ```
3.  **Import**: Open the project in Android Studio.
4.  **Integrate Core**: Place the Xray-core binary (`libxray.so`) for your target architecture in `app/src/main/jniLibs/[architecture directory]`. E.g., `app/src/main/jniLibs/arm64-v8a/libxray.so`.
5.  **Add Data Files**: Place `geoip.dat` and `geosite.dat` files into the `app/src/main/assets/` directory. These are required for routing.
6.  **Build**: Sync Gradle and run the build task.

## Contributing

Contributions are welcome! You can help by:
*   Submitting Bug Reports (Issues)
*   Suggesting Features
*   Submitting Code (Pull Requests)
*   Improving Documentation

## License

This project is licensed under the **[Mozilla Public License Version 2.0](LICENSE)**.

## Multi-protocol mode (Xray / SSH)

This fork includes the SSH transport stack in the SimpleXray application. The Profiles tab provides two modes:

- **XRAY** — uses the selected Xray JSON configuration.
- **SSH** — connects the saved SSH profile as a local SOCKS5 upstream and routes Xray egress through it. Xray remains the owner of Android's VPN/TUN interface.

SSH host, port, username, password, payload, optional HTTP proxy, and TLS/SNI settings are managed in the dedicated **Profil** tab, not on the Dashboard. VMess links (`vmess://`) are decoded from Base64 JSON and converted to Xray JSON during import; VLESS links remain supported.

The SSH foreground service runs in the same `:native` process as the Xray `VpnService` so its SSH transport sockets can call `VpnService.protect()` and avoid being captured by the tunnel they provide. Only the existing SimpleXray `TProxyService` owns the active TUN interface.

The VPN service no longer terminates its entire process from `onDestroy()`. It stops the HEV tunnel, closes the TUN descriptor, and lets Android manage the service process lifecycle.

**Build note:** Gradle build verification still needs to run in GitHub Actions or Android Studio; the Gradle wrapper could not download Gradle in this environment because outbound DNS/network access was unavailable.

## Build HEV SOCKS5 native library

The HEV JNI binding is `hev.htproxy.TProxyService`; its package and class name
must match the JNI registration compiled into the HEV native library. The
native source is cloned into `hev-socks5-tunnel/jni` and built with `ndk-build`.

Before building the Android app locally, set `ANDROID_NDK_HOME` (or `NDK_HOME`)
and run:

```bash
mkdir -p hev-socks5-tunnel
cd hev-socks5-tunnel
git clone --recursive https://github.com/heiher/hev-socks5-tunnel jni
cd ..
bash scripts/build-hev-native.sh
./gradlew assembleDebug
```

The helper script performs the clone step when `hev-socks5-tunnel/jni` is not
present, runs `ndk-build` for `arm64-v8a`, then renames the upstream output
`libhev-socks5-tunnel.so` to `libsockstunnel.so` in
`app/src/main/jniLibs/arm64-v8a`.

## Android ABI support

This build is configured for **arm64-v8a only**. The release workflow builds Xray-core and HEV SOCKS5 native libraries for ARM64 and packages them into the release APK. APK-content validation is intentionally not part of the workflow.

The HEV NDK build writes to an isolated temporary output directory and copies the HEV output into `jniLibs` as `libsockstunnel.so`; this prevents it from overwriting the separately built `libxray.so`. The build script removes any stale `libsshprotocol.so` from native library inputs.
