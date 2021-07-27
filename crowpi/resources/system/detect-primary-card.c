#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <xf86drm.h>
#include <xf86drmMode.h>

const char *const devices[] = {
    "/dev/dri/card0",
    "/dev/dri/card1",
};

int main() {
	// Loop through list of well-known card devices
	for (size_t i = 0; i < sizeof(devices) / sizeof(devices[0]); i++) {
		const char *const device = devices[i];
		fprintf(stderr, "Probing device for DRM render capabilities: %s\n", device);

		// Attempt to open device file for probing
		int fd = open(device, O_RDWR | O_CLOEXEC);
		if (fd == -1) {
			fprintf(stderr, "> Could not open device: %s\n", strerror(errno));
			continue;
		}

		// Attempt to query DRM resources for device
		// This will fail if the device is unable to provide DRIVER_RENDER
		drmModeRes *res = drmModeGetResources(fd);
		if (res == NULL) {
			fprintf(stderr, "> Card not suitable for DRM rendering\n");
			close(fd);
			continue;
		}

		// We have found a candidate, cleanup and print to stdout
		drmModeFreeResources(res);
		close(fd);
		fprintf(stdout, "%s\n", device);

		// Abort early with success
		return EXIT_SUCCESS;
	}

	// None of the well-known devices work as a DRM renderer
	// Abort with failure
	return EXIT_FAILURE;
}
