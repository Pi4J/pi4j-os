#!/bin/bash

# Ensure the script is running as sudo
if [ "$EUID" -eq 0 ]; then
  echo "   "
  echo "Please do not run this script with sudo!"
  echo "Sorry, can't continue..."
  echo "   "
  exit
fi

echo "   "
echo "PREPARING FOR JAVA DEVELOPMENT! LET'S START..."
echo "   "
echo "-------------------------"
echo "   "

# System updates
echo "STEP: Update the list of available packages and their versions"
echo "   "
sudo apt update

echo "   "
echo "-------------------------"
echo "   "

echo "STEP: Install the newest versions of all currently installed packages that have available updates"
echo "   "
sudo apt upgrade -y

echo "   "
echo "-------------------------"
echo "   "

# Configuration changes

echo "STEP: Enable SSH and VNC"
echo "   "
sudo raspi-config nonint do_ssh 0
sudo raspi-config nonint do_vnc 0

echo "   "
echo "-------------------------"
echo "   "

echo "STEP: Apply configuration changes to easier interact with electronic components"
echo "   "
sudo raspi-config nonint do_i2c 0
sudo raspi-config nonint do_spi 0
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

echo "STEP: Install missing dependencies"
echo "   "
sudo apt install -y i2c-tools vim git java-common libxi6 libxrender1 libxtst6

echo "   "
echo "-------------------------"
echo "   "

echo "STEP: I2C dependency needed for FFM plugin"
echo "   "
sudo apt install -y libi2c-dev

echo "   "
echo "-------------------------"
echo "   "

# Install Java (SDK with JavaFX) and related tools

echo "STEP: Install Java"
echo "   "
wget https://cdn.azul.com/zulu/bin/zulu25.28.85-ca-fx-jdk25.0.0-linux_arm64.deb
sudo dpkg -i zulu25.28.85-ca-fx-jdk25.0.0-linux_arm64.deb
rm zulu25.28.85-ca-fx-jdk25.0.0-linux_arm64.deb
echo "   "
echo "Installed Java version:"
java -version

echo "   "
echo "-------------------------"
echo "   "

echo "STEP: Install SDKMAN"
echo "   "
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
echo "Installed SDKMAN version:"
sdk version

echo "   "
echo "-------------------------"
echo "   "

echo "STEP: Install Maven"
echo "   "
sdk install maven
echo "   "
echo "Installed Maven version:"
mvn -v

echo "   "
echo "-------------------------"
echo "   "

echo "STEP: Install JBang"
echo "   "
sdk install jbang
echo "   "
echo "Installed JBang version:"
jbang --version

echo "   "
echo "-------------------------"
echo "   "

echo "STEP: Install Visual Studio Code"
echo "   "
sudo apt install -y code
echo "   "
echo "Installed Visual Studio Code, you will need to add the Java extensions within VSC yourself..."
echo "For more information, see https://www.pi4j.com/documentation/development/install-vsc-ide/"

echo "   "
echo "-------------------------"
echo "   "

echo "ALL DONE! HAVE FUN WHEN THE BOARD HAS RESTARTED..."
sudo reboot


