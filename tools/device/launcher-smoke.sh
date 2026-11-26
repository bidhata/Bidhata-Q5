#!/system/bin/sh
# Bidhata launcher smoke test. Runs ON the player (adb root shell), touches nothing permanent.
#   adb push tools/device/launcher-smoke.sh /data/local/tmp/ && adb shell sh /data/local/tmp/launcher-smoke.sh
# Prints one PASS/FAIL line per check and a summary; exit code = number of failures.
# Screen size assumed 480x800 (the Q5). Unlock gesture is a swipe up.

P=org.bidhata.launcher
fails=0
pass() { echo "PASS  $1"; }
fail() { echo "FAIL  $1"; fails=$((fails + 1)); }
focus() { dumpsys window | grep mCurrentFocus; }
unlock() { input keyevent 224; sleep 1; input swipe 240 700 240 200 300; sleep 2; }

logcat -c
logcat -b crash -c 2>/dev/null
svc power stayon true
unlock
input keyevent 3; sleep 2

# 1. home is up after unlocking
focus | grep -q HomeActivity && pass "home after unlock" || fail "home after unlock ($(focus))"

# 2. every launcher page opens and comes back
for a in AppsActivity SoundActivity PowerActivity ModulesActivity; do
    am start -n $P/.$a >/dev/null; sleep 3
    focus | grep -q "$a" && pass "page $a" || fail "page $a ($(focus))"
    input keyevent 4; sleep 1
done

# 3. tiles open their apps and home comes back, 12 times
input keyevent 3; sleep 1
ok=1; i=0
while [ $i -lt 6 ]; do
    for xy in "120 240" "360 560"; do
        input tap $xy; sleep 3
        focus | grep -q HomeActivity && ok=0
        input keyevent 3; sleep 2
        focus | grep -q HomeActivity || ok=0
    done
    i=$((i + 1))
done
[ $ok = 1 ] && pass "12 tile launches" || fail "tile launches (a tile did not leave home, or home did not come back)"

# 4. swipe down on home opens the notification shade
input swipe 240 300 240 600 250; sleep 2
focus | grep -q StatusBar && pass "swipe-down shade" || fail "swipe-down shade ($(focus))"
service call statusbar 2 >/dev/null; sleep 1

# 5. lockscreen on every wake
locks=0; i=0
while [ $i -lt 5 ]; do
    input keyevent 26; sleep 2; input keyevent 26; sleep 2
    focus | grep -q LockActivity && locks=$((locks + 1))
    input swipe 240 700 240 200 300; sleep 2
    i=$((i + 1))
done
[ $locks = 5 ] && pass "lockscreen on 5/5 wakes" || fail "lockscreen on $locks/5 wakes"

# 6. health
input keyevent 3; sleep 2
c=$(logcat -d -b crash | grep -c "FATAL EXCEPTION")
[ "$c" = 0 ] && pass "no Java crashes" || fail "$c Java crash(es)"
a=$(logcat -d | grep -c "ANR in $P")
[ "$a" = 0 ] && pass "no ANR" || fail "$a ANR(s)"
m=$(logcat -d | grep -c "BidhataModule.*failed to start")
[ "$m" = 0 ] && pass "all modules started" || fail "$m module(s) failed to start"
if [ "$(getprop ro.build.type)" != user ]; then
    s=$(logcat -d -s StrictMode | grep -c "policy violation")
    [ "$s" = 0 ] && pass "no main-thread disk/network (StrictMode)" || fail "$s StrictMode violation(s): logcat -s StrictMode"
fi
kb=$(dumpsys meminfo $P | grep "TOTAL" | head -1 | sed 's/^ *TOTAL *//; s/ .*//')
[ -n "$kb" ] && [ "$kb" -le 20480 ] && pass "memory ${kb} KB (limit 20480)" || fail "memory ${kb} KB (limit 20480)"

svc power stayon false
echo "---- $fails failure(s)"
exit $fails
