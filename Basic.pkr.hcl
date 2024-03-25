source "arm" "raspios" {
  file_urls = [
    "https://downloads.raspberrypi.org/raspios_arm64/images/raspios_arm64-2024-03-15/2024-03-15-raspios-bookworm-arm64.img.xz"
  ]
  file_checksum_url = "https://downloads.raspberrypi.org/raspios_arm64/images/raspios_arm64-2024-03-15/2024-03-15-raspios-bookworm-arm64.img.xz.sha256"
  file_checksum_type = "sha256"
  file_target_extension = "xz"
  file_unarchive_cmd = ["xz", "--decompress", "$ARCHIVE_PATH"]

  # Image Options
  image_build_method = "resize"
  image_path = "Pi4J-Basic-OS.img"
  image_type = "dos"
  image_size = "8G"

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
    source = "./basic/resources"
    destination = "/tmp/res-basic"
  }

  provisioner "shell" {
    script = "./base/base.sh"
  }

  provisioner "shell" {
    script = "./basic/basic.sh"
  }

  post-processor "compress" {
    output = "Pi4J-Basic-OS.img.zip"
    compression_level = 6
  }

  post-processor "artifice" {
    files = [
      "Pi4J-Basic-OS.img.zip"
    ]
  }

  post-processor "checksum" {
    checksum_types = [
      "sha256"
    ]
    output = "Pi4J-Basic-OS.img.sha256"
  }
}
