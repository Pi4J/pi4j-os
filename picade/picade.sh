#!/bin/bash
set -x

# Basic configuration
raspi-config nonint do_hostname picade

# Change default account passwords
echo 'root:picade' | chpasswd
echo 'pi:picade' | chpasswd
