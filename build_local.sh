#!/bin/bash

cd /workspaces/MultiStreamViewer

echo "🔧 Configurando Java 17..."
# Forçar Java 17
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 2>/dev/null || export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH

echo "📊 Java version:"
java -version

echo "🧹 Limpando build anterior..."
./gradlew clean

echo "🏗️ Construindo APK..."
./gradlew assembleDebug --stacktrace

if [ -f "app/build/outputs/apk/debug/*.apk" ]; then
    echo "✅ BUILD SUCESSO!"
    ls -lh app/build/outputs/apk/debug/*.apk
else
    echo "❌ Build falhou. Últimos logs:"
    tail -50 app/build/outputs/logs/*.log 2>/dev/null || echo "Nenhum log encontrado"
fi
