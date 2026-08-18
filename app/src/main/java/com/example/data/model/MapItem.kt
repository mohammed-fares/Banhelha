package com.example.data.model

sealed class MapItem {
    abstract val id: String
    abstract val latitude: Double
    abstract val longitude: Double
    abstract val title: String
    abstract val subtitle: String

    data class UserPin(
        val user: UserEntity,
        override val id: String = "user_${user.id}",
        override val latitude: Double = user.latitude,
        override val longitude: Double = user.longitude,
        override val title: String = user.name,
        override val subtitle: String = user.bio.ifBlank { "${user.age} yrs • ${user.gender}" }
    ) : MapItem()

    data class ServicePin(
        val service: LocalServiceEntity,
        override val id: String = "service_${service.id}",
        override val latitude: Double = service.latitude,
        override val longitude: Double = service.longitude,
        override val title: String = service.title,
        override val subtitle: String = "${service.category} • ⭐ ${service.rating}"
    ) : MapItem()

    data class ClusterPin(
        override val id: String,
        override val latitude: Double,
        override val longitude: Double,
        val itemsCount: Int,
        val items: List<MapItem>,
        override val title: String = "$itemsCount عنصر قريب",
        override val subtitle: String = "انقر لعرض التفاصيل وتكبير الخريطة"
    ) : MapItem()
}
