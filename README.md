# Pi4J V2 :: Tools to prepare Raspberry Pi OS for Java

[![Contributors](https://img.shields.io/github/contributors/Pi4J/pi4j-os)](https://github.com/Pi4J/pi4j-os/graphs/contributors)
[![License](https://img.shields.io/github/license/Pi4J/pi4j-os)](https://github.com/Pi4J/pi4j-os/blob/main/LICENSE)

This project provides some scripts to extend the official Raspberry Pi Operating System with additional tools to make it the perfect match for Java projects with Pi4J as described on [the Pi4J website > Prepare a Raspberry Pi](https://www.pi4j.com/prepare/).

## Hardware Configurations and Java Tools

1. Prepare an SD Card with 
   * For Raspberry Pi: The **64-bit version of the Raspberry Pi Operating System**, see [the Pi4J website > Prepare a Raspberry Pi > Write OS to SD card](https://www.pi4j.com/prepare/sd-card/).
   * For other type of 
2. Put the SD Card in your Raspberry Pi and start it.
3. The board will probably reboot to do some basic settings.
4. Make sure the board is connected to WiFi or cabled internet.
5. Open the terminal and run this command:
    ```shell
    curl -sL https://raw.githubusercontent.com/Pi4J/pi4j-os/main/script/prepare-for-java.sh | bash
    ```
   Or, if you are trying this out on a non-Raspberry-Pi-board:
    ```shell
    curl -sL https://raw.githubusercontent.com/Pi4J/pi4j-os/main/script/prepare-for-java-non-rpi.sh | bash
    ```
6. Make sure you see `All done! Have fun...` if the script finished. If not, you may need to run it again as one of the intermediate steps has stopped it.
7. You're done! Check [the Pi4J website > Getting Started With Pi4J](https://www.pi4j.com/getting-started/) for the next steps. Have fun with #JavaOnRaspberryPi.

## Wallpaper with System Information

An additional Java (JBang) script is available to turn the desktop wallpaper into an information screen. This script will take an image as input, overlay some useful info as text (IP, Java version, etc.), and save this as a new image. This generated image is then pushed as the new wallpaper to the desktop.

To install the script, open the terminal and run this command:
```shell
curl -sL https://raw.githubusercontent.com/Pi4J/pi4j-os/main/script/install-wallpaper.sh | bash
```

This will generate a result like this:

![Screenshot of a generated wallpaper](screenshot/generated-wallpaper.png)

Script tests:

```shell
# Simulate interface up event for eth0
sudo /etc/NetworkManager/dispatcher.d/99-ip-change-notify eth0 up

# Simulate interface up event for wlan0 (WiFi)
sudo /etc/NetworkManager/dispatcher.d/99-ip-change-notify wlan0 up

# Check the logs immediately after
sudo tail /var/log/ip-change-notify.log
```

## IO Checks

This repository contains a tool to check the status of the Raspberry Pi's IO configurations. It can be used to check if the IO configurations are correct for your project.

For each IO type, one or more checks are performed. You will get a result like this, indicating if the check passed or failed, with more info about the expected and found result:

```text
Results from PWM Detection
  Configuration check for PWM in config.txt
    Status: PASS
	Expected: 
	  dtoverlay=pwm (or dtoverlay=pwm-2chan for 2-channel PWM)
	Result: 
	  Found in /boot/firmware/config.txt: dtoverlay=pwm-2chan
```

1. Make sure JBang is installed on your Raspberry Pi. 
2. You don't need to checkout the code, as JBang will download the script automatically from GitHub. 
3. To execute all checks, run:
  ```shell
  jbang https://github.com/pi4j/pi4j-os/blob/main/iochecks/IOChecker.java
  ```
4. Individual checks can be executed by passing one or more check names as argument:
    * gpio
    * pwm
    * i2c
    * spi
    * serial
5. For example:
    * Only PWM:
    ```shell
    jbang https://github.com/pi4j/pi4j-os/blob/main/iochecks/IOChecker.java pwm
    ```
    * I2C and SPI:
    ```shell
    jbang https://github.com/pi4j/pi4j-os/blob/main/iochecks/IOChecker.java i2c spi
    ```

## History Of This Repository

The original goal of this repository was to provide a build of the official Raspberry Pi OS with additional tools to prepare it for Java(FX) and Pi4J projects. Because it became difficult to support because of changes in the OS for the Raspberry Pi 5, we decided to stop this goal, and provide some scripts here that can help you to achieve the same result. You can still find the latest sources of the Pi4J OS here with the tag [end-of-os](https://github.com/Pi4J/pi4j-os/releases/tag/end-of-os).

## License

This repository is licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and
limitations under the License.
