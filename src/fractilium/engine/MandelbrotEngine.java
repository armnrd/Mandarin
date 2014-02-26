package fractilium.engine;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

/**
 *
 * @author androkot
 */
public class MandelbrotEngine {

    public static interface EventHandler {

        public void renderingBegun();

        public void regionRendered(int startIdx, int endIdx);

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
        private final BigDecimal planeMinX, planeMaxX, planeMinY, planeMaxY;
        private final int imgWidth, imgHeight, maxIter, prec, sampleSize;
        private final boolean useArbPrec;
        private final MandelbrotVariant var;
        private final ColouringMethod colMeth;

        private Parameters() {
            planeMinX = planeMaxX = planeMinY = planeMaxY = null;
            imgWidth = imgHeight = maxIter = prec = sampleSize = 0;
            useArbPrec = false;
            var = null;
            colMeth = null;
        }

        public Parameters(BigDecimal plMinX, BigDecimal plMaxX, BigDecimal plMinY, BigDecimal plMaxY, int imgWidth, int imgHeight, int maxIter, boolean useArbPrec, int prec, int sampleSize, MandelbrotVariant mbrotVar, ColouringMethod colMeth) {
            planeMinX = plMinX;
            planeMaxX = plMaxX;
            planeMinY = plMinY;
            planeMaxY = plMaxY;
            this.imgWidth = imgWidth;
            this.imgHeight = imgHeight;
            this.maxIter = maxIter;
            this.useArbPrec = useArbPrec;
            this.prec = prec; // prec is the precision in bits not decimal digits!
            this.sampleSize = sampleSize;
            this.var = mbrotVar;
            this.colMeth = colMeth;
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
            return maxIter;
        }

        public int useArbitraryPrecision() {
            return useArbPrec ? 1 : 0;
        }

        public int precision() {
            return prec;
        }

        public int sampleSize() {
            return sampleSize;
        }

        public int mandelbrotVariant() {
            return var.toNumber();
        }

        public int colouringMethod() {
            return colMeth.toNumber();
        }
    }

    private static ByteBuffer b;
    private static EventHandler h;
    private static Parameters p;
    private static Statistics s;
    
    /**
     * Stats returned have the following format: min:mean:max:points:time. The
     * event handler handles communication between the native renderer and Java
     * code until rendering is complete.
     */
    private static native String _renderMandelbrot(ByteBuffer b, String plMinX, String plMaxX,
            String plMinY, String plMaxY, int imgWidth, int imgHeight, int maxIter,
            int useArbPrec, int prec, int sampleSize, int mbrotVar, int colMeth);

    public static void initialize(ByteBuffer b, Parameters p, EventHandler h) {
        MandelbrotEngine.b = b;
        MandelbrotEngine.p = p;
        MandelbrotEngine.h = h;
        System.out.println("MandelbrotEngine initialized.");
    }

    public static void changeRenderingRegion(BigDecimal plMinX, BigDecimal plMaxX, BigDecimal plMinY, BigDecimal plMaxY) {
    }

    public static void changeColouringMethod(Parameters.ColouringMethod colMeth) {
    }

    public static void startRendering() {
        Thread t;

        /*t = new Thread(new Runnable() {
            @Override
            public void run() {
                String stats, temp[];

                stats = _renderMandelbrot(b, p.planeMinX(),
                        p.planeMaxX(), p.planeMinY(), p.planeMaxY(), p.imageWidth(),
                        p.imageHeight(), p.maxIterations(), p.useArbitraryPrecision(),
                        p.precision(), p.sampleSize(), p.mandelbrotVariant(), p.colouringMethod());
                temp = stats.split(":");
                s = new Statistics(Integer.parseInt(temp[0]), Double.parseDouble(temp[1]),
                        Integer.parseInt(temp[2]), Integer.parseInt(temp[3]), Double.parseDouble(temp[4]));
                h.statsGenerated();
            }
        });

        t.start(); */
        System.out.println("Rendering begun.");
    }
    
    public static void pauseRendering() {
        
    }
    
    public static void resumeRendering() {
        
    }
    
    public static void stopRendering() {
        
    }

    public static Statistics getStatistics() {
        return s;
    }

    public static Parameters getParameters() {
        return p;
    }

    public static void cleanup() {
    }

    private MandelbrotEngine() {
    }
}
