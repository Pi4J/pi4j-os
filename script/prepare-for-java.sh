#!/bin/bash

# Ensure the script is running as sudo

if [ "$EUID" -ne 0 ]; then
  echo "   "
  echo "Please run with sudo!"
  echo "Sorry, can't continue..."
  echo "   "
  exit
fi

# Define the non-sudo user (= the currently logged-in user)

NON_SUDO_USER=$(logname)

# System updates

echo "Update the list of available packages and their versions"
apt update

echo "   "
echo "-------------------------"
echo "   "

echo "Install the newest versions of all currently installed packages that have available updates"
apt upgrade -y

echo "   "
echo "-------------------------"
echo "   "

# Configuration changes

echo "Apply configuration changes to easier interact with electronic components"
raspi-config nonint do_i2c 0
raspi-config nonint do_ssh 0
raspi-config nonint do_serial_hw 0
raspi-config nonint do_serial_cons 1
raspi-config nonint do_onewire 0
systemctl disable hciuart
echo "dtoverlay=disable-bt" | tee -a /boot/firmware/config.txt

echo "   "
echo "-------------------------"
echo "   "

# Missing dependencies

echo "Install missing dependencies"
apt install -y i2c-tools vim git java-common libxi6 libxrender1 libxtst6

echo "   "
echo "-------------------------"
echo "   "

# Install Java (SDK with JavaFX) and related tools

echo "Install Java"
wget https://cdn.azul.com/zulu/bin/zulu21.38.21-ca-fx-jdk21.0.5-linux_arm64.deb
dpkg -i zulu21.38.21-ca-fx-jdk21.0.5-linux_arm64.deb
rm zulu21.38.21-ca-fx-jdk21.0.5-linux_arm64.deb
echo "Installed Java version:"
java -version

echo "   "
echo "-------------------------"
echo "   "

echo "Install SDKMAN"
sudo -u "$NON_SUDO_USER" bash -c 'curl -s "https://get.sdkman.io" | bash'
sudo -u "$NON_SUDO_USER" bash -c 'source "$HOME/.sdkman/bin/sdkman-init.sh"'
echo "Installed SDKMAN version:"
sudo -u "$NON_SUDO_USER" bash -c 'sdk version'

echo "   "
echo "-------------------------"
echo "   "

echo "Install Maven"
sudo -u "$NON_SUDO_USER" bash -c 'sdk install maven'
echo "Installed Maven version:"
sudo -u "$NON_SUDO_USER" bash -c 'mvn -v'

echo "   "
echo "-------------------------"
echo "   "

echo "Install JBang"
sudo -u "$NON_SUDO_USER" bash -c 'sdk install jbang'
echo "Installed JBang version:"
sudo -u "$NON_SUDO_USER" bash -c 'jbang --version'

echo "   "
echo "-------------------------"
echo "   "

echo "All done! Have fun..."