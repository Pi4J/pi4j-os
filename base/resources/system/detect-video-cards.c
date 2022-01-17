#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <json.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <xf86drm.h>
#include <xf86drmMode.h>

#define DRI_DEVICE_DIR "/dev/dri/"

#define FREE(ptr)   \
    do              \
    {               \
        free(ptr);  \
        ptr = NULL; \
    } while (0)

struct video_mode
{
    char *name;
    uint32_t width;
    uint32_t height;
    uint32_t refresh;
};

struct video_connector
{
    uint32_t id;
    struct video_mode **modes;
    size_t mode_count;
};

struct video_device
{
    char *path;
    uint32_t max_width;
    uint32_t max_height;
    uint32_t min_width;
    uint32_t min_height;

    struct video_connector **connectors;
    size_t connector_count;
};

static char *concat(const char *s1, const char *s2);
static struct json_object *video_device_json(const struct video_device *dev);
static struct json_object *video_connector_json(const struct video_connector *conn);
static struct json_object *video_mode_json(const struct video_mode *mode);
static struct video_device *video_device_scan(const char *path);
static struct video_connector *video_connector_scan(int fd, uint32_t conn_id);
static struct video_mode *video_mode_scan(const drmModeConnector *drm_conn, size_t mode_idx);
static void video_device_free(struct video_device *dev);
static void video_connector_free(struct video_connector *connector);
static void video_mode_free(struct video_mode *mode);

int main()
{
    // open directory for listing card devices
    DIR *dir = opendir(DRI_DEVICE_DIR);
    if (dir == NULL)
    {
        fprintf(stderr, "could not list contents of %s\n", DRI_DEVICE_DIR);
        return -1;
    }

    // prepare new json array
    struct json_object *dev_arr = json_object_new_array();

    // loop over all directory entries
    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL)
    {
        // skip if entry does not start with "card"
        if (strncmp("card", entry->d_name, strlen("card")) != 0)
            continue;

        // build full path to directory
        char *path = concat(DRI_DEVICE_DIR, entry->d_name);
        if (path == NULL)
        {
            fprintf(stderr, "could not build path to directory entry [%s]\n", entry->d_name);
            return -2;
        }

        // attempt to open as video device
        struct video_device *dev = video_device_scan(path);
        if (dev == NULL)
        {
            fprintf(stderr, "ignoring invalid video device [%s]\n", path);
            FREE(path);
            continue;
        }

        // append video device to json array
        json_object_array_add(dev_arr, video_device_json(dev));

        // free up resources
        video_device_free(dev);
        FREE(path);
    }

    // output device array as json
    const char *dev_arr_str = json_object_to_json_string_ext(dev_arr, JSON_C_TO_STRING_PLAIN);
    puts(dev_arr_str);

    // free up resources
    json_object_put(dev_arr);
    closedir(dir);
}

static char *concat(const char *s1, const char *s2)
{
    const size_t l1 = strlen(s1);
    const size_t l2 = strlen(s2);

    char *output = malloc(l1 + l2 + 1);
    if (output == NULL)
    {
        fprintf(stderr, "could not allocate memory for string concat\n");
        return NULL;
    }

    memcpy(output, s1, l1);
    memcpy(output + l1, s2, l2 + 1);

    return output;
}

static struct json_object *video_device_json(const struct video_device *dev)
{
    struct json_object *conn_arr = json_object_new_array_ext(dev->connector_count);
    for (size_t conn_idx = 0; conn_idx < dev->connector_count; conn_idx++)
    {
        struct video_connector *conn = dev->connectors[conn_idx];
        json_object_array_put_idx(conn_arr, conn_idx, video_connector_json(conn));
    }

    struct json_object *obj = json_object_new_object();
    json_object_object_add(obj, "path", json_object_new_string(dev->path));
    json_object_object_add(obj, "maxWidth", json_object_new_uint64(dev->max_width));
    json_object_object_add(obj, "maxHeight", json_object_new_uint64(dev->max_height));
    json_object_object_add(obj, "minWidth", json_object_new_uint64(dev->min_width));
    json_object_object_add(obj, "minHeight", json_object_new_uint64(dev->min_height));
    json_object_object_add(obj, "connectors", conn_arr);

    return obj;
}

static struct json_object *video_connector_json(const struct video_connector *conn)
{
    struct json_object *mode_arr = json_object_new_array_ext(conn->mode_count);
    for (size_t mode_idx = 0; mode_idx < conn->mode_count; mode_idx++)
    {
        struct video_mode *mode = conn->modes[mode_idx];
        json_object_array_put_idx(mode_arr, mode_idx, video_mode_json(mode));
    }

    struct json_object *obj = json_object_new_object();
    json_object_object_add(obj, "id", json_object_new_uint64(conn->id));
    json_object_object_add(obj, "modes", mode_arr);

    return obj;
}

static struct json_object *video_mode_json(const struct video_mode *mode)
{
    struct json_object *obj = json_object_new_object();
    json_object_object_add(obj, "name", json_object_new_string(mode->name));
    json_object_object_add(obj, "width", json_object_new_uint64(mode->width));
    json_object_object_add(obj, "height", json_object_new_uint64(mode->height));
    json_object_object_add(obj, "refresh", json_object_new_uint64(mode->refresh));

