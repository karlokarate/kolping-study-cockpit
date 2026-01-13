#!/bin/bash
# Post-Create Script für Kolping Study Cockpit Codespace
# Installiert alle Tools die Copilot für perfektes Arbeiten braucht

set -e
echo "🚀 Setting up Kolping Study Cockpit Development Environment..."

# =====================================================
# JAVA & ANDROID SDK
# =====================================================
echo "☕ Configuring Java..."
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
echo "export JAVA_HOME=$JAVA_HOME" >> ~/.bashrc
echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> ~/.bashrc

echo "📱 Setting up Android SDK..."
mkdir -p ~/Android/Sdk/cmdline-tools

# Check if Android SDK already exists
if [ ! -d "$HOME/Android/Sdk/cmdline-tools/latest" ]; then
    cd ~/Android/Sdk/cmdline-tools
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O tools.zip || echo "⚠️ Android SDK download failed (firewall?)"
    if [ -f tools.zip ]; then
        unzip -q tools.zip
        mv cmdline-tools latest
        rm tools.zip
        
        export ANDROID_HOME=~/Android/Sdk
        export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
        
        yes | sdkmanager --licenses 2>/dev/null || true
        sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" || echo "⚠️ SDK components install failed"
    fi
fi

echo "export ANDROID_HOME=~/Android/Sdk" >> ~/.bashrc
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
