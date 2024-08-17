/*
 * Decompiled with CFR 0.2.2 (FabricMC 7c48b8c4).
 */
package xyz.bobkinn.minecarts

import com.google.common.collect.Iterables
import io.netty.buffer.ByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BlockTags
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.vehicle.AbstractMinecart
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.block.BaseRailBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.PoweredRailBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.RailShape
import net.minecraft.world.phys.Vec3
import xyz.bobkinn.minecarts.mixin.EntityAccessor
import xyz.bobkinn.minecarts.mixin.MinecartAccessor
import xyz.bobkinn.minecarts.mixin.MixinAbstractMinecart
import java.util.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

class NewMinecartBehavior(abstractMinecart: AbstractMinecart, private val mixin: MixinAbstractMinecart) : MinecartBehavior(abstractMinecart) {
    private var cacheIndexAlpha: StepPartialTicks? = null
    private var cachedLerpDelay = 0
    private var cachedPartialTick = 0f
    private var lerpDelay = 0
    val lerpSteps: MutableList<MinecartStep> = LinkedList()
    val currentLerpSteps: MutableList<MinecartStep> = LinkedList()
    var currentLerpStepsTotalWeight: Double = 0.0
    var oldLerp: MinecartStep = MinecartStep.ZERO
    private var firstTick = true

    private val accessor = abstractMinecart as MinecartAccessor
    private val entityAccessor = abstractMinecart as EntityAccessor


    private fun AbstractMinecart.getCurrentBlockPos(): BlockPos {
        val i = Mth.floor(this.x)
        var j = Mth.floor(this.y)
        val k = Mth.floor(this.z)
        if (this.level().getBlockState(BlockPos(i, j - 1, k)).`is`(BlockTags.RAILS)) {
            --j
        }
        return BlockPos(i, j, k)
    }

    private fun Vec3.horizontal(): Vec3 {
        return Vec3(x, 0.0, z);
    }

    private fun Vec3i.toVec() : Vec3{
        return Vec3(x.toDouble(), y.toDouble(), z.toDouble());
    }

    private fun BlockPos.getBottomCenter(): Vec3 {
        return Vec3.atBottomCenterOf(this)
    }

    private var isFlipped: Boolean = false;

    private fun AbstractMinecart.isFlipped(): Boolean {
        return isFlipped
    }

    private fun AbstractMinecart.setFlipped(value: Boolean) {
        isFlipped = value
    }

    private fun Entity.oldPosition(): Vec3 {
        return Vec3(this.xOld, this.yOld, this.zOld)
    }

    private fun Entity.isAffectedByBlocks(): Boolean {
        return !this.isRemoved && !this.noPhysics
    }

    private fun Entity.applyEffectsFromBlocks() {
        if (!this.isAffectedByBlocks()) {
            return
        }
        val bl: Boolean = this.isOnFire()
        if (this.onGround()) {
            val blockPos: BlockPos = this.getOnPosLegacy()
            val blockState2 = level().getBlockState(blockPos)
            blockState2.block.stepOn(this.level(), blockPos, blockState2, this)
        }
    }

    override fun tick() {
        if (level().isClientSide) {
            this.lerpClientPositionAndRotation()

            val bl = BaseRailBlock.isRail(level().getBlockState(minecart.getCurrentBlockPos()))
            accessor.setOnRails(bl)
            this.firstTick = false
            return
        }
        val blockPos = minecart.getCurrentBlockPos();
        val blockState = level().getBlockState(blockPos)
        if (this.firstTick) {
            accessor.setOnRails(BaseRailBlock.isRail(blockState))
            this.adjustToRails(blockPos, blockState)
        }
        entityAccessor.invokeApplyGravity()
        moveAlongTrack()
        this.firstTick = false
    }

    private fun lerpClientPositionAndRotation() {
        if (--this.lerpDelay <= 0) {
            this.setOldLerpValues()
            currentLerpSteps.clear()
            if (lerpSteps.isNotEmpty()) {
                currentLerpSteps.addAll(this.lerpSteps)
                lerpSteps.clear()
                this.lerpDelay = 3
                this.currentLerpStepsTotalWeight = 0.0
                for ((_, _, _, _, weight) in this.currentLerpSteps) {
                    this.currentLerpStepsTotalWeight += weight.toDouble()
                }
            }
        }
        if (this.cartHasPosRotLerp()) {
            this.setPos(this.getCartLerpPosition(1.0f))
            this.deltaMovement = this.getCartLerpMovements(1.0f)
            this.xRot = this.getCartLerpXRot(1.0f)
            this.yRot = this.getCartLerpYRot(1.0f)
        }
    }

