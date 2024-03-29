#!/bin/bash
set -euxo pipefail
IFS=$'\n\t'

# Basic configuration
raspi-config nonint do_hostname crowpi

# Change default account passwords
echo 'root:pi4j' | chpasswd
echo 'pi:pi4j' | chpasswd

# Deploy system configuration via /boot/config.txt
install -Dm 0644 /tmp/res-crowpi/system/config.txt /boot/config.txt

# Deploy audio configuration
sudo install -Dm 0644 /tmp/res-crowpi/system/asound.conf /root/.asoundrc
