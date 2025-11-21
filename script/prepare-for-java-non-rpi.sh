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

echo "   "
echo "-------------------------"
echo "   "

# Install SDKMAN and use it to install Java (SDK with JavaFX) and related tools

echo "STEP: Install SDKMAN"
echo "   "
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
echo "Installed SDKMAN version:"
sdk version

echo "   "
echo "-------------------------"
echo "   "

echo "STEP: Install Java"
echo "   "
sdk install java 25.fx-zulu
echo "   "
echo "Installed Java version:"
java -version

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


