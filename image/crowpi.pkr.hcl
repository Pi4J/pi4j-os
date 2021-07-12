source "arm" "crowpi" {
  # Raspberry Pi OS with Desktop
  file_urls = [
    "https://downloads.raspberrypi.org/raspios_armhf/images/raspios_armhf-2021-03-25/2021-03-04-raspios-buster-armhf.zip"
  ]
  file_checksum_url = "https://downloads.raspberrypi.org/raspios_armhf/images/raspios_armhf-2021-03-25/2021-03-04-raspios-buster-armhf.zip.sha256"
  file_checksum_type = "sha256"
  file_target_extension = "zip"

  # Image Options
  image_build_method = "resize"
  image_path = "crowpi.img"
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
  qemu_binary_source_path = "/usr/bin/qemu-arm-static"
  qemu_binary_destination_path = "/usr/bin/qemu-arm-static"
}

build {
  sources = [
    "sources.arm.crowpi"
  ]

  provisioner "file" {
    source = "./resources"
    destination = "/tmp/resources"
  }

  provisioner "shell" {
    script = "crowpi.sh"
  }

  post-processor "compress" {
    output = "crowpi.img.zip"
    compression_level = 6
  }

  post-processor "artifice" {
    files = [
      "crowpi.img.zip"
    ]
  }

  post-processor "checksum" {
    checksum_types = [
      "sha256"
    ]
    output = "crowpi.img.zip.sha256"
  }
}
