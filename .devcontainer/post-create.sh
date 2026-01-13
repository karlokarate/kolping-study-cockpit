#!/bin/bash
# Post-Create Script für Kolping Study Cockpit Codespace
# Installiert alle Tools die Copilot für perfektes Arbeiten braucht

# Don't exit on error - continue with remaining setup
set +e
echo "🚀 Setting up Kolping Study Cockpit Development Environment..."

# =====================================================
# PATH SETUP (fix pip script warnings)
# =====================================================
export PATH="$HOME/.local/bin:$PATH"
echo "export PATH=\"\$HOME/.local/bin:\$PATH\"" >> ~/.bashrc
echo "✅ PATH includes ~/.local/bin"

# =====================================================
# JAVA (should be installed by devcontainer feature)
# =====================================================
echo "☕ Configuring Java..."
if [ -d "/usr/lib/jvm/msopenjdk-17" ]; then
    export JAVA_HOME=/usr/lib/jvm/msopenjdk-17
elif [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
else
    echo "⚠️ Java 17 not found, attempting to locate..."
    JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) 2>/dev/null || true
fi

if [ -n "$JAVA_HOME" ]; then
    echo "export JAVA_HOME=$JAVA_HOME" >> ~/.bashrc
    echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> ~/.bashrc
    echo "✅ Java configured: $JAVA_HOME"
else
    echo "⚠️ Java not available - Kotlin builds may fail"
fi

# =====================================================
# ANDROID SDK (manual installation - more reliable)
# =====================================================
echo "📱 Setting up Android SDK..."
export ANDROID_HOME=$HOME/Android/Sdk
mkdir -p "$ANDROID_HOME/cmdline-tools"

if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
    cd "$ANDROID_HOME/cmdline-tools"
    
    # Download command line tools
    CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    echo "Downloading Android SDK command line tools..."
    
    if curl -fsSL "$CMDLINE_TOOLS_URL" -o tools.zip 2>/dev/null || wget -q "$CMDLINE_TOOLS_URL" -O tools.zip 2>/dev/null; then
        unzip -q tools.zip
        mv cmdline-tools latest
        rm -f tools.zip
        
        export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
        
        # Accept licenses and install components
        echo "Installing Android SDK components..."
        yes | sdkmanager --licenses 2>/dev/null || true
        sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" 2>/dev/null || echo "⚠️ SDK components install incomplete"
        
        echo "✅ Android SDK installed"
    else
        echo "⚠️ Android SDK download failed (network/firewall issue)"
    fi
else
    echo "✅ Android SDK already installed"
fi

echo "export ANDROID_HOME=$ANDROID_HOME" >> ~/.bashrc
echo "export PATH=\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH" >> ~/.bashrc

# =====================================================
# GRADLE
# =====================================================
echo "🐘 Setting up Gradle..."
if [ -f "/workspaces/kolping-study-cockpit/kmp/gradlew" ]; then
    chmod +x /workspaces/kolping-study-cockpit/kmp/gradlew
fi

# Create local.properties for Android SDK path
echo "sdk.dir=$HOME/Android/Sdk" > /workspaces/kolping-study-cockpit/kmp/local.properties

# =====================================================
# PYTHON ENVIRONMENT
# =====================================================
echo "🐍 Python environment..."
if [ -d "/workspaces/kolping-study-cockpit/.venv" ]; then
    echo "✅ Virtual environment exists"
else
    python3 -m venv /workspaces/kolping-study-cockpit/.venv
    echo "✅ Virtual environment created"
fi

# =====================================================
# SUMMARY
# =====================================================
echo ""
echo "========================================="
echo "✅ Setup complete!"
echo "========================================="
echo "JAVA_HOME: ${JAVA_HOME:-not set}"
echo "ANDROID_HOME: $ANDROID_HOME"
echo "Python: $(python3 --version 2>/dev/null || echo 'not found')"
echo ""
echo "Next steps:"
echo "  1. Source bashrc: source ~/.bashrc"
echo "  2. Build Android: cd kmp && ./gradlew :androidApp:assembleDebug"
echo "========================================="
