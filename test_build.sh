#!/bin/bash

cd /workspaces/MultiStreamViewer

echo "🧪 Testando build local..."

# 1. Verificar estrutura
echo "📁 Estrutura do projeto:"
find . -name "*.png" -o -name "ic_launcher.*" | sort

# 2. Verificar ícones
echo "🎯 Verificando ícones:"
for dir in app/src/main/res/mipmap-*; do
    if [ -f "$dir/ic_launcher.png" ]; then
        echo "✅ $dir/ic_launcher.png"
    else
        echo "❌ $dir/ic_launcher.png (FALTANDO)"
    fi
done

# 3. Testar build
echo "🏗️ Executando build..."
./gradlew clean
./gradlew assembleDebug --stacktrace

# 4. Verificar APK gerado
if [ -f "app/build/outputs/apk/debug/*.apk" ]; then
    echo "✅ BUILD SUCESSO! APK gerado."
    ls -lh app/build/outputs/apk/debug/*.apk
else
    echo "❌ Build falhou. Verifique os logs acima."
fi
