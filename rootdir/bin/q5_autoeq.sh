#!/system/bin/sh
# Apply an AutoEQ profile to JamesDSP (or switch it off).
#
# The Bidhata "Sound" page saves the chosen GraphicEQ line to
# /sdcard/Bidhata/AutoEQ/current.txt and sets bidhata.autoeq.target to
# headset | bluetooth | speaker (or "<output>:off"), then bumps bidhata.autoeq.req.
# JamesDSP keeps one settings file per output; only it and root may write them.

# Also handles the personal boot screen (More -> Boot screen name):
#   bootanim:apply  /sdcard/Bidhata/bootanimation.zip -> /data/system/theme/bootanimation.zip
#   bootanim:reset  remove it, back to the stock animation
TARGET=$(getprop bidhata.autoeq.target)
case "$TARGET" in
    bootanim:apply)
        mkdir -p /data/system/theme
        cp /data/media/0/Bidhata/bootanimation.zip /data/system/theme/bootanimation.zip &&
            chmod 644 /data/system/theme/bootanimation.zip && chmod 755 /data/system/theme
        log -t q5_autoeq "personal boot animation installed"
        setprop bidhata.autoeq.done "$(getprop bidhata.autoeq.req)"
        exit 0 ;;
    btprop:*)
        # Sound page codec choices; only these two settings may be changed this way
        kv=${TARGET#btprop:}; key=${kv%%=*}; val=${kv#*=}
        case "$key=$val" in
            persist.bt.a2dp.codec=auto|persist.bt.a2dp.codec=ldac|persist.bt.a2dp.codec=aac|persist.bt.a2dp.codec=sbc|\
            persist.bt.ldac.quality=abr|persist.bt.ldac.quality=hq|persist.bt.ldac.quality=sq|persist.bt.ldac.quality=mq)
                setprop "$key" "$val"; log -t q5_autoeq "$key -> $val" ;;
            *) log -t q5_autoeq "refused $kv" ;;
        esac
        setprop bidhata.autoeq.done "$(getprop bidhata.autoeq.req)"
        exit 0 ;;
    poweron:*)
        # next scheduled power-on (unix seconds, 0 = none); AlarmManagerService arms it at shutdown
        at=${TARGET#poweron:}
        case "$at" in *[!0-9]*|"") log -t q5_autoeq "refused poweron $at" ;;
            *) setprop persist.bidhata.poweron "$at"; log -t q5_autoeq "power on at $at" ;; esac
        setprop bidhata.autoeq.done "$(getprop bidhata.autoeq.req)"
        exit 0 ;;
    bootanim:reset)
        rm -f /data/system/theme/bootanimation.zip
        log -t q5_autoeq "boot animation back to stock"
        setprop bidhata.autoeq.done "$(getprop bidhata.autoeq.req)"
        exit 0 ;;
esac

PKG=james.dsp
PREFS=/data/data/$PKG/shared_prefs
SRC=/data/media/0/Bidhata/AutoEQ/current.txt
TARGET=$(getprop bidhata.autoeq.target)
OUT=${TARGET%%:*}
case "$OUT" in headset|bluetooth|speaker) ;; *) log -t q5_autoeq "bad target '$TARGET'"; exit 1 ;; esac
FILE=$PREFS/$PKG.$OUT.xml

OWNER=$(stat -c %u:%g $PREFS 2>/dev/null)
[ -n "$OWNER" ] || { log -t q5_autoeq "JamesDSP has never run"; exit 1; }
[ -f "$FILE" ] || printf "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n</map>\n" > "$FILE"

# drop our three keys, then add them back with the new values
sed -i -e '/name="dsp.streq.stringp"/d' -e '/name="dsp.streq.enable"/d' \
       -e '/name="dsp.masterswitch.enable"/d' "$FILE"
if [ "${TARGET#*:}" = "off" ]; then
    sed -i 's#</map>#    <boolean name="dsp.streq.enable" value="false" />\n</map>#' "$FILE"
    log -t q5_autoeq "AutoEQ off for $OUT"
else
    EQ=$(head -n 1 "$SRC" | tr -d '\r&<>"')
    case "$EQ" in GraphicEQ:*) ;; *) log -t q5_autoeq "no GraphicEQ in $SRC"; exit 1 ;; esac
    sed -i "s#</map>#    <boolean name=\"dsp.masterswitch.enable\" value=\"true\" />\n    <boolean name=\"dsp.streq.enable\" value=\"true\" />\n    <string name=\"dsp.streq.stringp\">$EQ</string>\n</map>#" "$FILE"
    log -t q5_autoeq "AutoEQ applied to $OUT"
fi
chown $OWNER "$FILE"
chmod 660 "$FILE"

# JamesDSP caches its settings: restart it so the new curve is loaded right away
am force-stop $PKG
am startservice -n $PKG/.service.HeadsetService >/dev/null 2>&1
setprop bidhata.autoeq.done "$(getprop bidhata.autoeq.req)"
