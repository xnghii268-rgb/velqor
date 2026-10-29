package com.velqor.app.ui;

public class SpringSolver {
    private float mMass = 1.0f;
    private float mStiffness = 180.0f;
    private float mDamping = 18.0f;
    private float mCurrentValue = 0.0f;
    private float mTargetValue = 0.0f;
    private float mVelocity = 0.0f;
    private float mRestThreshold = 0.001f;

    public SpringSolver() {}

    public SpringSolver(float mass, float stiffness, float damping) {
        this.mMass = mass;
        this.mStiffness = stiffness;
        this.mDamping = damping;
    }

    public void setParams(float mass, float stiffness, float damping) {
        this.mMass = Math.max(0.001f, mass);
        this.mStiffness = Math.max(0.001f, stiffness);
        this.mDamping = Math.max(0.001f, damping);
    }

    public void setTarget(float target) {
        this.mTargetValue = target;
    }

    public float getTarget() {
        return mTargetValue;
    }

    public void setValue(float value) {
        this.mCurrentValue = value;
    }

    public float getValue() {
        return mCurrentValue;
    }

    public void setVelocity(float velocity) {
        this.mVelocity = velocity;
    }

    public float getVelocity() {
        return mVelocity;
    }

    public boolean isAtRest() {
        return Math.abs(mVelocity) < mRestThreshold && Math.abs(mTargetValue - mCurrentValue) < mRestThreshold;
    }

    public float update(float dt) {
        if (dt <= 0.0f) return mCurrentValue;
        float clampedDt = Math.min(dt, 0.064f);
        float subSteps = 4;
        float stepDt = clampedDt / subSteps;
        for (int i = 0; i < subSteps; i++) {
            float springForce = -mStiffness * (mCurrentValue - mTargetValue);
            float dampingForce = -mDamping * mVelocity;
            float acceleration = (springForce + dampingForce) / mMass;
            mVelocity += acceleration * stepDt;
            mCurrentValue += mVelocity * stepDt;
        }
        if (isAtRest()) {
            mCurrentValue = mTargetValue;
            mVelocity = 0.0f;
        }
        return mCurrentValue;
    }

    public static int computeSnapIndex(float currentPos, float velocity, float[] tabPositions) {
        if (tabPositions == null || tabPositions.length == 0) return 0;
        float predictedPos = currentPos + velocity * 0.16f;
        int bestIndex = 0;
        float minDiff = Float.MAX_VALUE;
        for (int i = 0; i < tabPositions.length; i++) {
            float diff = Math.abs(tabPositions[i] - predictedPos);
            if (diff < minDiff) {
                minDiff = diff;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    public static float applyRubberBand(float offset, float maxRange, float dimension) {
        if (maxRange <= 0.0f || dimension <= 0.0f) return offset;
        if (offset < 0.0f) {
            return -applyRubberBandHelper(-offset, dimension);
        } else if (offset > maxRange) {
            return maxRange + applyRubberBandHelper(offset - maxRange, dimension);
        }
        return offset;
    }

    private static float applyRubberBandHelper(float overscroll, float dimension) {
        return (overscroll * 0.55f * dimension) / (dimension + 0.55f * overscroll);
    }
}
