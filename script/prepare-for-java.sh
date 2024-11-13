#!/bin/bash

# System updates
echo "Update the list of available packages and their versions"
sudo apt update
echo "Install the newest versions of all currently installed packages that have available updates"
sudo apt upgrade -y

# Configuration changes
echo "Apply configuration changes to easier interact with electronic components"
sudo raspi-config nonint do_i2c 0
sudo raspi-config nonint do_ssh 0
sudo raspi-config nonint do_serial_hw 0
sudo raspi-config nonint do_serial_cons 1
sudo raspi-config nonint do_onewire 0
sudo systemctl disable hciuart
echo "dtoverlay=disable-bt" | sudo tee -a /boot/firmware/config.txt

# Missing Dependencies
echo "Install missing dependencies"
sudo apt install -y i2c-tools vim git java-common libxi6 libxrender1 libxtst6

# Install Java (SDK with JavaFX) and related tools
echo "Install Java"
wget https://cdn.azul.com/zulu/bin/zulu21.38.21-ca-fx-jdk21.0.5-linux_arm64.deb
sudo dpkg -i zulu21.38.21-ca-fx-jdk21.0.5-linux_arm64.deb
rm zulu21.38.21-ca-fx-jdk21.0.5-linux_arm64.deb
echo "Installed Java version:"
java -version

echo "Install SDKMAN"
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
echo "Installed SDKMAN version:"
sdk version

echo "Install Maven"
sdk install maven
echo "Installed Maven version:"
mvn -v

echo "Install JBang"
sdk install jbang
echo "Installed JBang version:"
jbang --version
