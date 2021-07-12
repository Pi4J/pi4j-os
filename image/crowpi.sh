#!/bin/bash

# Basic configuration
raspi-config nonint do_hostname crowpi

# Change localization options
raspi-config nonint do_change_locale en_US.UTF-8
raspi-config nonint do_configure_keyboard us
raspi-config nonint do_change_timezone Europe/Zurich

# Enable remote management
raspi-config nonint do_ssh 0
raspi-config nonint do_vnc 0

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
  maven \
  openjdk-11-jdk \

# Customize UI settings based on "Set default for large screens" of pipanel
# Logic has been taken from https://github.com/raspberrypi-ui/pipanel/blob/master/src/pipanel.c
# > save_lxsession_settings()
sudo -u pi install -Dm 0644 /tmp/resources/ui/lxde-desktop.conf /home/pi/.config/lxsession/LXDE-pi/desktop.conf
# > save_pcman_g_settings()
sudo -u pi install -Dm 0644 /tmp/resources/ui/pcmanfm-global.conf /home/pi/.config/pcmanfm/LXDE-pi/pcmanfm.conf
# > save_pcman_settings(0)
sudo -u pi install -Dm 0644 /tmp/resources/ui/pcmanfm-desktop.conf /home/pi/.config/pcmanfm/LXDE-pi/desktop-items-0.conf
# > save_pcman_settings(1)
sudo -u pi install -Dm 0644 /tmp/resources/ui/pcmanfm-desktop.conf /home/pi/.config/pcmanfm/LXDE-pi/desktop-items-1.conf
echo "folder=/home/pi/Desktop" >> /home/pi/.config/pcmanfm/LXDE-pi/desktop-items-1.conf
# > save_libfm_settings()
sudo -u pi install -Dm 0644 /tmp/resources/ui/libfm.conf /home/pi/.config/libfm/libfm.conf
# > save_obconf_settings()
sudo -u pi install -Dm 0644 /tmp/resources/ui/openbox-session.xml /home/pi/.config/openbox/lxde-pi-rc.xml
# > save_gtk3_settings() + save_scrollbar_settings()
sudo -u pi install -Dm 0644 /tmp/resources/ui/gtk2-style.rc /home/pi/.gtkrc-2.0
sudo -u pi install -Dm 0644 /tmp/resources/ui/gtk3-style.css /home/pi/.config/gtk-3.0/gtk.css
# > save_lxpanel_settings()
sudo -u pi install -Dm 0644 /etc/xdg/lxpanel/LXDE-pi/panels/panel /home/pi/.config/lxpanel/LXDE-pi/panels/panel
sudo -u pi sed -i 's/iconsize=.*/iconsize=52/g' /home/pi/.config/lxpanel/LXDE-pi/panels/panel
sudo -u pi sed -i 's/height=.*/height=52/g' /home/pi/.config/lxpanel/LXDE-pi/panels/panel
sudo -u pi sed -i 's/MaxTaskWidth=.*/MaxTaskWidth=300/g' /home/pi/.config/lxpanel/LXDE-pi/panels/panel
# > save_qt_settings()
sudo -u pi install -Dm 0644 /tmp/resources/ui/qt5-global.conf /home/pi/.config/qt5ct/qt5ct.conf
# > save_lxterm_settings()
sudo -u pi install -Dm 0644 /tmp/resources/ui/lxterminal.conf /home/pi/.config/lxterminal/lxterminal.conf

# Override system-wide default wallpaper
sed -i 's/wallpaper=.*/wallpaper=\/opt\/fhnw\/wallpaper-static.jpg/g' /etc/xdg/pcmanfm/LXDE-pi/desktop-items-*.conf
sed -i 's/wallpaper_mode=.*/wallpaper_mode=stretch/g' /etc/xdg/pcmanfm/LXDE-pi/desktop-items-*.conf

# Deploy dynamic wallpaper script and resources
sudo -u pi install -Dm 0644 /tmp/resources/wallpaper-autostart.desktop /home/pi/.config/autostart/fhnw-wallpaper.desktop
sudo -u pi install -Dm 0644 /tmp/resources/wallpaper-systemd.service /home/pi/.config/systemd/user/fhnw-wallpaper.service
sudo -u pi install -Dm 0644 /tmp/resources/wallpaper-systemd.path /home/pi/.config/systemd/user/fhnw-wallpaper.path
install -Dm 0755 /tmp/resources/wallpaper-hook.sh /lib/dhcpcd/dhcpcd-hooks/99-fhnw
install -Dm 0644 /tmp/resources/wallpaper-static.jpg /opt/fhnw/wallpaper-static.jpg

# Deploy default WiFi configuration
install -Dm 0644 /tmp/resources/wpa-supplicant.conf /etc/wpa_supplicant/wpa_supplicant.conf

# Disable getting started wizard
rm /etc/xdg/autostart/piwiz.desktop
