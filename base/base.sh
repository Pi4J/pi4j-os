#!/bin/bash
set -x

# Script configuration
declare -gr GLUON_JAVAFX_URL="https://download2.gluonhq.com/openjfx/17/openjfx-17_linux-arm32_bin-sdk.zip"
declare -gr GLUON_JAVAFX_PATH="/opt/javafx-sdk"
declare -gr GLUON_JAVAFX_VERSION_PATH="/opt/javafx-sdk-17"

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

# Install and upgrade software packages
export DEBIAN_FRONTEND=noninteractive
apt-get -qqy update
apt-get -qqy -o 'Dpkg::Options::=--force-confdef' -o 'Dpkg::Options::=--force-confold' upgrade
apt-get -qqy install \
  git \
  imagemagick \
  libdrm-dev \
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

# Deploy default WiFi configuration
install -Dm 0644 /tmp/res-base/system/wpa-supplicant.conf /etc/wpa_supplicant/wpa_supplicant.conf

# Deploy and enable MIME database regeneration script for first boot
install -Dm 0644 /tmp/res-base/system/pi4j-update-mime-db.service /etc/systemd/system/pi4j-update-mime-db.service
systemctl enable pi4j-update-mime-db.service

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
sudo -u pi install -Dm 0644 /tmp/res-base/wallpaper/wallpaper-autostart.desktop /home/pi/.config/autostart/fhnw-wallpaper.desktop
sudo -u pi install -Dm 0644 /tmp/res-base/wallpaper/wallpaper-systemd.service /home/pi/.config/systemd/user/fhnw-wallpaper.service
sudo -u pi install -Dm 0644 /tmp/res-base/wallpaper/wallpaper-systemd.path /home/pi/.config/systemd/user/fhnw-wallpaper.path
install -Dm 0755 /tmp/res-base/wallpaper/wallpaper-hook.sh /lib/dhcpcd/dhcpcd-hooks/99-fhnw
install -Dm 0644 /tmp/res-base/wallpaper/wallpaper-static.jpg /opt/fhnw/wallpaper-static.jpg

# Deploy java-kiosk helper script for JavaFX apps
sudo install -Dm 0755 /tmp/res-base/java/java-kiosk.py /usr/local/bin/java-kiosk
sudo install -Dm 0755 /tmp/res-base/java/java-last-kiosk.py /usr/local/bin/java-last-kiosk

# Compile and deploy helper executable for detecting primary video card
gcc -I/usr/include/libdrm -ldrm -o /usr/local/bin/detect-primary-card /tmp/res-base/system/detect-primary-card.c

# Deploy music samples
sudo -u pi install -Dm 0644 /tmp/res-base/music/* -t /home/pi/Music/

# Deploy Pi4J libraries
sudo -u pi install -Dm 0644 /tmp/res-base/java-deploy/pom.xml /home/pi/deploy/pom.xml
sudo -u pi mvn -f /home/pi/deploy/pom.xml dependency:copy-dependencies -DoutputDirectory=. -Dhttps.protocols=TLSv1.2

# Deploy minimal Java samples for JavaFX and Pi4J
sudo -u pi cp -r /tmp/res-base/java-examples /home/pi/java-examples
