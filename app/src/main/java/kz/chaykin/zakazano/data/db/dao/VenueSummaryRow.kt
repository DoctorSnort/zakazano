package kz.chaykin.zakazano.data.db.dao

import androidx.room.Embedded
import kz.chaykin.zakazano.data.db.entity.VenueEntity

data class VenueSummaryRow(
    @Embedded val venue: VenueEntity,
    val dishCount: Int,
    val drinkCount: Int,
    val averageRating: Double?,
)
