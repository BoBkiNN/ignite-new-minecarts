package xyz.bobkinn.minecarts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

public abstract class MinecartBehavior {
    protected final AbstractMinecart minecart;

    protected MinecartBehavior(AbstractMinecart abstractMinecart) {
        this.minecart = abstractMinecart;
    }

    public void lerpTo(double d, double e, double f, float g, float h, int i) {
        this.setPos(d, e, f);
        this.setYRot(g % 360.0f);
        this.setXRot(h % 360.0f);
    }

    public double lerpTargetX() {
        return this.getX();
    }

    public double lerpTargetY() {
        return this.getY();
    }

    public double lerpTargetZ() {
        return this.getZ();
    }

    public float lerpTargetXRot() {
        return this.getXRot();
    }

    public float lerpTargetYRot() {
        return this.getYRot();
    }

    public void lerpMotion(double d, double e, double f) {
        this.setDeltaMovement(d, e, f);
    }

    public abstract void tick();

    public Level level() {
        return this.minecart.level();
    }

    public abstract void moveAlongTrack();

    public abstract double stepAlongTrack(BlockPos var1, RailShape var2, double var3);

    public Vec3 getDeltaMovement() {
        return this.minecart.getDeltaMovement();
    }

    public void setDeltaMovement(Vec3 vec3) {
        this.minecart.setDeltaMovement(vec3);
    }

    public void setDeltaMovement(double d, double e, double f) {
        this.minecart.setDeltaMovement(d, e, f);
    }

    public Vec3 position() {
        return this.minecart.position();
    }

    public double getX() {
        return this.minecart.getX();
    }

    public double getY() {
        return this.minecart.getY();
    }

    public double getZ() {
        return this.minecart.getZ();
    }

    public void setPos(Vec3 vec3) {
        this.minecart.setPos(vec3);
    }

    public void setPos(double d, double e, double f) {
        this.minecart.setPos(d, e, f);
    }

    public float getXRot() {
        return this.minecart.getXRot();
    }

    public void setXRot(float f) {
        this.minecart.setXRot(f);
    }

    public float getYRot() {
        return this.minecart.getYRot();
    }

    public void setYRot(float f) {
        this.minecart.setYRot(f);
    }

    public Direction getMotionDirection() {
        return this.minecart.getDirection();
    }

    public Vec3 getKnownMovement(Vec3 vec3) {
        return vec3;
    }

    public abstract double getMaxSpeed();

    public abstract double getSlowdownFactor();
}