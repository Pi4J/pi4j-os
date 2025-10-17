#!/bin/bash

echo "Installing wallpaper"

mkdir -p ~/wallpaper
cd ~/wallpaper
wget https://raw.githubusercontent.com/Pi4J/pi4j-os/main/wallpaper/GenerateWallpaperInfoImage.java
wget https://raw.githubusercontent.com/Pi4J/pi4j-os/main/wallpaper/pi4j-logo.png
wget https://raw.githubusercontent.com/Pi4J/pi4j-os/main/wallpaper/99-ip-change-notify
sudo mv 99-ip-change-notify /etc/NetworkManager/dispatcher.d/
sudo chmod +x /etc/NetworkManager/dispatcher.d/99-ip-change-notify
sudo chown root:root /etc/NetworkManager/dispatcher.d/99-ip-change-notify
jbang GenerateWallpaperInfoImage.java

echo "Wallpaper script is ready, make sure to adjust the username in the paths!"
