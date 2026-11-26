#!/system/bin/sh
# The Q5 was never given factory MAC addresses: its NVRAM only holds MediaTek placeholders
# (Wi-Fi 00:00:00:00:00:00, which also causes the "NVRAM WARNING" fake network, and the
# generic BT address 00:00:46:65:82:01 shared by every unit).
# Derive stable, per-unit addresses from the eMMC CID instead. Only placeholders are replaced.
F=/data/nvram/APCFG/APRDEB
cid=$(cat /sys/block/mmcblk0/device/cid)
[ -n "$cid" ] || exit 0

# print 6 raw bytes from a hex string, first octet made locally-administered unicast
# (toybox printf has a broken %o, so only \xHH escapes are used)
hexdigit() { echo 0123456789abcdef | cut -c$(( $1 + 1 )); }
raw6() {
    first=1
    for x in $(echo "$1" | cut -c1-12 | sed 's/../& /g'); do
        if [ $first = 1 ]; then
            b=$(( (0x$x & 252) | 2 ))
            x=$(hexdigit $((b / 16)))$(hexdigit $((b % 16)))
            first=0
        fi
        printf "\\x$x"
    done
}
hex6() { dd if=$1 bs=1 skip=$2 count=6 2>/dev/null | od -An -tx1 | tr -d ' \n'; }

if [ -f $F/WIFI ] && [ "$(hex6 $F/WIFI 4)" = "000000000000" ]; then
    raw6 $(echo -n "$cid" | md5sum | cut -c1-12) | dd of=$F/WIFI bs=1 seek=4 conv=notrunc 2>/dev/null
    log -t q5_mac "wifi mac set from emmc cid"
fi
# Bluetooth: MediaTek's BT library only accepts a complete BT_Addr record (address plus voice/
# codec/radio/sleep settings); a mostly-zero record is rejected ("Nvram data all zero") and the
# generic default address is used. Start from the stock default record and put our address in.
T=/system/etc/q5/BT_Addr.default
if [ -f $F/BT_Addr ] && [ -f $T ]; then
    cur=$(hex6 $F/BT_Addr 0)
    if [ "$cur" = "000046658201" ] || [ "$cur" = "000000000000" ]; then
        cat $T > $F/BT_Addr
        raw6 $(echo -n "${cid}bt" | md5sum | cut -c1-12) | dd of=$F/BT_Addr bs=1 seek=0 conv=notrunc 2>/dev/null
        log -t q5_mac "bt address set from emmc cid"
    fi
    # the bluetooth user must be able to reach and update the record
    chmod 0771 /data/nvram /data/nvram/APCFG /data/nvram/APCFG/APRDEB
    chown bluetooth:net_bt_stack $F/BT_Addr
    chmod 0660 $F/BT_Addr
fi

# Bluetooth: bluedroid takes its local address from ro.bt.bdaddr_path before anything it
# cached, so write the derived address there as text ("xx:xx:xx:xx:xx:xx").
bt=$(raw6 $(echo -n "${cid}bt" | md5sum | cut -c1-12) | od -An -tx1 | tr -s ' ' | sed 's/^ //; s/ $//; s/ /:/g')
if [ -n "$bt" ]; then
    echo "$bt" > /data/misc/bluedroid/bdaddr
    chown bluetooth:net_bt_stack /data/misc/bluedroid/bdaddr
    chmod 0644 /data/misc/bluedroid/bdaddr
fi