    private fun setOldLerpValues() {
        this.oldLerp = MinecartStep(this.position(), this.deltaMovement, this.yRot, this.xRot, 0.0f)
    }

    private fun cartHasPosRotLerp(): Boolean {
        return currentLerpSteps.isNotEmpty()
    }

    private fun getCartLerpXRot(f: Float): Float {
        val stepPartialTicks = this.getCurrentLerpStep(f)
        return Mth.rotLerp(stepPartialTicks!!.partialTicksInStep, stepPartialTicks.previousStep.xRot, stepPartialTicks.currentStep.xRot
        )
    }

    private fun getCartLerpYRot(f: Float): Float {
        val stepPartialTicks = this.getCurrentLerpStep(f)
        return Mth.rotLerp(stepPartialTicks!!.partialTicksInStep, stepPartialTicks.previousStep.yRot, stepPartialTicks.currentStep.yRot
        )
    }

    private fun getCartLerpPosition(f: Float): Vec3 {
        val stepPartialTicks = this.getCurrentLerpStep(f)
        return lerp(stepPartialTicks!!.partialTicksInStep.toDouble(), stepPartialTicks.previousStep.position, stepPartialTicks.currentStep.position
        )
    }

    private fun getCartLerpMovements(f: Float): Vec3 {
        val stepPartialTicks = this.getCurrentLerpStep(f)
        return lerp(stepPartialTicks!!.partialTicksInStep.toDouble(), stepPartialTicks.previousStep.movement, stepPartialTicks.currentStep.movement
        )
    }

    private fun getCurrentLerpStep(f: Float): StepPartialTicks? {
        var j: Int
        if (f == this.cachedPartialTick && this.lerpDelay == this.cachedLerpDelay && this.cacheIndexAlpha != null) {
            return this.cacheIndexAlpha
        }
        val g = ((3 - this.lerpDelay).toFloat() + f) / 3.0f
        var h = 0.0f
        var i = 1.0f
        var bl = false
        j = 0
        while (j < currentLerpSteps.size) {
            val k = currentLerpSteps[j].weight
            if (k <= 0.0f || !(k.let { h += it; h }.toDouble() >= this.currentLerpStepsTotalWeight * g.toDouble())) {
                ++j
                continue
            }
            val l = h - k
            i = ((g.toDouble() * this.currentLerpStepsTotalWeight - l.toDouble()) / k.toDouble()).toFloat()
            bl = true
            break
        }
        if (!bl) {
            j = currentLerpSteps.size - 1
        }
        val minecartStep = currentLerpSteps[j]
        val minecartStep2 = if (j > 0) currentLerpSteps[j - 1] else this.oldLerp
        this.cacheIndexAlpha = StepPartialTicks(i, minecartStep, minecartStep2)
        this.cachedLerpDelay = this.lerpDelay
        this.cachedPartialTick = f
        return this.cacheIndexAlpha
    }

    private fun adjustToRails(blockPos: BlockPos, blockState: BlockState) {
        if (!BaseRailBlock.isRail(blockState)) {
            return
        }
        val railShape = blockState.getValue((blockState.block as BaseRailBlock).shapeProperty)
        val pair = MinecartAccessor.invokeExits(railShape)
        val vec3i = pair.first
        val vec3i2 = pair.second
        var vec3 = vec3i.toVec().scale(0.5).horizontal()
        val vec32 =  vec3i2.toVec().scale(0.5).horizontal()
        if (this.deltaMovement.length() > 1.0E-5 && this.deltaMovement.dot(vec3) < this.deltaMovement.dot(vec32)) {
            vec3 = vec32
        }
        var f = 180.0f - (atan2(vec3.z, vec3.x) * 180.0 / Math.PI).toFloat()
        this.yRot = (if (minecart.isFlipped()) 180.0f else 0.0f).let { f += it; f }
        val bl = vec3i.y != vec3i2.y
        val vec33 = this.position()
        val vec34: Vec3 = blockPos.getBottomCenter().subtract(vec33)
        this.setPos(vec33.add(vec34))
        if (bl) {
            val vec35: Vec3 = blockPos.getBottomCenter().add(vec32)
            val d = vec35.distanceTo(this.position())
            this.setPos(position().add(0.0, d + 0.1, 0.0))
        } else {
            this.setPos(position().add(0.0, 0.1, 0.0))
            this.xRot = 0.0f
        }
        val e = vec33.distanceTo(this.position())
        if (e > 0.0) {
            lerpSteps.add(MinecartStep(this.position(), this.deltaMovement, this.yRot, this.xRot, e.toFloat()))
        }
    }

