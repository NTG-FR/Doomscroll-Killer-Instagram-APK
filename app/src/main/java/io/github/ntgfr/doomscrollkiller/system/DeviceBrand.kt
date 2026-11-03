package io.github.ntgfr.doomscrollkiller.system

/**
 * Phone makers known to stop background apps aggressively, each needing its own extra steps.
 * The detailed, always up-to-date guides live on dontkillmyapp.com.
 */
enum class DeviceBrand(val dontKillMyAppSlug: String?) {
    XIAOMI("xiaomi"),
    SAMSUNG("samsung"),
    HUAWEI("huawei"),
    ONEPLUS("oneplus"),
    OPPO("oppo"),
    VIVO("vivo"),
    OTHER(null);

    val guideUrl: String
        get() = "https://dontkillmyapp.com/" + (dontKillMyAppSlug ?: "")

    companion object {
        /** Maps Build.MANUFACTURER to a known brand (sub-brands included). Pure function, tested. */
        fun fromManufacturer(manufacturer: String?): DeviceBrand = when (manufacturer?.trim()?.lowercase()) {
            "xiaomi", "redmi", "poco" -> XIAOMI
            "samsung" -> SAMSUNG
            "huawei", "honor" -> HUAWEI
            "oneplus" -> ONEPLUS
            "oppo", "realme" -> OPPO
            "vivo", "iqoo" -> VIVO
            else -> OTHER
        }
    }
}