    return obj;
}

static struct video_device *video_device_scan(const char *path)
{
    // attempt to open device
    int fd = open(path, O_RDWR | FD_CLOEXEC);
    if (fd == -1)
    {
        fprintf(stderr, "could not open device [%s]: %s\n", path, strerror(errno));
        return NULL;
    }

    // attempt to query drm resources
    drmModeRes *drm_res = drmModeGetResources(fd);
    if (drm_res == NULL)
    {
        fprintf(stderr, "could not query drm resources for device [%s]\n", path);
        close(fd);
        return NULL;
    }

    // allocate memory for device structure
    struct video_device *dev = calloc(1, sizeof(struct video_device));
    if (dev == NULL)
    {
        fprintf(stderr, "could not allocate memory for device struct\n");
        drmModeFreeResources(drm_res);
        close(fd);
        return NULL;
    }

    // copy basic information
    dev->path = strdup(path);
    dev->max_width = drm_res->max_width;
    dev->max_height = drm_res->max_height;
    dev->min_width = drm_res->min_width;
    dev->min_height = drm_res->min_height;

    // scan all connectors
    dev->connector_count = drm_res->count_connectors;
    dev->connectors = calloc(dev->connector_count, sizeof(struct video_connector *));
    for (size_t conn_idx = 0; conn_idx < dev->connector_count; conn_idx++)
    {
        // determine id of connector and scan it
        uint32_t conn_id = drm_res->connectors[conn_idx];
        dev->connectors[conn_idx] = video_connector_scan(fd, conn_id);

        // ensure connector was successfully scanned
        if (dev->connectors[conn_idx] == NULL)
        {
            fprintf(stderr, "could not scan connector #%d of device [%s]\n", conn_id, path);
            video_device_free(dev);
            drmModeFreeResources(drm_res);
            close(fd);
            return NULL;
        }
    }

    // free resources
    drmModeFreeResources(drm_res);
    close(fd);

    return dev;
}

static struct video_connector *video_connector_scan(int fd, uint32_t conn_id)
{
    // attempt to query drm connector
    drmModeConnector *drm_conn = drmModeGetConnector(fd, conn_id);
    if (drm_conn == NULL)
    {
        fprintf(stderr, "could not query connector #%d with drm using fd #%d\n", conn_id, fd);
        return NULL;
    }

    // allocate memory for connector structure
    struct video_connector *conn = calloc(1, sizeof(struct video_connector));
    if (conn == NULL)
    {
        fprintf(stderr, "could not allocate memory for connector struct\n");
        drmModeFreeConnector(drm_conn);
        return NULL;
    }

    // copy basic information
    conn->id = drm_conn->connector_id;

    // scan all modes
    conn->mode_count = drm_conn->count_modes;
    conn->modes = calloc(conn->mode_count, sizeof(struct video_mode));
    for (size_t mode_idx = 0; mode_idx < conn->mode_count; mode_idx++)
    {
        // scan mode at given index
        conn->modes[mode_idx] = video_mode_scan(drm_conn, mode_idx);

        // ensure mode was successfully scanned
        if (conn->modes[mode_idx] == NULL)
        {
            fprintf(stderr, "could not scan mode #%d of connector #d\n", mode_idx, conn_id);
            video_connector_free(conn);
            drmModeFreeConnector(drm_conn);
            return NULL;
        }
    }

    // free resources
    drmModeFreeConnector(drm_conn);

    return conn;
}

static struct video_mode *video_mode_scan(const drmModeConnector *drm_conn, size_t mode_idx)
{
    // lookup drm mode info
    drmModeModeInfo *drm_mode = &drm_conn->modes[mode_idx];
    if (drm_mode == NULL)
    {
        fprintf(stderr, "unexpected null pointer in drm modes\n");
        return NULL;
    }

    // allocate memory for mode structure
    struct video_mode *mode = calloc(1, sizeof(struct video_mode));
    if (mode == NULL)
    {
        fprintf(stderr, "could not allocate memory for mode struct\n");
        return NULL;
    }

    // copy basic information
    mode->name = strdup(drm_mode->name);
    mode->width = drm_mode->hdisplay;
    mode->height = drm_mode->vdisplay;
    mode->refresh = drm_mode->vrefresh;

    return mode;
}

static void video_device_free(struct video_device *dev)
{
    if (dev == NULL)
        return;

    if (dev->connectors != NULL)
    {
        for (size_t i = 0; i < dev->connector_count; i++)
            video_connector_free(dev->connectors[i]);

        FREE(dev->connectors);
        dev->connector_count = 0;
    }

    FREE(dev->path);
    FREE(dev);
}

static void video_connector_free(struct video_connector *conn)
{
    if (conn == NULL)
        return;

    if (conn->modes != NULL)
    {
        for (size_t i = 0; i < conn->mode_count; i++)
            video_mode_free(conn->modes[i]);

        FREE(conn->modes);
        conn->mode_count = 0;
    }

    FREE(conn);
}

static void video_mode_free(struct video_mode *mode)
{
    if (mode == NULL)
        return;

    FREE(mode->name);
    FREE(mode);
}