    override fun moveAlongTrack() {
        val trackIteration = TrackIteration()
        while (trackIteration.shouldIterate()) {
            val blockPos = minecart.getCurrentBlockPos();
            val blockState = level().getBlockState(blockPos)
            val bl = BaseRailBlock.isRail(blockState)
            if (minecart.isOnRails != bl) {
                accessor.setOnRails(bl)
                this.adjustToRails(blockPos, blockState)
            }
            if (bl) {
                minecart.resetFallDistance()
                minecart.setOldPosAndRot()
                if (blockState.`is`(Blocks.ACTIVATOR_RAIL)) {
                    minecart.activateMinecart(blockPos.x, blockPos.y, blockPos.z, blockState.getValue(PoweredRailBlock.POWERED))
                }
                val railShape = blockState.getValue((blockState.block as BaseRailBlock).shapeProperty)
                val vec3 = this.calculateTrackSpeed(this.deltaMovement.horizontal(), trackIteration, blockPos, blockState, railShape)
                trackIteration.movementLeft =
                    if (trackIteration.firstIteration) vec3.horizontalDistance()
                    else ((vec3.horizontalDistance() - this.deltaMovement.horizontalDistance()).let { trackIteration.movementLeft += it; trackIteration.movementLeft })
                this.deltaMovement = vec3
                trackIteration.movementLeft = stepAlongTrack(blockPos, railShape, trackIteration.movementLeft)
            } else {
                accessor.invokeComeOffTrack()
                trackIteration.movementLeft = 0.0
            }
            val vec32 = this.position()
            val d: Double = minecart.oldPosition().subtract(vec32).length()
            if (d > 1.0E-5) {
                var f = this.yRot
                if (this.deltaMovement.horizontalDistanceSqr() > 0.0) {
                    f = 180.0f - (atan2(this.deltaMovement.z, this.deltaMovement.x) * 180.0 / Math.PI).toFloat()
                    f += if (minecart.isFlipped()) 180.0f else 0.0f
                }
                var g = if (minecart.onGround() && !minecart.isOnRails) 0.0f else 90.0f - (atan2(
                    this.deltaMovement.horizontalDistance(),
                    this.deltaMovement.y
                ) * 180.0 / Math.PI).toFloat()
                g *= if (minecart.isFlipped()) -1.0f else 1.0f
                val e = abs((f - this.yRot).toDouble())
                if (e in 175.0..185.0) {
                    minecart.setFlipped(!minecart.isFlipped())
                    f -= 180.0f
                    g *= -1.0f
                }
                g = Math.clamp(g, -45.0f, 45.0f)
                this.xRot = g % 360.0f
                this.yRot = f % 360.0f
                lerpSteps.add(MinecartStep(vec32, this.deltaMovement, f, g, d.toFloat()))
            }
            if (d > 1.0E-5 || trackIteration.firstIteration) {
                minecart.applyEffectsFromBlocks()
            }
            trackIteration.firstIteration = false
        }
    }

    private fun calculateTrackSpeed(
        vec3: Vec3,
        trackIteration: TrackIteration,
        blockPos: BlockPos,
        blockState: BlockState,
        railShape: RailShape
    ): Vec3 {
        var vec33: Vec3
        var vec332: Vec3
        var vec32 = vec3
        if (!trackIteration.hasGainedSlopeSpeed && calculateSlopeSpeed(vec32, railShape).also { vec332 = it }
                .horizontalDistanceSqr() != vec32.horizontalDistanceSqr()) {
            trackIteration.hasGainedSlopeSpeed = true
            vec32 = it;
        }
        if (trackIteration.firstIteration && calculatePlayerInputSpeed(vec32).also { vec332 = it }
                .horizontalDistanceSqr() != vec32.horizontalDistanceSqr()) {
            trackIteration.hasHalted = true
            vec32 = vec332
        }
        if (!trackIteration.hasHalted && calculateHaltTrackSpeed(vec32, blockState).also { vec332 = it }
                .horizontalDistanceSqr() != vec32.horizontalDistanceSqr()) {
            trackIteration.hasHalted = true
            vec32 = vec332
        }
//        if (trackIteration.firstIteration && minecart.applyNaturalSlowdown().also { vec32 }
//                .lengthSqr() > 0.0) {
//            val d = min(vec32.length(), minecart.getMaxSpeed())
//            vec32 = vec32.normalize().scale(d)
//        }
//        if (!trackIteration.hasBoosted && calculateBoostTrackSpeed(vec32, blockPos, blockState).also { vec33 = it }
//                .horizontalDistanceSqr() != vec32.horizontalDistanceSqr()) {
//            trackIteration.hasBoosted = true
//            vec32 = vec33
//        }
        return vec32
    }

