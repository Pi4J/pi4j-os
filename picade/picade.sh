#!/bin/bash
set -x

# Script configuration
declare -gr PICADE_GIT_URL="https://github.com/pimoroni/picade-hat.git"
declare -gr PICADE_GIT_REVISION="df02844c0cd773af5b908f47eac5fb1f7f361531"

# Basic configuration
raspi-config nonint do_hostname picade

# Change default account passwords
echo 'root:picade' | chpasswd
echo 'pi:picade' | chpasswd

# Deploy system configuration via /boot/config.txt
install -Dm 0644 /tmp/res-picade/system/config.txt /boot/config.txt

# Clone official picade-hat repository for dependencies
git clone "${PICADE_GIT_URL}" /tmp/picade-hat
pushd /tmp/picade-hat
git reset --hard "${PICADE_GIT_REVISION}"
popd

# Copy udev rules from picade-hat repository
install -Dm 0644 /tmp/picade-hat/etc/udev/rules.d/10-picade.rules /etc/udev/rules.d/10-picade.rules

# Copy ALSA configuration from picade-hat repository
install -Dm 0644 /tmp/picade-hat/etc/asound.conf /etc/asound.conf

# Build device-tree overlay of picade-hat repository
make -C /tmp/picade-hat

# Install previously built device-tree overlay
install -Dm 0644 /tmp/picade-hat/picade.dtbo /boot/overlays/picade.dtbo
