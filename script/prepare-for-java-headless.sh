#!/bin/bash

# Ensure the script is *NOT* running as sudo
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
sudo apt update || die

echo "   "
echo "-------------------------"
echo "   "

echo "STEP: Install the newest versions of all currently installed packages that have available updates"
echo "   "
sudo apt upgrade -y || die

echo "   "
echo "-------------------------"
echo "   "

# Configuration changes

echo "STEP: Enable SSH and VNC"
echo "   "
sudo raspi-config nonint do_ssh 0 || die

echo "   "
echo "-------------------------"
echo "   "

echo "STEP: Apply configuration changes to easier interact with electronic components"
echo "   "
sudo raspi-config nonint do_i2c 0 || die
sudo raspi-config nonint do_spi 0 || die
sudo raspi-config nonint do_serial_hw 0 || die
sudo raspi-config nonint do_serial_cons 1 || die
sudo raspi-config nonint do_onewire 0 || die
sudo sed -i "$ a\dtoverlay=pwm-2chan" /boot/firmware/config.txt || die
sudo systemctl disable hciuart
sudo echo "dtoverlay=disable-bt" | tee -a /boot/firmware/config.txt

echo "   "
echo "-------------------------"
echo "   "

# Missing dependencies

echo "STEP: Install missing dependencies"
echo "   "
sudo apt install -y i2c-tools vim git java-common libxi6 libxrender1 libxtst6 fontconfig-config  fonts-freefont-ttf  libfontconfig1  libfreetype6 || die

echo "   "
echo "-------------------------"
echo "   "

echo "STEP: I2C dependency needed for FFM plugin"
echo "   "
sudo apt install -y libi2c-dev || die

echo "   "
echo "-------------------------"
echo "   "

# Install Java (SDK with JavaFX) and related tools

echo "STEP: Install Java"
echo "   "
wget https://cdn.azul.com/zulu/bin/zulu25.34.17-ca-jdk25.0.3-linux_arm64.deb || die
sudo dpkg -i zulu25.34.17-ca-jdk25.0.3-linux_arm64.deb || die
rm zulu25.34.17-ca-jdk25.0.3-linux_arm64.deb || die
echo "   "
echo "Installed Java version:"
java -version || die

echo "   "
echo "-------------------------"
echo "   "

echo "STEP: Install SDKMAN"
echo "   "
curl -s "https://get.sdkman.io" | bash || die
source "$HOME/.sdkman/bin/sdkman-init.sh"
echo "Installed SDKMAN version:"
sdk version || die

echo "   "
echo "-------------------------"
echo "   "

echo "STEP: Install Maven"
echo "   "
sdk install maven || die
echo "   "
echo "Installed Maven version:"
mvn -v || die

echo "   "
echo "-------------------------"
echo "   "

echo "STEP: Install JBang"
echo "   "
sdk install jbang || die
echo "   "
echo "Installed JBang version:"
jbang --version || die

echo "   "
echo "-------------------------"
echo "   "

echo "ALL DONE! HAVE FUN WHEN THE BOARD HAS RESTARTED..."
sudo reboot


