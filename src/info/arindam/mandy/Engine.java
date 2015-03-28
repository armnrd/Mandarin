/*
 *!------------------------------------------------------------------------------------------------!
 *  Engine.java
 *
 *  The class contains the actual code for rendering Mandelbrot fractals.
 *
 *  Creation date: 04/12/2012
 *  Author: Arindam Biswas <arindam dot b at eml dot cc>
 *!------------------------------------------------------------------------------------------------!
 */

package info.arindam.mandy;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Arindam Biswas <arindam dot b at eml dot cc>
 */
public class Engine {
    public static interface Listener {

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
        private final double planeMinX, planeMaxX, planeMinY, planeMaxY, planeXUnit, planeYUnit;
        private final int imgWidth, imgHeight, maxIters;
        private final ColouringMethod colMethod;

        private Parameters() {
            planeMinX = planeMaxX = planeMinY = planeMaxY = planeXUnit = planeYUnit = 0;
            imgWidth = imgHeight = maxIters = 0;
            colMethod = null;
        }

        public Parameters(double plMinX, double plMaxX, double plMinY, double plMaxY, int imgWidth,
                int imgHeight, int maxIter, ColouringMethod colMeth) {
            planeMinX = plMinX;
            planeMaxX = plMaxX;
            planeMinY = plMinY;
            planeMaxY = plMaxY;
            planeXUnit = (plMaxX - plMinX) / imgWidth;
            planeYUnit = (plMaxY - plMinY) / imgHeight;
            this.imgWidth = imgWidth;
            this.imgHeight = imgHeight;
            this.maxIters = maxIter;
            this.colMethod = colMeth;
        }
    }

    private static int coreCount, threadCount, buffer[];
    private static BufferedImage image;
    private static Listener handler;
    private static Parameters params;
    private static Statistics stats;
    private static ArrayBlockingQueue<Rectangle> regionQueue;

    private Engine() {
    }

    public static void initialize(Listener h) {
        Engine.handler = h;
        Engine.coreCount = Runtime.getRuntime().availableProcessors();
    }

    public static void setParameters(Engine.Parameters p) {
        Engine.params = p;

    }

    public static void startRendering() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                Object lock = new Object();
                image = new BufferedImage(params.imgWidth, params.imgHeight, BufferedImage.TYPE_INT_RGB);
                buffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
                stats = new Statistics(params.maxIters, 0, 0, 0, System.nanoTime());
                launchThreads(lock);
                try {
                    synchronized (lock) {
                        lock.wait();
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(Engine.class.getName()).log(Level.SEVERE, null, ex);
                }
                stats.renderingTime = System.nanoTime() - stats.renderingTime;
                stats.renderingTime /= 1000000;
                stats.meanIterations /= buffer.length;
                handler.renderingEnded();
                handler.statsGenerated();
            }
        }).start();
        handler.renderingBegun();
    }

    public static BufferedImage getImage() {
        return image;
    }

    public static Statistics getStatistics() {
        return stats;
    }

    public static void cleanup() {
        image = null;
        buffer = null;
        handler = null;
        params = null;
        stats = null;
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

    private static int pixelColour(int iters, double zR, double zI) {
        if (iters == params.maxIters) {
            return 0;
        }
        float ratio = (float) (iters - Math.log(Math.log(Math.sqrt(zR * zR + zI * zI)))) / params.maxIters;
        return Color.HSBtoRGB(ratio, 1.0f, 0.8f);
    }

    private static void renderRegionPrimitive(Rectangle region) {
        int k, convCount = 0, totalIterationCount = 0, minIterationCount = params.maxIters,
                maxIterationCount = 1;
        double zR, cR, aR, zI, cI, aI, planeXUnit, planeYUnit, temp;

        planeXUnit = params.planeXUnit;
        planeYUnit = params.planeYUnit;
        aR = params.planeMinX + region.x * planeXUnit;
        aI = params.planeMaxY - region.y * planeYUnit;
        for (int i = 0; i < region.width; i++) {
            for (int j = 0; j < region.height; j++) {
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
                if (k < params.maxIters) {
                    convCount++;
                }
                if (k > maxIterationCount) {
                    maxIterationCount = k;
                }
                if (k < minIterationCount) {
                    minIterationCount = k;
                }
                totalIterationCount += k;
                int dataIdx = (region.y + j) * params.imgWidth + region.x + i;
                buffer[dataIdx] = pixelColour(k, zR, zI);
            }
        }
        synchronized (stats) {
        stats.convergentPoints += convCount;
        if (stats.maxIterations < maxIterationCount) {
            stats.maxIterations = maxIterationCount;
        }
        if (stats.minIterations > minIterationCount) {
            stats.minIterations = minIterationCount;
        }
        stats.meanIterations += totalIterationCount;
        }
    }
}
