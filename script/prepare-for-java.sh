#!/bin/bash

# Ensure the script is running as sudo

if [ "$EUID" -eq 0 ]; then
  echo "   "
  echo "Please do not run this script with sudo!"
  echo "Sorry, can't continue..."
  echo "   "
  exit
fi

# System updates

echo "Update the list of available packages and their versions"
sudo apt update

echo "   "
echo "-------------------------"
echo "   "

echo "Install the newest versions of all currently installed packages that have available updates"
sudo apt upgrade -y

echo "   "
echo "-------------------------"
echo "   "

# Configuration changes

echo "Apply configuration changes to easier interact with electronic components"
sudo raspi-config nonint do_i2c 0
sudo raspi-config nonint do_ssh 0
sudo raspi-config nonint do_serial_hw 0
sudo raspi-config nonint do_serial_cons 1
sudo raspi-config nonint do_onewire 0
sudo sed -i "$ a\dtoverlay=pwm-2chan" /boot/firmware/config.txt
sudo systemctl disable hciuart
sudo echo "dtoverlay=disable-bt" | tee -a /boot/firmware/config.txt

echo "   "
echo "-------------------------"
echo "   "

# Missing dependencies

echo "Install missing dependencies"
sudo apt install -y i2c-tools vim git java-common libxi6 libxrender1 libxtst6

echo "   "
echo "-------------------------"
echo "   "

# Install Java (SDK with JavaFX) and related tools

echo "Install Java"
wget https://cdn.azul.com/zulu/bin/zulu21.38.21-ca-fx-jdk21.0.5-linux_arm64.deb
sudo dpkg -i zulu21.38.21-ca-fx-jdk21.0.5-linux_arm64.deb
rm zulu21.38.21-ca-fx-jdk21.0.5-linux_arm64.deb
echo "Installed Java version:"
java -version

echo "   "
echo "-------------------------"
echo "   "

echo "Install SDKMAN"
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
echo "Installed SDKMAN version:"
sdk version

echo "   "
echo "-------------------------"
echo "   "

echo "Install Maven"
sdk install maven
echo "Installed Maven version:"
mvn -v

echo "   "
echo "-------------------------"
echo "   "

echo "Install JBang"
sdk install jbang
echo "Installed JBang version:"
jbang --version

echo "   "
echo "-------------------------"
echo "   "

echo "All done! Have fun when the board is restarted..."
sudo reboot


