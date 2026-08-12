#!/bin/bash
# build-release.sh — 一键发布安卓客户端
# 流程: WSL(Windows)编译签名 → ECS 拉取 → HK 生产部署 → 更新下载页 → 验证
# 用法: bash build-release.sh [版本号]   (默认 2.3.1, 需与 android/app/build.gradle.kts versionName 一致)
set -e
VERSION=${1:-2.3.1}
echo "=== [1/4] WSL 编译签名 APK ==="
ssh -p 2222 teacross@127.0.0.1 'export JAVA_HOME=$HOME/tools/jdk; export PATH=$JAVA_HOME/bin:$HOME/tools/gradle/bin:$PATH; export ANDROID_HOME=$HOME/Android ANDROID_SDK_ROOT=$HOME/Android; cd ~/naamtaan1008-app/android; KEYSTORE_FILE=/home/teacross/naamtaan1008-app/android/app/android.keystore KEYSTORE_PASSWORD=android KEYSTORE_ALIAS=naamtaan KEY_PASSWORD=android gradle assembleRelease --no-daemon --no-configuration-cache 2>&1 | tail -3'

echo "=== [2/4] 拉取 APK 到 ECS ==="
scp -P 2222 teacross@127.0.0.1:~/naamtaan1008-app/android/app/build/outputs/apk/release/app-release.apk /tmp/naamtaan1008-v${VERSION}.apk
ls -la /tmp/naamtaan1008-v${VERSION}.apk

echo "=== [3/4] 推送 HK 生产并更新下载页 ==="
scp /tmp/naamtaan1008-v${VERSION}.apk hk-slave:/home/admin/naamtaan1008-blog/public/apk/naamtaan1008-v${VERSION}.apk
ssh hk-slave "sudo chown admin:admin /home/admin/naamtaan1008-blog/public/apk/naamtaan1008-v${VERSION}.apk && sed -i 's|/apk/naamtaan1008-v[0-9.]*\.apk|/apk/naamtaan1008-v${VERSION}.apk|' /home/admin/naamtaan1008-blog/public/download.html && echo '下载页指向:' && grep -o 'naamtaan1008-v[0-9.]*\.apk' /home/admin/naamtaan1008-blog/public/download.html"

echo "=== [4/4] 生产验证 ==="
curl -s -o /dev/null -w "APK 下载 → HTTP %{http_code} (%{size_download} bytes)\n" -L "https://naamtaan1008.com/apk/naamtaan1008-v${VERSION}.apk"
curl -s "https://naamtaan1008.com/download" | grep -o "naamtaan1008-v[0-9.]*\.apk" | head -1
echo "✅ 发布完成: v${VERSION}"
