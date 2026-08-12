# naamtaan1008 · 原生安卓客户端

珠三角独立音乐场景平台「平和日常 naamtaan1008」的**原生 Kotlin 安卓客户端**。独立于旧的 PWA/TWA 壳子，直接从公开 API 拉取数据本地渲染。

## 现状（最小可用版）

三个主页面 + 各自详情：

| 页面 | 说明 |
|------|------|
| 首页 | 站点简介 + 焦点演出 |
| 演出 | 演出列表（含海报/时间/场地/票价），下拉刷新 |
| 场景 | 六个分栏（乐队/场地/排练房/商店/工作室/民宿） |
| 演出详情 | 海报 + 时间/地点/票价/阵容/购票链接 |
| 场景详情 | 按类型渲染字段（地址/价格/设备/营业时间/联系等） |

其余页面（文章/招募/杂货铺/关于/联系/使用说明）留待二期补全。

## 技术栈

- **Kotlin** 2.0 + **Material 3**（zakka 奶油色 `#FAF3E0` 主题，与前站保持视觉一致）
- **OkHttp** + **kotlinx-serialization** 消费 API
- **Coil** 加载海报/图片
- SDK 34 / minSdk 24，无第三方重型框架

## API

数据来自 `https://naamtaan1008.com/api`（CORS 全开），契约见
`naamtaan1008-blog/docs/api-contract.md` 与 `data/Repository.kt`。

## 构建

```bash
# 本地（需 JDK 17 + Android SDK）
cd android
./gradlew assembleDebug        # 调试 APK（可直接安装）

# CI：推送 v* tag 自动构建
#   - debug + 签名 release APK 作为 artifact
#   - 同时附加到 GitHub Release
```

Release 签名由 CI 生成 keystore（`KEYSTORE_FILE/PASSWORD/ALIAS/KEY_PASSWORD`
环境变量驱动，见 `.github/workflows/build-apk.yml`）。

## 目录结构

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/naamtaan1008/app/
│   │   │   ├── MainActivity.kt          # 底部导航壳
│   │   │   ├── data/                    # API 客户端 + 模型 + 仓库
│   │   │   └── ui/                      # 三个 Fragment + 两个详情 Activity + 适配器
│   │   └── res/                         # 布局 / 主题 / 图标
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradle/wrapper
```
