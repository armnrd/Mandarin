package fractilium.engine;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * @author androkot
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

        public final int minIterations, maxIterations, convergentPoints;
        public final double meanIterations, renderingTime;

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

        public static enum MandelbrotVariant {

            REGULAR(0),
            BUDDHABROT(1);
            private final int n;

            MandelbrotVariant(int n) {
                this.n = n;
            }

            public int toNumber() {
                return n;
            }
        }

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
        private final int imgWidth, imgHeight, maxIters, sampleSize;
        private final MandelbrotVariant variant;
        private final ColouringMethod colMethod;

        private Parameters() {
            planeMinX = planeMaxX = planeMinY = planeMaxY = planeXUnit
                    = planeYUnit = null;
            imgWidth = imgHeight = maxIters = sampleSize = 0;
            variant = null;
            colMethod = null;
        }

        public Parameters(BigDecimal plMinX, BigDecimal plMaxX, BigDecimal plMinY, BigDecimal plMaxY, int imgWidth, int imgHeight, int maxIter, boolean useArbPrec, int prec, int sampleSize, MandelbrotVariant mbrotVar, ColouringMethod colMeth) {
            planeMinX = plMinX;
            planeMaxX = plMaxX;
            planeMinY = plMinY;
            planeMaxY = plMaxY;
            planeXUnit = plMaxX.subtract(plMinX, mathCont).divide(new BigDecimal(imgWidth, mathCont), mathCont);
            planeYUnit = plMaxY.subtract(plMinY, mathCont).divide(new BigDecimal(imgHeight, mathCont), mathCont);
            this.imgWidth = imgWidth;
            this.imgHeight = imgHeight;
            this.maxIters = maxIter;
            this.sampleSize = sampleSize;
            this.variant = mbrotVar;
            this.colMethod = colMeth;
        }

        public String planeMinX() {
            return planeMinX.toString();
        }

        public String planeMaxX() {
            return planeMaxX.toString();
        }

        public String planeMinY() {
            return planeMinY.toString();
        }

        public String planeMaxY() {
            return planeMaxY.toString();
        }

        public int imageWidth() {
            return imgWidth;
        }

        public int imageHeight() {
            return imgHeight;
        }

        public int maxIterations() {
            return maxIters;
        }

        public int sampleSize() {
            return sampleSize;
        }

        public int mandelbrotVariant() {
            return variant.toNumber();
        }

        public int colouringMethod() {
            return colMethod.toNumber();
        }
    }

    private static BufferedImage image;
    private static EventHandler handler;
    private static Parameters params;
    private static Statistics stats;
    private static MathContext mathCont;
    private static int coreCount, threadCount;
    private static ArrayBlockingQueue<Rectangle> queue;

    private void MandelbrotEngine() {
    }

    public static void initialize(EventHandler h, int precision) {
        MandelbrotEngine.handler = h;
        MandelbrotEngine.coreCount = Runtime.getRuntime().availableProcessors();
        mathCont = new MathContext(precision, RoundingMode.HALF_EVEN);
    }

    public static void setParameters(MandelbrotEngine.Parameters p) {
        MandelbrotEngine.params = p;
    }

    public static void changeRenderingRegion(BigDecimal plMinX, BigDecimal plMaxX, BigDecimal plMinY, BigDecimal plMaxY) {
    }

    public static void changeColouringMethod(Parameters.ColouringMethod colMeth) {
    }

    public static void startRendering() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                launchThreads();
            }
        }).start();
        handler.renderingBegun();
    }

    private static void launchThreads() {
        image = new BufferedImage(params.imgWidth, params.imgHeight, BufferedImage.TYPE_INT_RGB);
        int stripWidth = params.imgWidth / (coreCount * 10) + 1, resdlWidth = params.imgWidth % stripWidth;

        int startIndex = 0;
        queue = new ArrayBlockingQueue<>(params.imgWidth / stripWidth + 1);
        do {
            queue.add(new Rectangle(startIndex, 0, stripWidth, params.imgHeight));
            startIndex += stripWidth;
        } while (startIndex + stripWidth <= params.imgWidth);

        if (resdlWidth != 0) {
            queue.add(new Rectangle(startIndex, 0, resdlWidth, params.imgHeight));
        }

        final Object lock = new Object();
        threadCount = 0;
        for (int i = 1; i <= coreCount; i++) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    Rectangle region = queue.poll();
                    while (region != null) {
                        renderRegionPrimitive(region);
                        handler.regionRendered(region);
                        region = queue.poll();
                    }

                    if (threadCount == 1) {
                        postProcess();
                    }
                    threadCount--;
                }
            }).start();
            threadCount++;
        }
    }

    private static void postProcess() {
        handler.renderingEnded();
    }

    private static int colour(int iters) {
        double ratio = iters / (double) params.maxIters;
        return (int) (Math.sqrt(ratio) * ratio * (ratio / 2 + 0.5) * 0xf21f5f);
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
                
                image.setRGB(region.x + i, region.y + j, colour(k));
            }
        }
    }

    public static void pauseRendering() {

    }

    public static void resumeRendering() {

    }

    public static void stopRendering() {

    }

    public static BufferedImage getImage() {
        return image;
    }

    public static Statistics getStatistics() {
        return stats;
    }

    public static Parameters getParameters() {
        return params;
    }

    public static void cleanup() {
    }

    private void renderMandebrotPrimitive() {

    }
}
