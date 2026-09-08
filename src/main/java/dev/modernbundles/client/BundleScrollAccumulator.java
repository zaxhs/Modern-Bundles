package dev.modernbundles.client;

final class BundleScrollAccumulator {
    private double accumulatedX;
    private double accumulatedY;

    Step add(double xOffset, double yOffset) {
        if (this.accumulatedX != 0.0 && Math.signum(xOffset) != Math.signum(this.accumulatedX)) {
            this.accumulatedX = 0.0;
        }
        if (this.accumulatedY != 0.0 && Math.signum(yOffset) != Math.signum(this.accumulatedY)) {
            this.accumulatedY = 0.0;
        }

        this.accumulatedX += xOffset;
        this.accumulatedY += yOffset;
        int x = (int)this.accumulatedX;
        int y = (int)this.accumulatedY;
        this.accumulatedX -= x;
        this.accumulatedY -= y;
        return new Step(x, y);
    }

    void reset() {
        this.accumulatedX = 0.0;
        this.accumulatedY = 0.0;
    }

    record Step(int x, int y) {
    }
}

