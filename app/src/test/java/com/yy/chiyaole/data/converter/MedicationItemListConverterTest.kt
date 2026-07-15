package com.yy.chiyaole.data.converter

import org.junit.Assert.assertEquals
import org.junit.Test
import com.yy.chiyaole.data.model.MedicationItem

class MedicationItemListConverterTest {
    @Test
    fun roundTrip_singleItem() {
        val c = MedicationItemListConverter()
        val list = listOf(MedicationItem("阿莫西林", "0.5g", "每日3次", "7天"))
        val back = c.toMedicationItemList(c.fromMedicationItemList(list))
        assertEquals(list, back)
    }

    @Test
    fun roundTrip_multipleItems() {
        val c = MedicationItemListConverter()
        val list = listOf(
            MedicationItem("阿莫西林", "0.5g", "每日3次", "7天"),
            MedicationItem("布洛芬", "0.2g", "每日2次", "3天"),
            MedicationItem("维生素C", "0.1g", "每日1次", "")
        )
        val json = c.fromMedicationItemList(list)
        val back = c.toMedicationItemList(json)
        assertEquals(list, back)
    }
}
