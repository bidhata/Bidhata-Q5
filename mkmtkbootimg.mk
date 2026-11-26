# Bidhata-Q5: boot/recovery images with MediaTek headers on the ramdisk.
# The prebuilt kernel already carries its own KERNEL header.
MTK_HDR := device/timmkoo/q5/tools/mtk_hdr.py

MTK_BOOT_RAMDISK := $(PRODUCT_OUT)/ramdisk-mtk.img
$(MTK_BOOT_RAMDISK): $(INSTALLED_RAMDISK_TARGET) $(MTK_HDR)
	$(hide) rm -f $@
	$(hide) python $(MTK_HDR) ROOTFS $< $@

$(INSTALLED_BOOTIMAGE_TARGET): $(MKBOOTIMG) $(INTERNAL_BOOTIMAGE_FILES) $(MTK_BOOT_RAMDISK)
	$(call pretty,"Target boot image: $@")
	$(hide) $(MKBOOTIMG) $(subst --ramdisk $(INSTALLED_RAMDISK_TARGET),--ramdisk $(MTK_BOOT_RAMDISK),$(INTERNAL_BOOTIMAGE_ARGS)) $(BOARD_MKBOOTIMG_ARGS) --output $@
	$(hide) $(call assert-max-image-size,$@,$(BOARD_BOOTIMAGE_PARTITION_SIZE))
	@echo -e ${CL_CYN}"Made boot image: $@"${CL_RST}

MTK_RECOVERY_RAMDISK := $(PRODUCT_OUT)/ramdisk-recovery-mtk.img
$(MTK_RECOVERY_RAMDISK): $(recovery_ramdisk) $(MTK_HDR)
	$(hide) rm -f $@
	$(hide) python $(MTK_HDR) RECOVERY $< $@

$(INSTALLED_RECOVERYIMAGE_TARGET): $(MKBOOTIMG) $(recovery_ramdisk) $(recovery_kernel) $(MTK_RECOVERY_RAMDISK)
	@echo -e ${CL_CYN}"----- Making recovery image ------"${CL_RST}
	$(hide) $(MKBOOTIMG) $(subst --ramdisk $(recovery_ramdisk),--ramdisk $(MTK_RECOVERY_RAMDISK),$(INTERNAL_RECOVERYIMAGE_ARGS)) $(BOARD_MKBOOTIMG_ARGS) --output $@
	$(hide) $(call assert-max-image-size,$@,$(BOARD_RECOVERYIMAGE_PARTITION_SIZE))
	@echo -e ${CL_CYN}"Made recovery image: $@"${CL_RST}
