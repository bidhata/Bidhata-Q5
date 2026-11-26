/*
 * q5_poweron: arm MediaTek's "power on at time" RTC alarm through /dev/alarm.
 *   q5_poweron <unix-seconds> [type]     type defaults to 6 (power on with boot logo)
 *   q5_poweron 0 [type]                  clears it
 * MTK's alarm-dev adds two alarm types after the five AOSP ones: 5 = power on (alarm boot),
 * 6 = power on with logo (normal boot). ANDROID_ALARM_SET(type) = _IOW('a', 2 | type << 4, timespec).
 */
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <time.h>
#include <unistd.h>

#define ALARM_IOW(c, type) _IOW('a', (c) | ((type) << 4), struct timespec)
#define ANDROID_ALARM_SET(type) ALARM_IOW(2, type)
#define ANDROID_ALARM_CLEAR(type) _IO('a', 0 | ((type) << 4))

int main(int argc, char **argv)
{
    if (argc < 2) {
        fprintf(stderr, "usage: %s <unix-seconds|0> [type]\n", argv[0]);
        return 2;
    }
    long when = atol(argv[1]);
    int type = argc > 2 ? atoi(argv[2]) : 6;
    int fd = open("/dev/alarm", O_RDWR);
    if (fd < 0) { perror("open /dev/alarm"); return 1; }
    int r;
    if (when == 0) {
        r = ioctl(fd, ANDROID_ALARM_CLEAR(type));
    } else {
        struct timespec ts = { when, 0 };
        r = ioctl(fd, ANDROID_ALARM_SET(type), &ts);
    }
    if (r < 0) { perror("ioctl"); close(fd); return 1; }
    printf("type %d %s %ld (now %ld)\n", type, when ? "set" : "cleared", when, (long) time(NULL));
    close(fd);
    return 0;
}
