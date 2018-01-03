package ng_labo.nglogger;

/**
 * imported from
 *
 * Created by n-ogawa on 2017/09/15.
 */

public class StepDetector {

    private final int ACCEL_RING_SIZE; //= 50;
    private final int VEL_RING_SIZE; //= 10;

    // change this threshold according to your sensitivity preferences
    private final float STEP_THRESHOLD;// = 50f;

    private final int STEP_DELAY_NS;// = 250000000;

    private int accelRingCounter = 0;
    private float[] accelRingX;// = new float[ACCEL_RING_SIZE];
    private float[] accelRingY;// = new float[ACCEL_RING_SIZE];
    private float[] accelRingZ;// = new float[ACCEL_RING_SIZE];
    private int velRingCounter = 0;
    private float[] velRing;// = new float[VEL_RING_SIZE];
    private long lastStepTimeNs = 0;
    private float oldVelocityEstimate = 0;

    private LazyService listener;

    public StepDetector(int accel_ring_size,int vel_ring_size,float step_threshold,int step_delay_ns) {
        ACCEL_RING_SIZE = accel_ring_size;
        VEL_RING_SIZE = vel_ring_size;
        STEP_THRESHOLD = step_threshold;
        STEP_DELAY_NS = step_delay_ns;
        accelRingX = new float[ACCEL_RING_SIZE];
        accelRingY = new float[ACCEL_RING_SIZE];
        accelRingZ = new float[ACCEL_RING_SIZE];
        velRing = new float[VEL_RING_SIZE];
    }
    public StepDetector(){
        this(50, 10, 50f, 250000000);
    }

    public void registerListener(LazyService listener) {
        this.listener = listener;
    }


    public void updateAccel(long timeNs, float x, float y, float z) {
        float[] currentAccel = new float[3];
        currentAccel[0] = x;
        currentAccel[1] = y;
        currentAccel[2] = z;

        // First step is to update our guess of where the global z vector is.
        accelRingCounter++;
        accelRingX[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[0];
        accelRingY[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[1];
        accelRingZ[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[2];

        float[] worldZ = new float[3];
        worldZ[0] = MathFunc.sum(accelRingX) / ACCEL_RING_SIZE;
        worldZ[1] = MathFunc.sum(accelRingY) / ACCEL_RING_SIZE;
        worldZ[2] = MathFunc.sum(accelRingZ) / ACCEL_RING_SIZE;

        float normalization_factor = MathFunc.norm(worldZ);

        worldZ[0] = worldZ[0] / normalization_factor;
        worldZ[1] = worldZ[1] / normalization_factor;
        worldZ[2] = worldZ[2] / normalization_factor;

        float currentZ = MathFunc.dot(worldZ, currentAccel) - normalization_factor;
        velRingCounter++;
        velRing[velRingCounter % VEL_RING_SIZE] = currentZ;

        float velocityEstimate = MathFunc.sum(velRing);

        if (velocityEstimate > STEP_THRESHOLD && oldVelocityEstimate <= STEP_THRESHOLD
                && (timeNs - lastStepTimeNs > STEP_DELAY_NS)) {
            listener.step(timeNs);
            lastStepTimeNs = timeNs;
        }
        oldVelocityEstimate = velocityEstimate;
    }
}