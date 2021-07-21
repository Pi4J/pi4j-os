#!/bin/bash

# Script configuration
declare -gr GLUON_JAVAFX_URL="https://gluonhq.com/download/javafx-17-ea-sdk-linux-arm32/"
declare -gr GLUON_JAVAFX_PATH="/opt/javafx-sdk"
declare -gr GLUON_JAVAFX_VERSION_PATH="/opt/javafx-sdk-17"

# Basic configuration
raspi-config nonint do_hostname crowpi

# Change localization options
raspi-config nonint do_change_locale en_US.UTF-8
raspi-config nonint do_configure_keyboard us
raspi-config nonint do_change_timezone Europe/Zurich

# Enable remote management
raspi-config nonint do_ssh 0
raspi-config nonint do_vnc 0

# Ensure required kernel modules are loaded
truncate -s0 /etc/modprobe.d/raspi-blacklist.conf
grep -qxF 'i2c-dev' || echo 'i2c-dev' >>/etc/modules

# Enable additional interfaces
raspi-config nonint do_i2c 0
raspi-config nonint do_spi 0

# Enable WiFi by default
for file in /var/lib/systemd/rfkill/*:wlan; do
  echo 0 > "${file}"
done

# Change default account passwords
echo 'root:crowpi' | chpasswd
echo 'pi:crowpi' | chpasswd

# Install and upgrade software packages
export DEBIAN_FRONTEND=noninteractive
apt-get -qqy update
apt-get -qqy -o 'Dpkg::Options::=--force-confdef' -o 'Dpkg::Options::=--force-confold' upgrade
apt-get -qqy install \
  git \
  imagemagick \
  lirc \
  maven \
  openjdk-11-jdk
rm -rf /var/lib/apt/lists/*

# Download and extract Gluon JavaFX
wget -O /tmp/gluon-javafx.zip "${GLUON_JAVAFX_URL}"
rm -rf "${GLUON_JAVAFX_VERSION_PATH}"
unzip -d /tmp /tmp/gluon-javafx.zip
rm -f /tmp/gluon-javafx.zip
mv /tmp/javafx-sdk-17 "${GLUON_JAVAFX_VERSION_PATH}"
ln -sf "${GLUON_JAVAFX_VERSION_PATH}" "${GLUON_JAVAFX_PATH}"

# Create symlink to newest libgluon_drm
GLUON_JAVAFX_DRM="$(ls -v "${GLUON_JAVAFX_VERSION_PATH}"/lib/libgluon_drm-*.so | tail -n1)"
if [[ -n "${GLUON_JAVAFX_DRM}" ]]; then
  ln -sf "${GLUON_JAVAFX_DRM}" "${GLUON_JAVAFX_VERSION_PATH}/lib/libgluon_drm.so"
else
  echo "Unable to determine latest version of libgluon_drm"
  exit 1
fi

# Deploy system configuration via /boot/config.txt
install -Dm 0644 /tmp/resources/system/config.txt /boot/config.txt

# Deploy default WiFi configuration
install -Dm 0644 /tmp/resources/system/wpa-supplicant.conf /etc/wpa_supplicant/wpa_supplicant.conf

# Disable getting started wizard
rm /etc/xdg/autostart/piwiz.desktop

# Disable screen blanking by default
mkdir -p /etc/X11/xorg.conf.d/
cp /usr/share/raspi-config/10-blanking.conf /etc/X11/xorg.conf.d/

# Remove default backgrounds
rm /usr/share/rpd-wallpaper/*.jpg

# Override system-wide default wallpaper
sed -i 's/wallpaper=.*/wallpaper=\/opt\/fhnw\/wallpaper-static.jpg/g' /etc/xdg/pcmanfm/LXDE-pi/desktop-items-*.conf
sed -i 's/wallpaper_mode=.*/wallpaper_mode=stretch/g' /etc/xdg/pcmanfm/LXDE-pi/desktop-items-*.conf

# Deploy dynamic wallpaper script and resources
sudo -u pi install -Dm 0644 /tmp/resources/wallpaper/wallpaper-autostart.desktop /home/pi/.config/autostart/fhnw-wallpaper.desktop
sudo -u pi install -Dm 0644 /tmp/resources/wallpaper/wallpaper-systemd.service /home/pi/.config/systemd/user/fhnw-wallpaper.service
sudo -u pi install -Dm 0644 /tmp/resources/wallpaper/wallpaper-systemd.path /home/pi/.config/systemd/user/fhnw-wallpaper.path
install -Dm 0755 /tmp/resources/wallpaper/wallpaper-hook.sh /lib/dhcpcd/dhcpcd-hooks/99-fhnw
install -Dm 0644 /tmp/resources/wallpaper/wallpaper-static.jpg /opt/fhnw/wallpaper-static.jpg

# Deploy java-kiosk helper script for JavaFX apps
sudo install -Dm 0755 /tmp/resources/java/java-kiosk.py /usr/local/bin/java-kiosk
