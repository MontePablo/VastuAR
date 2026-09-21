package com.example.vastuar.ar

import io.github.sceneview.ar.node.PoseNode
import io.github.sceneview.node.LineNode
import android.Manifest
import io.github.sceneview.node.BillboardNode
import io.github.sceneview.node.Node
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.vastuar.compass.CompassManager
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.arcore.createAnchorOrNull
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.vastuar.model.FurnitureType

import io.github.sceneview.rememberModelInstance

import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import com.example.vastuar.model.Direction
import io.github.sceneview.rememberMaterialLoader

@Composable
fun ArScreen() {

    val context = LocalContext.current

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            cameraGranted = granted
        }

    LaunchedEffect(Unit) {

        if (!cameraGranted) {

            permissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    if (!cameraGranted) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "Camera permission is required"
            )
        }

        return
    }

    ArContent(
        context = context
    )
}
@Composable
private fun ArContent(
    context: Context
) {
    var floorAnchor by remember {
        mutableStateOf<Anchor?>(null)
    }
    var calibrationPose by remember { mutableStateOf<com.google.ar.core.Pose?>(null) }
    var calibrationCameraPose by remember {
        mutableStateOf<com.google.ar.core.Pose?>(null)
    }
    var calibrationHeading by remember { mutableFloatStateOf(0f) }
    var debugCalibrationNorthAngle by remember {
        mutableDoubleStateOf(0.0)
    }

    var debugCalibrationTableX by remember {
        mutableFloatStateOf(0f)
    }

    var debugCalibrationTableZ by remember {
        mutableFloatStateOf(0f)
    }
    val engine = rememberEngine()

    val modelLoader =
        rememberModelLoader(engine)

    val materialLoader = rememberMaterialLoader(engine)
    val northLineMaterial =
        remember(materialLoader) {
            materialLoader.createUnlitColorInstance(
                Color.Red
            )
        }
    val bedModel =
        rememberModelInstance(
            modelLoader,
            "models/bed.glb"
        )

    val sofaModel =
        rememberModelInstance(
            modelLoader,
            "models/sofa.glb"
        )

    val tableModel =
        rememberModelInstance(
            modelLoader,
            "models/table.glb"
        )

    val tvModel =
        rememberModelInstance(
            modelLoader,
            "models/tv.glb"
        )

    val plantModel =
        rememberModelInstance(
            modelLoader,
            "models/plant.glb"
        )
    val compassManager =
        remember {
            CompassManager(context)
        }
    var displayedHeading by remember {
        mutableIntStateOf(0)
    }

    DisposableEffect(Unit) {

        compassManager.start()

        onDispose {
            compassManager.stop()
        }
    }

    var floorDetected by remember {
        mutableStateOf(false)
    }

    var detectedFloor by remember {
        mutableStateOf<Plane?>(null)
    }

    var detectedWalls by remember {
        mutableStateOf<List<Plane>>(emptyList())
    }

    var trackingStable by remember {
        mutableStateOf(false)
    }
    var stableFrameCount by remember {
        mutableIntStateOf(0)
    }


    var automaticPlacementDone by remember {
        mutableStateOf(false)
    }
    var debugArCameraAngle by remember {
        mutableDoubleStateOf(0.0)
    }

    var debugNorthAngle by remember {
        mutableDoubleStateOf(0.0)
    }

    var debugTableX by remember {
        mutableFloatStateOf(0f)
    }

    var debugTableZ by remember {
        mutableFloatStateOf(0f)
    }
    var debugNorthLineX by remember { mutableFloatStateOf(0f) }
    var debugNorthLineZ by remember { mutableFloatStateOf(0f) }
    var showNorthLine by remember { mutableStateOf(false) }

    var debugAnchorYaw by remember {
        mutableFloatStateOf(0f)
    }
    var debugCameraToAnchorDistance by remember {
        mutableFloatStateOf(0f)
    }

    var debugLocalNorthX by remember {
        mutableFloatStateOf(0f)
    }

    var debugLocalNorthZ by remember {
        mutableFloatStateOf(0f)
    }
    data class PlacedFurniture(
        val anchor: Anchor,
        val type: FurnitureType,
        val position: Position = Position(0f),
        val rotationY: Float = 0f

    )

    var placedFurniture by remember {
        mutableStateOf<List<PlacedFurniture>>(emptyList())
    }
    var vastuStatus by remember {
        mutableStateOf<Map<FurnitureType, Boolean>>(emptyMap())
    }
    fun updateVastuStatus(
        type: FurnitureType,
        worldX: Float,
        worldZ: Float
    ) {

        val anchor =
            floorAnchor
                ?: return

        val northAngle =
            calibrationPose?.let { pose ->
                VastuPlacementEngine.calculateNorthAngle(
                    sensorPose = pose,
                    compassHeading = calibrationHeading
                )
            } ?: return

        val isCompliant =
            VastuPlacementEngine.isVastuCompliant(
                type = type,
                objectWorldX = worldX,
                objectWorldZ = worldZ,
                roomCenterWorldX = anchor.pose.tx(),
                roomCenterWorldZ = anchor.pose.tz(),
                northAngle = northAngle
            )

        vastuStatus =
            vastuStatus + (
                    type to isCompliant
                    )
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        ARSceneView(

            modifier = Modifier.fillMaxSize(),

            engine = engine,

            modelLoader = modelLoader,

            planeRenderer = true,


            sessionConfiguration = { session, config ->

                config.planeFindingMode =
                    Config
                        .PlaneFindingMode.HORIZONTAL_AND_VERTICAL

                config.lightEstimationMode =
                    Config
                        .LightEstimationMode.ENVIRONMENTAL_HDR

                if (
                    session.isDepthModeSupported(
                        Config.DepthMode.AUTOMATIC
                    )
                ) {

                    config.depthMode =
                        Config.DepthMode.AUTOMATIC
                }
            },

            onSessionUpdated = { _, frame ->

                displayedHeading = compassManager.roundedAzimuth()
//                displayedHeading= calibrationHeading.roundToInt()

                debugArCameraAngle =
                    VastuPlacementEngine.calculateArCameraAngle(
                        frame.getAndroidSensorPose()
                    )

                debugNorthAngle =
                    VastuPlacementEngine.calculateNorthAngle(
                        sensorPose = frame.getAndroidSensorPose(),
                        compassHeading = compassManager.azimuth
                    )
                val planes = frame.getUpdatedPlanes()

                val floor =
                    planes.firstOrNull {
                        it.type == Plane.Type.HORIZONTAL_UPWARD_FACING
                    }
// Once detected, keep the state true
                if (floor != null) {
                    floorDetected = true
                    detectedFloor = floor
                }



                val walls = planes.filter {
                    it.type == Plane.Type.VERTICAL &&
                            it.trackingState == TrackingState.TRACKING
                }

                if (walls.isNotEmpty()) {
                    detectedWalls = MeasurementUtils.uniqueWalls(
                        existingWalls = detectedWalls,
                        newWalls = walls
                    )
                }
                trackingStable =
                    frame.camera.trackingState ==
                            TrackingState.TRACKING

                if (trackingStable) {
                    stableFrameCount++
                } else {
                    stableFrameCount = 0
                }

                floorAnchor?.let { anchor ->
                    val cameraPose = frame.camera.pose
                    val anchorPose = anchor.pose

                    val dx = cameraPose.tx() - anchorPose.tx()
                    val dz = cameraPose.tz() - anchorPose.tz()

                    debugCameraToAnchorDistance =
                        kotlin.math.sqrt(
                            dx * dx + dz * dz
                        )
                }

                if (
                    !automaticPlacementDone &&
                    floor != null &&
                    detectedWalls.size >= 1 &&
                    stableFrameCount >= 20

                ) {

                    calibrationPose = frame.getAndroidSensorPose()
                    calibrationHeading = compassManager.azimuth
                    calibrationCameraPose =
                        frame.camera.pose
                    val calibrationNorthAngle =
                        VastuPlacementEngine.calculateNorthAngle(
                            sensorPose = calibrationPose!!,
                            compassHeading = calibrationHeading
                        )

                    debugCalibrationNorthAngle = calibrationNorthAngle

                    val calibrationTablePosition =
                        VastuPlacementEngine.positionNorth(
                            northAngle = calibrationNorthAngle,
                            distance = 1.0f
                        )

                    debugCalibrationTableX =
                        calibrationTablePosition.x

                    debugCalibrationTableZ =
                        calibrationTablePosition.z

                    // Create the permanent anchor at the latest known floor center.
                    if (floorAnchor == null) {
                        floorAnchor =
                            floor.createAnchorOrNull(
                                floor.centerPose
                            )
                    }

                    val anchorPose =
                        floor.centerPose

// Convert the North vector from AR world space
// into the AnchorNode's local coordinate system.
                    val localNorth =
                        anchorPose.inverse().rotateVector(
                            floatArrayOf(
                                calibrationTablePosition.x,
                                0f,
                                calibrationTablePosition.z
                            )
                        )

                    debugLocalNorthX =
                        localNorth[0]

                    debugLocalNorthZ =
                        localNorth[2]

// The line uses this exact same local position.
                    debugNorthLineX =
                        localNorth[0]

                    debugNorthLineZ =
                        localNorth[2]

                    showNorthLine = true

// Debug anchor yaw.
                    val anchorForward =
                        anchorPose.rotateVector(
                            floatArrayOf(
                                0f,
                                0f,
                                -1f
                            )
                        )

                    debugAnchorYaw =
                        Math.toDegrees(
                            kotlin.math.atan2(
                                anchorForward[0].toDouble(),
                                -anchorForward[2].toDouble()
                            )
                        ).toFloat()

                    automaticPlacementDone = true
                }

                // Create the permanent room anchor only when calibration happens.
            // Do not anchor to the very first small floor patch.
            }
        ){
// FLOOR MEASUREMENT LABELS
            if (showNorthLine) {
                floorAnchor?.let { anchor ->

                    AnchorNode(
                        anchor = anchor
                    ) {

                        LineNode(
                            start = Position(
                                x = 0f,
                                y = 0.03f,
                                z = 0f
                            ),
                            end = Position(
                                x = debugNorthLineX,
                                y = 0.03f,
                                z = debugNorthLineZ
                            ),
                            materialInstance = northLineMaterial
                        )
                    }
                }
            }
            detectedFloor?.let { floor ->

                val width =
                    MeasurementUtils.planeWidthMeters(floor)

                val depth =
                    MeasurementUtils.planeDepthMeters(floor)

                PoseNode(
                    pose = floor.centerPose
                ) {

                    Node(
                        rotation = Rotation(
                            x = -90f
                        )
                    ) {

                        TextNode(
                            text =
                                "${MeasurementUtils.metersText(width)} × " +
                                        MeasurementUtils.metersText(depth),

                            fontSize = 42f,

                            textColor =
                                android.graphics.Color.WHITE,

                            backgroundColor =
                                0xCC000000.toInt(),

                            widthMeters = 1.2f,
                            heightMeters = 0.25f,

                            position = Position(
                                x = 0f,
                                y = 0f,
                                z = 0f
                            )
                        )
                    }
                }
            }
            placedFurniture.forEach { furniture ->

                AnchorNode(
                    anchor = furniture.anchor
                ) {

                    when (furniture.type) {

                        FurnitureType.BED -> {
                            bedModel?.let { model ->

                                ModelNode(
                                    modelInstance = model,
                                    scaleToUnits = 1f,
                                    position = furniture.position,
                                    rotation = Rotation(
                                        y = furniture.rotationY
                                    ),
                                    isEditable = true,
                                    apply = {
                                        isPositionEditable = true
                                        isRotationEditable = true
                                        isScaleEditable = true
                                    },
                                ){
                                    TextNode(
                                        text =
                                            if (vastuStatus[FurnitureType.BED] == true)
                                                "✓ Vastu OK"
                                            else
                                                "⚠ Vastu",

                                        fontSize = 90f,

                                        textColor = android.graphics.Color.WHITE,

                                        backgroundColor =
                                            if (vastuStatus[FurnitureType.BED] == true)
                                                0xCC2E7D32.toInt()
                                            else
                                                0xCCB71C1C.toInt(),

                                        widthMeters = 2.5f,
                                        heightMeters =0.8f,

                                        position = Position(
                                            x = 0f,
                                            y = 4.2f,
                                            z = 0f
                                        )
                                    )
                                }
                            }
                        }

                        FurnitureType.SOFA -> {
                            sofaModel?.let { model ->

                                ModelNode(
                                    modelInstance = model,
                                    scaleToUnits = 1f,
                                    position = furniture.position,
                                    rotation = Rotation(
                                        y = furniture.rotationY
                                    ),
                                    isEditable = true,
                                    apply = {
                                        isPositionEditable = true
                                        isRotationEditable = true
                                        isScaleEditable = true
                                    }
                                ){
                                    TextNode(
                                        text =
                                            if (vastuStatus[FurnitureType.SOFA] == true)
                                                "✓ Vastu OK"
                                            else
                                                "⚠ Vastu",

                                        fontSize = 90f,

                                        textColor = android.graphics.Color.WHITE,

                                        backgroundColor =
                                            if (vastuStatus[FurnitureType.SOFA] == true)
                                                0xCC2E7D32.toInt()
                                            else
                                                0xCCB71C1C.toInt(),
                                        widthMeters = 2.5f,
                                        heightMeters =0.8f,

                                        position = Position(
                                            x = 0f,
                                            y = 16.2f,
                                            z = 0f
                                        )
                                    )
                                }
                            }
                        }

                        FurnitureType.TABLE -> {
                            tableModel?.let { model ->

                                ModelNode(
                                    modelInstance = model,
                                    scaleToUnits = 1f,
                                    position = furniture.position,
                                    rotation = Rotation(
                                        y = furniture.rotationY
                                    ),
                                    isEditable = true,
                                    apply = {
                                        isPositionEditable = true
                                        isRotationEditable = true
                                        isScaleEditable = true
                                    }
                                ){
                                    TextNode(
                                        text =
                                            if (vastuStatus[FurnitureType.TABLE] == true)
                                                "✓ Vastu OK"
                                            else
                                                "⚠ Vastu",

                                        fontSize = 90f,

                                        textColor = android.graphics.Color.WHITE,

                                        backgroundColor =
                                            if (vastuStatus[FurnitureType.TABLE] == true)
                                                0xCC2E7D32.toInt()
                                            else
                                                0xCCB71C1C.toInt(),

                                        widthMeters = 2.5f,
                                        heightMeters =0.8f,

                                        position = Position(
                                            x = 0f,
                                            y = 10.2f,
                                            z = 0f
                                        )
                                    )
                                }
                            }
                        }

                        FurnitureType.TV -> {
                            tvModel?.let { model ->

                                ModelNode(
                                    modelInstance = model,
                                    scaleToUnits = 1f,
                                    position = furniture.position,
                                    rotation = Rotation(
                                        y = furniture.rotationY
                                    ),
                                    isEditable = true,
                                    apply = {
                                        isPositionEditable = true
                                        isRotationEditable = true
                                        isScaleEditable = true
                                    }
                                ){
                                    TextNode(
                                        text =
                                            if (vastuStatus[FurnitureType.TV] == true)
                                                "✓ Vastu OK"
                                            else
                                                "⚠ Vastu",

                                        fontSize = 90f,

                                        textColor = android.graphics.Color.WHITE,

                                        backgroundColor =
                                            if (vastuStatus[FurnitureType.TV] == true)
                                                0xCC2E7D32.toInt()
                                            else
                                                0xCCB71C1C.toInt(),

                                        widthMeters = 2.5f,
                                        heightMeters =0.8f,

                                        position = Position(
                                            x = 0f,
                                            y = 10.7f,
                                            z = 0f
                                        )
                                    )
                                }
                            }
                        }

                        FurnitureType.PLANT -> {
                            plantModel?.let { model ->

                                ModelNode(
                                    modelInstance = model,
                                    scaleToUnits = 1f,
                                    position = furniture.position,
                                    rotation = Rotation(
                                        y = furniture.rotationY
                                    ),
                                    isEditable = true,
                                    apply = {
                                        isPositionEditable = true
                                        isRotationEditable = true
                                        isScaleEditable = true
                                    }
                                ) {
                                    TextNode(
                                        text =
                                            if (vastuStatus[FurnitureType.PLANT] == true)
                                                "✓ Vastu OK"
                                            else
                                                "⚠ Vastu",

                                        fontSize = 120f,

                                        textColor = android.graphics.Color.WHITE,

                                        backgroundColor =
                                            if (vastuStatus[FurnitureType.PLANT] == true)
                                                0xCC2E7D32.toInt()
                                            else
                                                0xCCB71C1C.toInt(),

                                        widthMeters = 2.5f,
                                        heightMeters =0.8f,

                                        position = Position(
                                            x = 0f,
                                            y = 8.2f,
                                            z = 0f
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(
            automaticPlacementDone,
            floorAnchor,
            bedModel,
            sofaModel,
            tableModel,
            tvModel,
            plantModel
        ) {

            if (!automaticPlacementDone) {
                return@LaunchedEffect
            }

            if (placedFurniture.isNotEmpty()) {
                return@LaunchedEffect
            }

            val floor =
                detectedFloor
                    ?: return@LaunchedEffect

            val anchor =
                floorAnchor
                    ?: return@LaunchedEffect

            val cameraPose =
                calibrationCameraPose
                    ?: return@LaunchedEffect

            val northAngle =
                calibrationPose?.let { pose ->
                    VastuPlacementEngine.calculateNorthAngle(
                        sensorPose = pose,
                        compassHeading = calibrationHeading
                    )
                } ?: return@LaunchedEffect

            val roomWidth =
                MeasurementUtils.planeWidthMeters(floor)

            val roomDepth =
                MeasurementUtils.planeDepthMeters(floor)

            val furniture =
                mutableListOf<PlacedFurniture>()

            val usedDirections =
                mutableSetOf<Direction>()

            fun addFloorFurniture(
                type: FurnitureType
            ) {

                val direction =
                    VastuPlacementEngine.chooseDirection(
                        type = type,
                        usedDirections = usedDirections
                    ) ?: return

                usedDirections.add(direction)

                val worldPosition =
                    VastuPlacementEngine.positionForDirection(
                        direction = direction,
                        northAngle = northAngle,
                        roomWidth = roomWidth,
                        roomDepth = roomDepth
                    )

                /*
                 * positionForDirection() gives us a WORLD displacement.
                 *
                 * ModelNode.position is LOCAL to AnchorNode,
                 * so convert that displacement into anchor-local space.
                 */
                val localPosition =
                    anchor.pose.inverse().rotateVector(
                        floatArrayOf(
                            worldPosition.x,
                            0f,
                            worldPosition.z
                        )
                    )

                val worldFurnitureX =
                    anchor.pose.tx() + worldPosition.x

                val worldFurnitureZ =
                    anchor.pose.tz() + worldPosition.z

                val rotationY =
                    VastuPlacementEngine.calculateFurnitureRotationTowardCamera(
                        cameraPose = cameraPose,
                        objectWorldX = worldFurnitureX,
                        objectWorldZ = worldFurnitureZ,
                        anchorPose = anchor.pose
                    )

                furniture.add(
                    PlacedFurniture(
                        anchor = anchor,
                        type = type,
                        position = Position(
                            x = localPosition[0],
                            y = 0f,
                            z = localPosition[2]
                        ),
                        rotationY = rotationY
                    )
                )
            }

            /*
             * Floor furniture.
             */
            addFloorFurniture(
                FurnitureType.BED
            )

            addFloorFurniture(
                FurnitureType.SOFA
            )

            addFloorFurniture(
                FurnitureType.TABLE
            )

            addFloorFurniture(
                FurnitureType.PLANT
            )

            /*
             * TV:
             *
             * 1. Choose its Vastu direction.
             * 2. Find the detected wall closest to that direction.
             * 3. Put the TV on that wall.
             * 4. Rotate it to face into the room.
             */
            val tvDirection =
                VastuPlacementEngine.chooseDirection(
                    type = FurnitureType.TV,
                    usedDirections = usedDirections
                )

            if (tvDirection != null) {

                val desiredDirection =
                    VastuPlacementEngine.directionVector(
                        direction = tvDirection,
                        northAngle = northAngle
                    )

                val tvWall =
                    detectedWalls.maxByOrNull { wall ->

                        val dx =
                            wall.centerPose.tx() -
                                    anchor.pose.tx()

                        val dz =
                            wall.centerPose.tz() -
                                    anchor.pose.tz()

                        val distance =
                            kotlin.math.sqrt(
                                dx * dx +
                                        dz * dz
                            )

                        if (distance < 0.001f) {
                            -1f
                        } else {
                            val normalizedX =
                                dx / distance

                            val normalizedZ =
                                dz / distance

                            normalizedX * desiredDirection[0] +
                                    normalizedZ * desiredDirection[2]
                        }
                    }

                if (tvWall != null) {

                    /*
                     * Wall -> room direction.
                     *
                     * We use this to move the TV slightly
                     * away from the wall.
                     */
                    val wallFacingDirection =
                        VastuPlacementEngine.calculateWallFacingDirection(
                            wallPose = tvWall.centerPose,
                            cameraPose = cameraPose
                        )

                    val tvWorldX =
                        tvWall.centerPose.tx() +
                                wallFacingDirection[0] * 0.08f

                    val tvWorldZ =
                        tvWall.centerPose.tz() +
                                wallFacingDirection[2] * 0.08f

                    val relativeToAnchor =
                        floatArrayOf(
                            tvWorldX - anchor.pose.tx(),
                            0f,
                            tvWorldZ - anchor.pose.tz()
                        )

                    val localTvPosition =
                        anchor.pose.inverse().rotateVector(
                            relativeToAnchor
                        )

                    val tvRotationY =
                        VastuPlacementEngine.calculateTvRotationForWall(
                            wallPose = tvWall.centerPose,
                            cameraPose = cameraPose,
                            anchorPose = anchor.pose
                        )

                    furniture.add(
                        PlacedFurniture(
                            anchor = anchor,
                            type = FurnitureType.TV,
                            position = Position(
                                x = localTvPosition[0],
                                y = 1.0f,
                                z = localTvPosition[2]
                            ),
                            rotationY = tvRotationY
                        )
                    )
                }
            }
            val initialStatus =
                mutableMapOf<FurnitureType, Boolean>()

            furniture.forEach { item ->

                val worldX =
                    anchor.pose.tx() +
                            item.position.x

                val worldZ =
                    anchor.pose.tz() +
                            item.position.z

                val isCompliant =
                    VastuPlacementEngine.isVastuCompliant(
                        type = item.type,
                        objectWorldX = worldX,
                        objectWorldZ = worldZ,
                        roomCenterWorldX = anchor.pose.tx(),
                        roomCenterWorldZ = anchor.pose.tz(),
                        northAngle = northAngle
                    )

                initialStatus[item.type] =
                    isCompliant
            }

            vastuStatus = initialStatus

            placedFurniture =
                furniture
        }
        ScanOverlay(
            floorDetected = floorDetected,
            wallCount = detectedWalls.size,
            trackingStable = trackingStable,
            compassDegrees = displayedHeading,
            debugArCameraAngle = debugArCameraAngle,
            debugNorthAngle = debugNorthAngle,
            debugTableX = debugTableX,
            debugTableZ = debugTableZ,
            debugCalibrationTableZ = debugCalibrationTableZ,
            debugCalibrationTableX= debugCalibrationTableX,
            debugCalibrationNorthAngle = debugCalibrationNorthAngle,
            debugAnchorYaw = debugAnchorYaw,
            debugLocalNorthX = debugLocalNorthX,
            debugLocalNorthZ = debugLocalNorthZ,
            debugCameraToAnchorDistance =
                debugCameraToAnchorDistance,
            stableFrameCount = stableFrameCount,

            floorWidth = detectedFloor?.let {
                MeasurementUtils.planeWidthMeters(it)
            },
            floorDepth = detectedFloor?.let {
                MeasurementUtils.planeDepthMeters(it)
            },
        )
    }
}
@Composable
private fun ScanOverlay(
    floorDetected: Boolean,
    wallCount: Int,
    trackingStable: Boolean,
    compassDegrees: Int,

    stableFrameCount: Int,
    debugArCameraAngle: Double,
    debugNorthAngle: Double,
    debugTableX: Float,
    debugTableZ: Float,
    debugCalibrationTableZ:Float,
    debugCalibrationTableX:Float,
    debugCalibrationNorthAngle: Double,
    debugAnchorYaw: Float,
    debugLocalNorthX: Float,
    debugLocalNorthZ: Float,
    debugCameraToAnchorDistance: Float,

    floorWidth: Float?,
    floorDepth: Float?,
) {

    val ready =
        floorDetected &&
                wallCount >= 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        // TOP: MEASUREMENT
        if (floorDetected &&
            floorWidth != null &&
            floorDepth != null
        ) {

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(
                        Color.Black.copy(alpha = 0.75f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(
                        horizontal = 18.dp,
                        vertical = 12.dp
                    )
            ) {

                Text(
                    text = "Detected Floor",
                    color = Color.White,
                    fontSize = 16.sp
                )

                Text(
                    text =
                        "${MeasurementUtils.metersText(floorWidth)} × " +
                                MeasurementUtils.metersText(floorDepth),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Walls detected: $wallCount",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        // BOTTOM: SCAN STATUS
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(
                    Color.Black.copy(alpha = 0.70f)
                )
                .padding(16.dp)
        ) {

            Text(
                text =
                    if (ready)
                        "✓ Room detected"
                    else
                        "🔍 Scanning room",
                color = Color.White
            )

            Text(
                text =
                    if (floorDetected)
                        "✓ Floor detected"
                    else
                        "○ Searching for floor",
                color = Color.White
            )

            Text(
                text =
                    if (wallCount > 0)
                        "✓ Walls detected: $wallCount"
                    else
                        "○ Searching for walls",
                color = Color.White
            )

            Text(
                text =
                    if (trackingStable)
                        "✓ Tracking stable"
                    else
                        "→ Move phone slowly",
                color = Color.White
            )

            Text(
                text = "Heading: $compassDegrees°",
                color = Color.White
            )
            Text(
                text = "AR angle: ${Math.toDegrees(debugArCameraAngle).toInt()}°",
                color = Color.White
            )

            Text(
                text = "North angle: ${Math.toDegrees(debugNorthAngle).toInt()}°",
                color = Color.White
            )

            Text(
                text = "Table X/Z: %.2f / %.2f".format(
                    debugTableX,
                    debugTableZ
                ),
                color = Color.White
            )

            Text(
                text = "Stable frames: $stableFrameCount",
                color = Color.White
            )
            Text(
                text = "CAL North: ${
                    Math.toDegrees(debugCalibrationNorthAngle).toInt()
                }°",
                color = Color.White
            )

            Text(
                text = "CAL Table X/Z: %.2f / %.2f".format(
                    debugCalibrationTableX,
                    debugCalibrationTableZ
                ),
                color = Color.White
            )
            Text(
                text = "Anchor yaw: %.1f°".format(debugAnchorYaw),
                color = Color.White
            )

            Text(
                text = "Local North X/Z: %.2f / %.2f".format(
                    debugLocalNorthX,
                    debugLocalNorthZ
                ),
                color = Color.White
            )
            Text(
                text = "Camera → Anchor: %.2f m".format(
                    debugCameraToAnchorDistance
                ),
                color = Color.White
            )
        }
    }
}
