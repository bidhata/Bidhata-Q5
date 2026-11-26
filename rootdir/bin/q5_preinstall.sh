#!/system/bin/sh
# Install the bundled apps (HiBy Music, VLC, Cool Reader, Material Files, Markor, Calculator++,
# F-Droid) as ordinary user apps, so they can be updated or removed like any other app.
# Each APK is remembered once installed, so a ROM update that adds an app installs just that one.

# Every boot: memory and storage tuning for a 1 GB player.
#  - 256 MB compressed swap in RAM (zram): more room before apps are closed
#  - eMMC: deadline scheduler and 512 KB read-ahead (long sequential music files)
if [ -e /sys/block/zram0/disksize ] && ! grep -q zram0 /proc/swaps; then
    echo 268435456 > /sys/block/zram0/disksize && mkswap /dev/block/zram0 >/dev/null && \
        swapon /dev/block/zram0 && log -t q5_tune "zram swap on"
fi
echo deadline > /sys/block/mmcblk0/queue/scheduler 2>/dev/null
echo 512 > /sys/block/mmcblk0/queue/read_ahead_kb 2>/dev/null

# Doze (deep sleep while idle). The image enables it in the framework config; this also covers
# units that only got a live update.
dumpsys deviceidle enable >/dev/null 2>&1

MARK=/data/system/q5_preinstall.list
touch "$MARK"

for apk in /system/preinstall/*.apk; do
    [ -f "$apk" ] || continue
    name=${apk##*/}
    grep -qxF "$name" "$MARK" && continue
    log -t q5_preinstall "installing $name"
    if pm install -r "$apk" >/dev/null 2>&1; then
        echo "$name" >> "$MARK"
    else
        log -t q5_preinstall "failed: $name"
    fi
done

# HeliBoard as the keyboard (once): dark rounded theme, hide-keyboard key first in its toolbar,
# swipe down on the toolbar to hide. The stock keyboard stays installed as a fallback.
KB=helium314.keyboard
if pm path $KB >/dev/null 2>&1 && ! grep -qxF "ime:$KB" "$MARK"; then
    ime enable $KB/.latin.LatinIME >/dev/null && ime set $KB/.latin.LatinIME >/dev/null
    ime disable com.android.inputmethod.latin/.LatinIME >/dev/null
    PD=/data/data/$KB/shared_prefs
    if [ -d /data/data/$KB ] && [ ! -f $PD/${KB}_preferences.xml ]; then
        own=$(stat -c %u:%g /data/data/$KB)
        mkdir -p $PD
        cat > $PD/${KB}_preferences.xml <<'X'
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="theme_auto_day_night" value="false" />
    <string name="theme_colors">dark</string>
    <string name="theme_colors_night">dark</string>
    <string name="theme_style">Rounded</string>
    <boolean name="theme_key_borders" value="true" />
    <boolean name="toolbar_swipe_down_to_hide" value="true" />
    <string name="toolbar_keys">HIDE_KEYBOARD;true,CLIPBOARD;true,UNDO;true,SELECT_ALL;true,NUMPAD;true,ONE_HANDED;true,SETTINGS;true</string>
</map>
X
        chown -R $own $PD; chmod 771 $PD; chmod 660 $PD/${KB}_preferences.xml
    fi
    echo "ime:$KB" >> "$MARK"
fi

# Cool Reader, once: KOReader-like paper look (plain warm page, dark serif ink, roomy margins),
# "Paperbook" page curl, full HarfBuzz shaping and Bengali / Devanagari fallback fonts
# (light shaping breaks Bengali conjuncts such as ক্ষ, স্ত্র, জ্ঞ).
INI=/data/media/0/.cr3/cr3.ini
crset() {
    if grep -q "^$1=" $INI; then sed -i "s|^$1=.*|$1=$2|" $INI; else echo "$1=$2" >> $INI; fi
}
if pm path org.coolreader >/dev/null 2>&1 && ! grep -qxF "cr3:v3" "$MARK"; then
    mkdir -p /data/media/0/.cr3
    touch $INI
    crset crengine.page.animation 1
    crset font.shaping.mode 2
    crset crengine.font.fallback.faces "Noto Sans Bengali; Noto Sans Devanagari UI; Noto Color Emoji; Noto Serif; Roboto;"
    crset background.image "(NONE)"
    crset background.image.day "(NONE)"
    crset background.color.day "\\\\#F4EFE6"
    crset background.color.default "\\\\#F4EFE6"
    crset font.color.day "\\\\#1E1C1A"
    crset font.color.default "\\\\#1E1C1A"
    crset font.face.default "Noto Serif"
    crset crengine.interline.space 115
    crset crengine.page.margin.left 28
    crset crengine.page.margin.right 28
    crset crengine.page.margin.top 12
    crset crengine.page.margin.bottom 12
    crset crengine.page.header.font.face "Noto Serif"
    crset crengine.page.header.font.color.day "\\\\#6E685E"
    crset crengine.page.header.font.color "\\\\#6E685E"
    crset crengine.page.header.font.size 20
    chown -R media_rw:media_rw /data/media/0/.cr3
    echo "cr3:v3" >> "$MARK"
fi

# HiBy Music, once: the Black theme (it starts in Green otherwise), and its own lockscreen off
# (OPTION.xml system_app: 0 HiBy lock, 1 system, 2 off) so it never stacks on top of ours.
H=/data/data/com.hiby.music
if pm path com.hiby.music >/dev/null 2>&1 && ! grep -qxF "hiby:v2" "$MARK" && [ -d $H ]; then
    am force-stop com.hiby.music
    own=$(stat -c %u:%g $H)
    mkdir -p $H/shared_prefs
    O=$H/shared_prefs/OPTION.xml
    [ -f $O ] || printf "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n</map>\n" > $O
    sed -i '/name="system_app"/d; /<string name="theme">/d' $O
    sed -i 's#</map>#    <int name="system_app" value="2" />\n    <string name="theme">black</string>\n</map>#' $O
    chown $own $H/shared_prefs $O; chmod 771 $H/shared_prefs; chmod 660 $O
    echo "hiby:v2" >> "$MARK"
fi

# JamesDSP, once: it keeps a permanent "running" notification. The audio engine doesn't need it,
# so mute the app's notifications (the same switch as Settings > Apps > JamesDSP > Block).
if pm path james.dsp >/dev/null 2>&1 && ! grep -qxF "jdsp:v1" "$MARK"; then
    appops set james.dsp POST_NOTIFICATION ignore && echo "jdsp:v1" >> "$MARK"
fi
