package kz.chaykin.zakazano.data.repo

import kz.chaykin.zakazano.data.db.dao.ItemWithPhotos
import kz.chaykin.zakazano.data.db.dao.VenueSummaryRow
import kz.chaykin.zakazano.data.db.entity.ItemEntity
import kz.chaykin.zakazano.data.db.entity.PhotoEntity
import kz.chaykin.zakazano.data.db.entity.VenueEntity
import kz.chaykin.zakazano.model.Item
import kz.chaykin.zakazano.model.ItemKind
import kz.chaykin.zakazano.model.Photo
import kz.chaykin.zakazano.model.Rating
import kz.chaykin.zakazano.model.Venue
import kz.chaykin.zakazano.model.VenueSummary

internal fun VenueEntity.toDomain(): Venue = Venue(
    id = id,
    name = name,
    address = address,
    note = note,
    rating = Rating.fromCodeOrNull(ratingCode),
    photoFileName = photoFileName,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun Venue.toEntity(createdAt: Long, updatedAt: Long): VenueEntity = VenueEntity(
    id = id,
    name = name,
    address = address,
    note = note,
    ratingCode = rating?.code,
    photoFileName = photoFileName,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun VenueSummaryRow.toDomain(): VenueSummary = VenueSummary(
    venue = venue.toDomain(),
    dishCount = dishCount,
    drinkCount = drinkCount,
    averageRating = averageRating,
)

internal fun PhotoEntity.toDomain(): Photo = Photo(id = id, fileName = fileName, sortOrder = sortOrder)

internal fun ItemWithPhotos.toDomain(): Item = Item(
    id = item.id,
    venueId = item.venueId,
    name = item.name,
    kind = item.kind,
    drinkType = item.drinkType,
    rating = Rating.fromCode(item.ratingCode),
    priceMinor = item.priceMinor,
    comment = item.comment,
    photos = photos.sortedBy { it.sortOrder }.map { it.toDomain() },
    createdAt = item.createdAt,
    updatedAt = item.updatedAt,
)

internal fun Item.toEntity(createdAt: Long, updatedAt: Long): ItemEntity = ItemEntity(
    id = id,
    venueId = venueId,
    name = name,
    kind = kind,
    // Вид напитка у блюда бессмысленен: чистим, чтобы он не всплыл при смене типа позиции.
    drinkType = drinkType.takeIf { kind == ItemKind.DRINK },
    ratingCode = rating.code,
    priceMinor = priceMinor,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
