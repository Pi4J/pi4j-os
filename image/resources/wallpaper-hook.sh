#!/bin/sh

# Configuration variables
BASE_PATH="/opt/fhnw"
LAN_INTERFACE="eth0"
WLAN_INTERFACE="wlan0"
WP_INPUT_FILE="${BASE_PATH}/wallpaper-static.jpg"
WP_OUTPUT_FILE="${BASE_PATH}/wallpaper-dynamic.jpg"

# Skip if reason is ROUTERADVERT (IPv6 RA happen every couple minutes)
if [ "${reason:-}" = "ROUTERADVERT" ]; then
	exit 0
fi

# Skip if this does not affect our monitored interfaces
if [ "${interface:-}" != "${LAN_INTERFACE}" ] && [ "${interface:-}" != "${WLAN_INTERFACE}" ]; then
  exit 0
fi

# Determine IP address based on dhcpcd hook data if possible
case "${reason:-}" in
  # If our interface has just been bound, use the passed IP address
  BOUND)
    if [ "${interface}" = "${LAN_INTERFACE}" ]; then
      lan_address="${new_ip_address:-}"
    elif [ "${interface}" = "${WLAN_INTERFACE}" ]; then
      wlan_address="${new_ip_address:-}"
      wlan_ssid="${ifssid:-}"
    fi
    ;;
  # If our lease expired or interface went down, treat as not connected
  EXPIRE | NOCARRIER)
    if [ "${interface}" = "${LAN_INTERFACE}" ]; then
      lan_address="<not connected>"
    elif [ "${interface}" = "${WLAN_INTERFACE}" ]; then
      wlan_address="${new_ip_address:-}"
    fi
    ;;
esac

# Detect LAN IP address from system if still unknown
if [ -z "${lan_address:-}" ]; then
  lan_address="$(ip -4 a s "${LAN_INTERFACE}" | grep -Eo 'inet [0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' | awk '{print $2}')"
  if [ -z "${lan_address}" ]; then
	  lan_address="<not connected>"
  fi
fi

# Detect WLAN IP address from system if still unknown
if [ -z "${wlan_address:-}" ]; then
  wlan_address="$(ip -4 a s "${WLAN_INTERFACE}" | grep -Eo 'inet [0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' | awk '{print $2}')"
  if [ -z "${wlan_address}" ]; then
	  wlan_address="<not connected>"
  fi
fi

# Detect WLAN SSID from system if still unknown
if [ -z "${wlan_ssid}" ]; then
  wlan_ssid="$(iwgetid -r)"
fi

# Build target string for WLAN state
if [ "${wlan_address}" != "<not connected>" ] && [ -n "${wlan_ssid}" ]; then
  wlan_state="${wlan_address} @ ${wlan_ssid}"
else
  wlan_state="${wlan_address}"
fi

# Generate wallpaper with network info
convert "${WP_INPUT_FILE}" \
	-gravity center \
	-pointsize 80 \
	-fill white \
	-draw "text 0,250 'Ethernet: ${lan_address}'" \
	-draw "text 0,350 'WLAN: ${wlan_state}'" \
	-draw "text 0,450 'Hostname: $(uname -n)'" \
	"${WP_OUTPUT_FILE}.new"

# Atomically replace wallpaper if different from current one
if ! cmp --silent "${WP_OUTPUT_FILE}" "${WP_OUTPUT_FILE}.new"; then
  mv -f "${WP_OUTPUT_FILE}.new" "${WP_OUTPUT_FILE}"
else
  rm -f "${WP_OUTPUT_FILE}.new"
fi