    private fun calculateSlopeSpeed(vec3: Vec3, railShape: RailShape): Vec3 {
        var d = max(0.0078125, vec3.horizontalDistance() * 0.02)
        if (minecart.isInWater) {
            d *= 0.2
        }
        return when (railShape) {
            RailShape.ASCENDING_EAST -> vec3.add(-d, 0.0, 0.0)
            RailShape.ASCENDING_WEST -> vec3.add(d, 0.0, 0.0)
            RailShape.ASCENDING_NORTH -> vec3.add(0.0, 0.0, d)
            RailShape.ASCENDING_SOUTH -> vec3.add(0.0, 0.0, -d)
            else -> vec3
        }
    }

    private fun calculatePlayerInputSpeed(vec3: Vec3): Vec3 {
        val entity = minecart.firstPassenger
        val vec32: Vec3 = minecart.getPassengerMoveIntent()
        if (entity is ServerPlayer && vec32.lengthSqr() > 0.0) {
            val vec33 = vec32.normalize()
            val d = vec3.horizontalDistanceSqr()
            if (vec33.lengthSqr() > 0.0 && d < 0.01) {
                return vec3.add(Vec3(vec33.x, 0.0, vec33.z).normalize().scale(0.001))
            }
        } else {
            minecart.setPassengerMoveIntent(Vec3.ZERO)
        }
        return vec3
    }

    private fun calculateHaltTrackSpeed(vec3: Vec3, blockState: BlockState): Vec3 {
        if (!blockState.`is`(Blocks.POWERED_RAIL) || blockState.getValue(PoweredRailBlock.POWERED)) {
            return vec3
        }
        if (vec3.length() < 0.03) {
            return Vec3.ZERO
        }
        return vec3.scale(0.5)
    }

    private fun calculateBoostTrackSpeed(vec3: Vec3, blockPos: BlockPos, blockState: BlockState): Vec3 {
        if (!blockState.`is`(Blocks.POWERED_RAIL) || !blockState.getValue(PoweredRailBlock.POWERED)) {
            return vec3
        }
        if (vec3.length() > 0.01) {
            return vec3.normalize().scale(vec3.length() + 0.06)
        }
        val vec32: Vec3 = minecart.getRedstoneDirection(blockPos)
        if (vec32.lengthSqr() <= 0.0) {
            return vec3
        }
        return vec32.scale(vec3.length() + 0.2)
    }

    override fun stepAlongTrack(blockPos: BlockPos, railShape: RailShape, d: Double): Double {
        var d = d
        if (d < 1.0E-5) {
            return 0.0
        }
        val vec3 = this.position()
        val pair = AbstractMinecart.exits(railShape)
        val vec3i = pair.first
        val vec3i2 = pair.second
        var vec32: Vec3 = this.deltaMovement.horizontal()
        if (vec32.length() < 1.0E-5) {
            this.deltaMovement = Vec3.ZERO
            return 0.0
        }
        val bl = vec3i.y != vec3i2.y
        val vec33: Vec3 = Vec3(vec3i2).scale(0.5).horizontal()
        var vec34: Vec3 = Vec3(vec3i).scale(0.5).horizontal()
        if (vec32.dot(vec34) < vec32.dot(vec33)) {
            vec34 = vec33
        }
        var vec35: Vec3 = blockPos.getBottomCenter().add(vec34).add(0.0, 0.1, 0.0).add(vec34.normalize().scale(1.0E-5))
        if (bl && !this.isDecending(vec32, railShape)) {
            vec35 = vec35.add(0.0, 1.0, 0.0)
        }
        val vec36 = vec35.subtract(this.position()).normalize()
        vec32 = vec36.scale(vec32.length() / vec36.horizontalDistance())
        var vec37 = vec3.add(vec32.normalize().scale(d * (if (bl) Mth.SQRT_OF_TWO else 1.0f).toDouble()))
        if (vec3.distanceToSqr(vec35) <= vec3.distanceToSqr(vec37)) {
            d = vec35.subtract(vec37).horizontalDistance()
            vec37 = vec35
        } else {
            d = 0.0
        }
        minecart.move(MoverType.SELF, vec37.subtract(vec3))
        val blockPos2 = BlockPos.containing(vec37)
        val blockState = level().getBlockState(blockPos2)
        if (bl && BaseRailBlock.isRail(blockState)) {
            this.setPos(vec37)
        }
        if (position().distanceTo(vec3) < 1.0E-5 && vec37.distanceTo(vec3) > 1.0E-5) {
            this.deltaMovement = Vec3.ZERO
            return 0.0
        }
        this.deltaMovement = vec32
        return d
    }

