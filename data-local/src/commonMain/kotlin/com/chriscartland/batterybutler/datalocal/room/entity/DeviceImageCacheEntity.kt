package com.chriscartland.batterybutler.datalocal.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A cached device photo, keyed by its content-addressed `imageEtag` (see
 * [com.chriscartland.batterybutler.domain.model.Device.imageEtag]). Content-addressed means
 * immutable: an entry never needs updating, only inserting (a new photo gets a new etag) or
 * evicting (when no device references it anymore). See `docs/DEVICE_IMAGES.md` §6D.
 */
@Entity(tableName = "device_image_cache")
data class DeviceImageCacheEntity(
    @PrimaryKey val imageEtag: String,
    val bytes: ByteArray,
    val contentType: String,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is DeviceImageCacheEntity &&
                    imageEtag == other.imageEtag &&
                    bytes.contentEquals(other.bytes) &&
                    contentType == other.contentType
            )

    override fun hashCode(): Int {
        var result = imageEtag.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}
