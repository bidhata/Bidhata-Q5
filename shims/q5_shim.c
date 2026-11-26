/*
 * Bidhata-Q5 shim: symbols the j5052 (alps 5.0) blobs expect from MediaTek's modified framework
 * that cm-12.1 does not provide. Loaded globally via TARGET_LDPRELOAD.
 * Found by checking the blobs for symbols the 6.0 libraries no longer export.
 */
#include <stdarg.h>
#include <stddef.h>
#include <android/log.h>

#define EXPORT __attribute__((visibility("default")))

/* MediaTek xlog: rec = { tag, fmt, prio } */
struct xlog_record { const char *tag_str; const char *fmt_str; int prio; };
EXPORT int __xlog_buf_printf(int bufid, const struct xlog_record *rec, ...)
{
    va_list ap;
    int ret;
    (void)bufid;
    if (!rec || !rec->fmt_str) return 0;
    va_start(ap, rec);
    ret = __android_log_vprint(rec->prio, rec->tag_str ? rec->tag_str : "xlog", rec->fmt_str, ap);
    va_end(ap);
    return ret;
}

/* libnetutils (MTK): used by bin/thermal for modem throttling; no modem here. */
EXPORT int ifc_set_throttle(const char *ifname, int rxKbps, int txKbps)
{
    (void)ifname; (void)rxKbps; (void)txKbps;
    return 0;
}

/* android::AudioSystem voice-unlock extensions (MediaTek). Feature unused: report failure/empty. */
EXPORT int ReadRefFromRing(void *a, unsigned b, void *c) __asm__("_ZN7android11AudioSystem15ReadRefFromRingEPvjS1_");
int ReadRefFromRing(void *a, unsigned b, void *c) { (void)a; (void)b; (void)c; return 0; }
EXPORT int SetVoiceUnlockSRC(unsigned a, unsigned b) __asm__("_ZN7android11AudioSystem17SetVoiceUnlockSRCEjj");
int SetVoiceUnlockSRC(unsigned a, unsigned b) { (void)a; (void)b; return 0; }
EXPORT int stopVoiceUnlockDL(void) __asm__("_ZN7android11AudioSystem17stopVoiceUnlockDLEv");
int stopVoiceUnlockDL(void) { return 0; }
EXPORT int startVoiceUnlockDL(void) __asm__("_ZN7android11AudioSystem18startVoiceUnlockDLEv");
int startVoiceUnlockDL(void) { return 0; }
EXPORT int GetVoiceUnlockULTime(void *a) __asm__("_ZN7android11AudioSystem20GetVoiceUnlockULTimeEPv");
int GetVoiceUnlockULTime(void *a) { (void)a; return 0; }
EXPORT int GetVoiceUnlockDLLatency(void) __asm__("_ZN7android11AudioSystem23GetVoiceUnlockDLLatencyEv");
int GetVoiceUnlockDLLatency(void) { return 0; }
EXPORT int getVoiceUnlockDLInstance(void) __asm__("_ZN7android11AudioSystem24getVoiceUnlockDLInstanceEv");
int getVoiceUnlockDLInstance(void) { return 0; }
EXPORT int freeVoiceUnlockDLInstance(void) __asm__("_ZN7android11AudioSystem25freeVoiceUnlockDLInstanceEv");
int freeVoiceUnlockDLInstance(void) { return 0; }

/*
 * static sp<IFoo> IFoo::asInterface(const sp<IBinder>&)
 * sp<> is non-trivial, so ARM EABI returns it via a hidden pointer in r0; the argument is in r1.
 * Returning a null sp<> = writing a NULL pointer into the result slot.
 */
EXPORT void IATVCtrlService_asInterface(void **ret, const void *binder) __asm__("_ZN7android15IATVCtrlService11asInterfaceERKNS_2spINS_7IBinderEEE");
void IATVCtrlService_asInterface(void **ret, const void *binder) { (void)binder; *ret = NULL; }
EXPORT void IDumpTunnel_asInterface(void **ret, const void *binder) __asm__("_ZN7android11IDumpTunnel11asInterfaceERKNS_2spINS_7IBinderEEE");
void IDumpTunnel_asInterface(void **ret, const void *binder) { (void)binder; *ret = NULL; }

/* cm-13.0: GraphicBufferMapper::lock() takes uint32_t usage now; the MTK libui_ext blob still
 * calls the old int-usage symbol. Same calling convention, so just forward to the new one.
 * Looked up lazily so this preloaded shim does not drag libui into every process. */
#include <dlfcn.h>
typedef int (*gbm_lock_fn)(void *self, const void *handle, unsigned usage, const void *rect, void **vaddr);
EXPORT int GraphicBufferMapper_lock_int(void *self, const void *handle, int usage, const void *rect, void **vaddr)
    __asm__("_ZN7android19GraphicBufferMapper4lockEPK13native_handleiRKNS_4RectEPPv");
int GraphicBufferMapper_lock_int(void *self, const void *handle, int usage, const void *rect, void **vaddr)
{
    static gbm_lock_fn real;
    if (!real)
        real = (gbm_lock_fn)dlsym(RTLD_DEFAULT, "_ZN7android19GraphicBufferMapper4lockEPK13native_handlejRKNS_4RectEPPv");
    return real ? real(self, handle, (unsigned)usage, rect, vaddr) : -1;
}