    override fun getMaxSpeed(): Double {
        return level().gameRules.getInt(GameRules.RULE_MINECART_MAX_SPEED)
            .toDouble() * (if (minecart.isInWater) 0.5 else 1.0) / 20.0
    }

    private fun isDecending(vec3: Vec3, railShape: RailShape): Boolean {
        return when (railShape) {
            RailShape.ASCENDING_EAST -> {
                if (vec3.x < 0.0) {
                    true
                }
                false
            }

            RailShape.ASCENDING_WEST -> {
                if (vec3.x > 0.0) {
                    true
                }
                false
            }

            RailShape.ASCENDING_NORTH -> {
                if (vec3.z > 0.0) {
                    true
                }
                false
            }

            RailShape.ASCENDING_SOUTH -> {
                if (vec3.z < 0.0) {
                    true
                }
                false
            }

            else -> false
        }
    }

    override fun getSlowdownFactor(): Double {
        return if (minecart.isVehicle) 0.997 else 0.975
    }

    @JvmRecord
    data class MinecartStep(
        val position: Vec3,
        val movement: Vec3,
        val yRot: Float,
        val xRot: Float,
        val weight: Float
    ) {
        companion object {
            val ROTATION_STREAM_CODEC: StreamCodec<ByteBuf, Float> =
                ByteBufCodecs.BYTE.map({ b: Byte -> uncompressRotation(b) }, { f: Float -> compressRotation(f) })
            val STREAM_CODEC: StreamCodec<ByteBuf, MinecartStep> =
                StreamCodec.composite<ByteBuf, MinecartStep, Vec3, Vec3, Float, Float, Float>(
                    Vec3.STREAM_CODEC,
                    MinecartStep::position,
                    Vec3.STREAM_CODEC,
                    MinecartStep::movement,
                    ROTATION_STREAM_CODEC,
                    MinecartStep::yRot,
                    ROTATION_STREAM_CODEC,
                    MinecartStep::xRot,
                    ByteBufCodecs.FLOAT,
                    MinecartStep::weight
                ) { position: Vec3, movement: Vec3, yRot: Float, xRot: Float, weight: Float ->
                    MinecartStep(
                        position,
                        movement,
                        yRot,
                        xRot,
                        weight
                    )
                }
            var ZERO: MinecartStep = MinecartStep(Vec3.ZERO, Vec3.ZERO, 0.0f, 0.0f, 0.0f)

            private fun compressRotation(f: Float): Byte {
                return Mth.floor(f * 256.0f / 360.0f).toByte()
            }

            private fun uncompressRotation(b: Byte): Float {
                return b.toFloat() * 360.0f / 256.0f
            }
        }
    }

    @JvmRecord
    internal data class StepPartialTicks(
        val partialTicksInStep: Float,
        val currentStep: MinecartStep,
        val previousStep: MinecartStep
    )

    internal class TrackIteration {
        var movementLeft: Double = 0.0
        var firstIteration: Boolean = true
        var hasGainedSlopeSpeed: Boolean = false
        var hasHalted: Boolean = false
        var hasBoosted: Boolean = false

        fun shouldIterate(): Boolean {
            return this.firstIteration || this.movementLeft > 1.0E-5
        }
    }

    companion object {
        const val POS_ROT_LERP_TICKS: Int = 3
        const val ON_RAIL_Y_OFFSET: Double = 0.1
        fun lerp(d: Double, vec3: Vec3, vec32: Vec3): Vec3 {
            return Vec3(Mth.lerp(d, vec3.x, vec32.x), Mth.lerp(d, vec3.y, vec32.y), Mth.lerp(d, vec3.z, vec32.z))
        }
    }
}

