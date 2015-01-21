/*
 *!------------------------------------------------------------------------------------------------!
 *  MandelbrotEngine.java
 *
 *  Functions in this class render Mandelbrot fractals.
 *
 *  Creation date: 04/12/2012
 *  Author: Arindam Biswas <arindam dot b at fastmail dot fm>
 *!------------------------------------------------------------------------------------------------!
 */

package info.arindam.fractilium.engine;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Arindam Biswas <arindam dot b at fastmail dot fm>
 */
public class MandelbrotEngine {

    public static interface EventHandler {

        public void renderingBegun();

        public void regionRendered(Rectangle region);

        public void renderingEnded();

        public void errorOccurred();

        public void statsGenerated();
    }

    public static class Statistics {

        public int minIterations, maxIterations, convergentPoints;
        public double meanIterations, renderingTime;

        private Statistics() {
            minIterations = maxIterations = convergentPoints = 0;
            meanIterations = renderingTime = 0;
        }

        public Statistics(int minIter, double meanIter, int maxIter, int conPts, double renTime) {
            minIterations = minIter;
            maxIterations = maxIter;
            meanIterations = meanIter;
            convergentPoints = conPts;
            renderingTime = renTime;
        }

    }

    public static class Parameters {

        public static enum ColouringMethod {

            REGULAR(0),
            RED(1),
            GREEN(2),
            BLUE(3);
            private final int n;

            ColouringMethod(int n) {
                this.n = n;
            }

            public int toNumber() {
                return n;
            }
        }
        private final BigDecimal planeMinX, planeMaxX, planeMinY, planeMaxY,
                planeXUnit, planeYUnit;
        private final int imgWidth, imgHeight, maxIters;
        private final ColouringMethod colMethod;

        private Parameters() {
            planeMinX = planeMaxX = planeMinY = planeMaxY = planeXUnit
                    = planeYUnit = null;
            imgWidth = imgHeight = maxIters = 0;
            colMethod = null;
        }

        public Parameters(BigDecimal plMinX, BigDecimal plMaxX, BigDecimal
                plMinY, BigDecimal plMaxY, int imgWidth, int imgHeight,
                int maxIter, ColouringMethod colMeth) {
            planeMinX = plMinX;
            planeMaxX = plMaxX;
            planeMinY = plMinY;
            planeMaxY = plMaxY;
            planeXUnit = plMaxX.subtract(plMinX, mathCont).divide(new
                BigDecimal(imgWidth, mathCont), mathCont);
            planeYUnit = plMaxY.subtract(plMinY, mathCont).divide(new
                BigDecimal(imgHeight, mathCont), mathCont);
            this.imgWidth = imgWidth;
            this.imgHeight = imgHeight;
            this.maxIters = maxIter;
            this.colMethod = colMeth;
        }
    }

    private static int coreCount, threadCount, maxPrecision, rawDataIters[], pixelArray[];
    private static double rawDataZR[], rawDataZI[];
    private static BufferedImage image;
    private static EventHandler handler;
    private static Parameters params;
    private static Statistics stats;
    private static MathContext mathCont;
    private static ArrayBlockingQueue<Rectangle> regionQueue;

    private void MandelbrotEngine() {
    }

    public static void initialize(EventHandler h, int maxPrecision) {
        MandelbrotEngine.handler = h;
        MandelbrotEngine.coreCount = Runtime.getRuntime().availableProcessors();
        MandelbrotEngine.maxPrecision = maxPrecision;
        mathCont = new MathContext(maxPrecision, RoundingMode.HALF_EVEN);
    }

    public static void setParameters(MandelbrotEngine.Parameters p) {
        MandelbrotEngine.params = p;
    }

    public static void startRendering() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                Object lock = new Object();
                image = new BufferedImage(params.imgWidth, params.imgHeight, BufferedImage.TYPE_INT_RGB);
                pixelArray = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
                if (rawDataIters == null || rawDataIters.length != pixelArray.length) {
                    rawDataIters = new int[pixelArray.length];
                    rawDataZR = new double[pixelArray.length];
                    rawDataZI = new double[pixelArray.length];
                }
                stats = new Statistics(params.maxIters, 0, 0, 0, System.nanoTime());
                launchThreads(lock);
                try {
                    synchronized (lock) {
                        lock.wait();
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(MandelbrotEngine.class.getName()).log(Level.SEVERE, null, ex);
                }
                stats.renderingTime = System.nanoTime() - stats.renderingTime;
                stats.renderingTime /= 1000000;
                postProcess();
                handler.renderingEnded();
                handler.statsGenerated();
            }
        }).start();
        handler.renderingBegun();
    }

    public static void pauseRendering() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void resumeRendering() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void stopRendering() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static BufferedImage getImage() {
        return image;
    }

    public static Statistics getStatistics() {
        return stats;
    }

    public static void cleanup() {
        image = null;
        pixelArray = null;
        rawDataIters = null;
        rawDataZR = null;
        rawDataZI = null;
        handler = null;
        params = null;
        stats = null;
        mathCont = null;
        regionQueue = null;
    }

    private static void launchThreads(final Object lock) {
        int stripWidth = params.imgWidth / (coreCount * 10) + 1, resdlWidth = params.imgWidth % stripWidth;

        int startIndex = 0;
        regionQueue = new ArrayBlockingQueue<>(params.imgWidth / stripWidth + 1);
        do {
            regionQueue.add(new Rectangle(startIndex, 0, stripWidth, params.imgHeight));
            startIndex += stripWidth;
        } while (startIndex + stripWidth <= params.imgWidth);

        if (resdlWidth != 0) {
            regionQueue.add(new Rectangle(startIndex, 0, resdlWidth, params.imgHeight));
        }

        threadCount = 0;
        for (int i = 1; i <= coreCount; i++) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    Rectangle region = regionQueue.poll();
                    while (region != null) {
                        renderRegionPrimitive(region);
                        handler.regionRendered(region);
                        region = regionQueue.poll();
                    }

                    if (threadCount == 1) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                    threadCount--;
                }
            }).start();
            threadCount++;
        }
    }

    private static void postProcess() {

    }

    private static int pixelColour(int iters, double zR, double zI) {
        float ratio = iters / (float) params.maxIters;
        return Color.HSBtoRGB((float) Math.cbrt(ratio)/ 2 + 0.25f, ratio * ratio, 0.8f);
    }

    private static void renderRegionPrimitive(Rectangle region) {
        int i, j, k;
        double zR, cR, aR, zI, cI, aI, planeXUnit, planeYUnit, temp;

        planeXUnit = params.planeXUnit.doubleValue();
        planeYUnit = params.planeYUnit.doubleValue();
        aR = params.planeMinX.doubleValue() + region.x * planeXUnit;
        aI = params.planeMaxY.doubleValue() - region.y * planeYUnit;
        for (i = 0; i < region.width; i++) {
            for (j = 0; j < region.height; j++) {
                zR = cR = aR + planeXUnit * i;
                zI = cI = aI - planeYUnit * j;
                k = 0;

                while (k < params.maxIters) {
                    if (zR * zR + zI * zI > (double) 25) {
                        break;
                    }

                    temp = zR;
                    zR = zR * zR - zI * zI + cR;
                    zI = 2 * temp * zI + cI;
                    k++;
                }
                int dataIdx = (region.y + j) * params.imgWidth + region.x + i;
                rawDataIters[dataIdx] = k;
                rawDataZR[dataIdx] = zR;
                rawDataZI[dataIdx] = zI;
            }
        }

    }
}
