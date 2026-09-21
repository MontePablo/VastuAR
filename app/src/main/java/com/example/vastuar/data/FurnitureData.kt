package com.example.vastuar.data

import com.example.vastuar.model.Direction
import com.example.vastuar.model.FurnitureDefinition
import com.example.vastuar.model.FurnitureType
import com.example.vastuar.model.SurfaceType

object FurnitureData {
    val bed = FurnitureDefinition(
        id = "bed_01",
        name = "Bed",
        type = FurnitureType.BED,
        modelPath = "models/bed.glb",
        preferredSurface = SurfaceType.FLOOR,
        preferredDirections = setOf(
            Direction.SOUTH,
            Direction.WEST
        ),
        defaultScale = 1f
    )

    val sofa = FurnitureDefinition(
        id = "sofa_01",
        name = "Sofa",
        type = FurnitureType.SOFA,
        modelPath = "models/sofa.glb",
        preferredSurface = SurfaceType.FLOOR,
        preferredDirections = setOf(
            Direction.SOUTH,
            Direction.WEST,
//            Direction.SOUTH_WEST
        ),
        defaultScale = 1f
    )

    val table = FurnitureDefinition(
        id = "table_01",
        name = "Table",
        type = FurnitureType.TABLE,
        modelPath = "models/table.glb",
        preferredSurface = SurfaceType.FLOOR,
        preferredDirections = setOf(
            Direction.NORTH,
            Direction.EAST
        ),
        defaultScale = 1f
    )

    val tv = FurnitureDefinition(
        id = "tv_01",
        name = "TV",
        type = FurnitureType.TV,
        modelPath = "models/tv.glb",
        preferredSurface = SurfaceType.WALL,
        preferredDirections = setOf(
            Direction.SOUTH,
//            Direction.SOUTH_EAST
        ),
        defaultScale = 1f
    )

    val plant = FurnitureDefinition(
        id = "plant_01",
        name = "Plant",
        type = FurnitureType.PLANT,
        modelPath = "models/plant.glb",
        preferredSurface = SurfaceType.FLOOR,
        preferredDirections = setOf(
            Direction.NORTH,
            Direction.EAST,
//            Direction.NORTH_EAST
        ),
        defaultScale = 1f
    )

    val demoFurniture = listOf(
        bed,
        sofa,
        table,
        tv,
        plant
    )
}