source "arm" "raspios" {
  # Raspberry Pi OS with Desktop
  #  file_urls = [
  #    "https://downloads.raspberrypi.org/raspios_arm64/images/raspios_arm64-2022-01-28/2022-01-28-raspios-bullseye-arm64.zip"
  #  ]
  #  file_checksum_url = "https://downloads.raspberrypi.org/raspios_arm64/images/raspios_arm64-2022-01-28/2022-01-28-raspios-bullseye-arm64.zip.sha256"
  #  file_checksum_type = "sha256"
  #  file_target_extension = "zip"


  file_urls = [
    "https://downloads.raspberrypi.org/raspios_arm64/images/raspios_arm64-2023-05-03/2023-05-03-raspios-bullseye-arm64.img.xz"
  ]
  file_checksum_url = "https://downloads.raspberrypi.org/raspios_arm64/images/raspios_arm64-2023-05-03/2023-05-03-raspios-bullseye-arm64.img.xz.sha256"
  file_checksum_type = "sha256"
  file_target_extension = "xz"
  file_unarchive_cmd = ["xz", "--decompress", "$ARCHIVE_PATH"]

  # Image Options
  image_build_method = "resize"
  image_path = "Pi4J-CrowPi-OS.img"
  image_type = "dos"
  image_size = "6G"

  # Boot Partition
  image_partitions {
    name = "boot"
    type = "c"
    start_sector = 8192
    filesystem = "vfat"
    size = "256M"
    mountpoint = "/boot"
  }

  # System Partition
  image_partitions {
    name = "root"
    type = "83"
    start_sector = 532480
    filesystem = "ext4"
    size = "0"
    mountpoint = "/"
  }

  # QEMU Toolchain
  qemu_binary_source_path = "/usr/bin/qemu-aarch64-static"
  qemu_binary_destination_path = "/usr/bin/qemu-aarch64-static"
}

build {
  sources = [
    "sources.arm.raspios"
  ]

  provisioner "file" {
    source = "./base/resources"
    destination = "/tmp/res-base"
  }

  provisioner "file" {
    source = "./crowpi/resources"
    destination = "/tmp/res-crowpi"
  }

  provisioner "shell" {
    script = "./base/base.sh"
  }

  provisioner "shell" {
    script = "./crowpi/crowpi.sh"
  }

  post-processor "compress" {
    output = "Pi4J-CrowPi-OS.img.zip"
    compression_level = 6
  }

  post-processor "artifice" {
    files = [
      "Pi4J-CrowPi-OS.img.zip"
    ]
  }

  post-processor "checksum" {
    checksum_types = [
      "sha256"
    ]
    output = "Pi4J-CrowPi-OS.img.sha256"
  }
}
