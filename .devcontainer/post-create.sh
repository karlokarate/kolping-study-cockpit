#!/bin/bash
# Post-Create Script für Kolping Study Cockpit Codespace
# Installiert alle Tools die Copilot für perfektes Arbeiten braucht

# Don't exit on error - continue with remaining setup
set +e
echo "🚀 Setting up Kolping Study Cockpit Development Environment..."

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
echo "🐍 Setting up Python virtual environment..."
cd /workspaces/kolping-study-cockpit
if [ ! -d ".venv" ]; then
    python3 -m venv .venv
fi
source .venv/bin/activate
pip install --upgrade pip
pip install -e ".[dev]" || true

# =====================================================
# USEFUL TOOLS
# =====================================================
echo "🛠️ Installing additional tools..."
sudo apt-get update -qq
sudo apt-get install -y -qq \
    tmux \
    htop \
    jq \
    tree \
    ripgrep \
    fd-find \
    bat \
    2>/dev/null || true

# =====================================================
# GIT CONFIGURATION
# =====================================================
echo "📝 Configuring Git..."
git config --global pull.rebase true
git config --global fetch.prune true
git config --global diff.colorMoved zebra

# =====================================================
# FINAL
# =====================================================
echo ""
echo "✅ Development environment setup complete!"
echo ""
echo "Available commands:"
echo "  ./kmp/gradlew :androidApp:assembleDebug  - Build Android app"
echo "  pytest                                    - Run Python tests"
echo "  kolping --help                           - CLI help"
echo ""
