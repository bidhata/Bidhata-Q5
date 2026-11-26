/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _BDROID_BUILDCFG_H
#define _BDROID_BUILDCFG_H

#define BTA_DISABLE_DELAY 1000 /* in milliseconds */

/* Bidhata-Q5 A2DP multi-codec (LDAC / AAC / SBC) */
#define AVDT_CODEC_SIZE  20   /* LDAC codec info = 11 bytes incl. length (stock 10 truncates it) */
#define BTA_AV_MAX_SEPS  4    /* SBC src, SBC sink, AAC src, LDAC src */
#define AVDT_NUM_SEPS    6    /* 1 audio BTA_AvRegister x 3 source SEPs + margin */

#endif