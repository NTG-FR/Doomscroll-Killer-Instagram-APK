package io.github.ntgfr.doomscrollkiller.system

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceBrandTest {

    @Test
    fun subBrands_mapToTheirParent() {
        assertEquals(DeviceBrand.XIAOMI, DeviceBrand.fromManufacturer("Xiaomi"))
        assertEquals(DeviceBrand.XIAOMI, DeviceBrand.fromManufacturer("Redmi"))
        assertEquals(DeviceBrand.XIAOMI, DeviceBrand.fromManufacturer("POCO"))
        assertEquals(DeviceBrand.HUAWEI, DeviceBrand.fromManufacturer("HONOR"))
        assertEquals(DeviceBrand.OPPO, DeviceBrand.fromManufacturer("realme"))
        assertEquals(DeviceBrand.SAMSUNG, DeviceBrand.fromManufacturer(" samsung "))
    }

    @Test
    fun unknownBrands_areOther() {
        assertEquals(DeviceBrand.OTHER, DeviceBrand.fromManufacturer("Google"))
        assertEquals(DeviceBrand.OTHER, DeviceBrand.fromManufacturer(""))
        assertEquals(DeviceBrand.OTHER, DeviceBrand.fromManufacturer(null))
    }

    @Test
    fun guideUrl_pointsToDontKillMyApp() {
        assertEquals("https://dontkillmyapp.com/xiaomi", DeviceBrand.XIAOMI.guideUrl)
    }
}
