source "arm" "raspios" {
  # Raspberry Pi OS with Desktop
  file_urls = [
    "https://downloads.raspberrypi.org/raspios_armhf/images/raspios_armhf-2021-05-28/2021-05-07-raspios-buster-armhf.zip"
  ]
  file_checksum_url = "https://downloads.raspberrypi.org/raspios_armhf/images/raspios_armhf-2021-05-28/2021-05-07-raspios-buster-armhf.zip.sha256"
  file_checksum_type = "sha256"
  file_target_extension = "zip"

  # Image Options
  image_build_method = "resize"
  image_path = "picade.img"
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
    "sources.arm.raspios"
  ]

  provisioner "file" {
    source = "./base/resources"
    destination = "/tmp/res-base"
  }

  provisioner "file" {
    source = "./picade/resources"
    destination = "/tmp/res-picade"
  }

  provisioner "shell" {
    script = "./base/base.sh"
  }

  provisioner "shell" {
    script = "./picade/picade.sh"
  }

  post-processor "compress" {
    output = "picade.img.zip"
    compression_level = 6
  }

  post-processor "artifice" {
    files = [
      "picade.img.zip"
    ]
  }

  post-processor "checksum" {
    checksum_types = [
      "sha256"
    ]
    output = "picade.img.sha256"
  }
}
