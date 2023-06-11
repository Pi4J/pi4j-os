#!/bin/bash
set -euxo pipefail
IFS=$'\n\t'

# Basic configuration
raspi-config nonint do_hostname pi4j

# Change default account passwords
echo 'root:pi4j' | chpasswd

# Deploy system configuration via /boot/config.txt
install -Dm 0644 /tmp/res-basic/system/config.txt /boot/config.txt

# Deploy audio configuration
sudo install -Dm 0644 /tmp/res-basic/system/asound.conf /root/.asoundrc