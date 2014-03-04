/* 
 * !---------------------------------------------------------------------------!
 *   MandelbrotSettingsPanel.java
 * 
 *   Provides an interface to MandelbrotEngine. Can be used as a standalone
 *   panel.
 * 
 *   Creation date: 04/12/2012
 *   Author: Arindam Biswas <ari.bsws at gmail.com>
 * !---------------------------------------------------------------------------!
 */

package fractilium.gui.fsp;

import fractilium.Main;
import fractilium.engine.MandelbrotEngine;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Arindam Biswas <ari.bsws at gmail.com>
 */
public class MandelbrotSettingsPanel extends javax.swing.JPanel implements MandelbrotEngine.EventHandler {
    
    public static interface ParentContainer {
        
    }
    
    private static final int MAX_PRECISION = 200;
    private double imageRotation;
    private Rectangle outputSize;
    private MathContext mathCont, mathContDisp;
    private BigDecimal planeMinX, planeMinY, planeMaxX, planeMaxY, planeUnitX,
            planeUnitY, selMinX, selMinY, selMaxX, selMaxY;
    private Main m;
    private boolean renderInProgress;
    private MandelbrotEngine.Statistics stats;

    /**
     * Creates new form MandelbrotSettingsPanel
     */
    public MandelbrotSettingsPanel(Main m, Rectangle outputSize) {
        initComponents();
        if (!outputSize.isEmpty()) {
            this.outputSize = outputSize;
        } else {
            this.outputSize = new Rectangle(100, 100);

        }
        imageRotation = 0;
        mathCont = new MathContext(MAX_PRECISION, RoundingMode.HALF_EVEN);
        mathContDisp = new MathContext(4, RoundingMode.HALF_EVEN);
        planeMinX = new BigDecimal("-2", mathCont);
        planeMaxX = new BigDecimal("1", mathCont);
        planeMinY = new BigDecimal("-1.5", mathCont);
        planeMaxY = new BigDecimal("1.5", mathCont);
        setSelRenRegion(planeMinX, planeMaxX, planeMinY, planeMaxY);
        setCurRenRegion(planeMinX, planeMaxX, planeMinY, planeMaxY);
        this.m = m;
        stats = new MandelbrotEngine.Statistics(0, 0, 0, 0, 0);
        MandelbrotEngine.initialize(this, MAX_PRECISION);
    }

    private MandelbrotEngine.Parameters.MandelbrotVariant getMandelbrotVariant(String s) {
        switch (s) {
            case "Regular":
                return MandelbrotEngine.Parameters.MandelbrotVariant.REGULAR;
            case "Buddhabrot":
                return MandelbrotEngine.Parameters.MandelbrotVariant.BUDDHABROT;
            default:
                return MandelbrotEngine.Parameters.MandelbrotVariant.REGULAR;
        }
    }

    private MandelbrotEngine.Parameters.ColouringMethod getColouringMethod(String s) {
        switch (s) {
            case "Regular":
                return MandelbrotEngine.Parameters.ColouringMethod.REGULAR;
            case "Red":
                return MandelbrotEngine.Parameters.ColouringMethod.RED;
            case "Green":
                return MandelbrotEngine.Parameters.ColouringMethod.GREEN;
            case "Blue":
                return MandelbrotEngine.Parameters.ColouringMethod.BLUE;
            default:
                return MandelbrotEngine.Parameters.ColouringMethod.REGULAR;
        }
    }

    public void setOutputSize(Rectangle r) {
        outputSize = r;
        planeUnitX = planeMaxX.subtract(planeMinX, mathCont).divide(new BigDecimal(
                outputSize.width, mathCont), mathCont);
        planeUnitY = planeMaxY.subtract(planeMinY, mathCont).divide(new BigDecimal(
                outputSize.height, mathCont), mathCont);
        sSizeLabel.setText(r.width + "x" + r.height);
    }

    public void setSelectionRegion(Rectangle r) {
        BigDecimal selMinX, selMinY, selMaxX, selMaxY, temp1, temp2, temp3;
        double aspectRatio;

        selMinX = planeMinX.add(planeUnitX.multiply(new BigDecimal(r.x, mathCont)), mathCont);
        selMaxY = planeMaxY.subtract(planeUnitY.multiply(new BigDecimal(r.y, mathCont)), mathCont);
        selMaxX = selMinX.add(planeUnitX.multiply(new BigDecimal(r.width + 1, mathCont), mathCont));
        selMinY = selMaxY.subtract(planeUnitY.multiply(new BigDecimal(r.height + 1, mathCont), mathCont));

        aspectRatio = outputSize.width / (double) outputSize.height;
        temp3 = selMaxX.subtract(selMinX, mathCont).divide(selMaxY.subtract(selMinY, mathCont), mathCont);
        if (temp3.compareTo(new BigDecimal(aspectRatio, mathCont)) > 0) {
            temp1 = selMaxX.subtract(selMinX, mathCont);
            temp2 = selMaxY.subtract(selMinY, mathCont);
            temp1 = temp1.divide(new BigDecimal(aspectRatio, mathCont), mathCont).subtract(temp2, mathCont);
            temp1 = temp1.divide(new BigDecimal(2, mathCont), mathCont);
            selMinY = selMinY.subtract(temp1, mathCont);
            selMaxY = selMaxY.add(temp1, mathCont);
        } else if (temp3.compareTo(new BigDecimal(aspectRatio, mathCont)) < 0) {
            temp1 = selMaxX.subtract(selMinX, mathCont);
            temp2 = selMaxY.subtract(selMinY, mathCont);
            temp1 = temp2.multiply(new BigDecimal(aspectRatio, mathCont), mathCont).subtract(temp1, mathCont);
            temp1 = temp1.divide(new BigDecimal(2, mathCont), mathCont);
            selMinX = selMinX.subtract(temp1, mathCont);
            selMaxX = selMaxX.add(temp1, mathCont);
        }
        setSelRenRegion(selMinX, selMaxX, selMinY, selMaxY);
    }

    private void setCurRenRegion(BigDecimal planeMinX, BigDecimal planeMaxX, BigDecimal planeMinY, BigDecimal planeMaxY) {
        this.planeMinX = planeMinX;
        this.planeMaxX = planeMaxX;
        this.planeMinY = planeMinY;
        this.planeMaxY = planeMaxY;
        planeUnitX = planeMaxX.subtract(planeMinX, mathCont).divide(new BigDecimal(outputSize.width + 1, mathCont), mathCont); // Zero output size means the output has just one pixel.
        planeUnitY = planeMaxY.subtract(planeMinY, mathCont).divide(new BigDecimal(outputSize.height + 1, mathCont), mathCont); // Zero output size means the output has just one pixel.
        curRenRegXLabel.setText("X: " + planeMinX.round(mathContDisp).toEngineeringString() + " + " + planeMaxX.subtract(planeMinX, mathCont).round(mathContDisp).toEngineeringString());
        curRenRegYLabel.setText("Y: " + planeMinY.round(mathContDisp).toEngineeringString() + " + " + planeMaxY.subtract(planeMinY, mathCont).round(mathContDisp).toEngineeringString());
    }

    private void setSelRenRegion(BigDecimal selMinX, BigDecimal selMaxX, BigDecimal selMinY, BigDecimal selMaxY) {
        this.selMinX = selMinX;
        this.selMaxX = selMaxX;
        this.selMinY = selMinY;
        this.selMaxY = selMaxY;
        selRenRegXLabel.setText("X: " + selMinX.round(mathContDisp).toEngineeringString() + " + " + selMaxX.subtract(selMinX, mathCont).round(mathContDisp).toEngineeringString());
        selRenRegYLabel.setText("Y: " + selMinY.round(mathContDisp).toEngineeringString() + " + " + selMaxY.subtract(selMinY, mathCont).round(mathContDisp).toEngineeringString());
    }

    public void startRendering() {
        renderInProgress = true;
        MandelbrotEngine.Parameters p;

        if (autoAdjustIterLimitCheckBox.isSelected()) {
            int limit;

            limit = Integer.parseInt(maxIterTextField.getText());
            if (stats.minIterations > 0.125 * limit) {
                maxIterTextField.setText(String.format("%d", (int) (stats.minIterations / 0.125 + 1)));
            } else if (stats.meanIterations > 0 && stats.meanIterations < 0.125 * limit) {
                maxIterTextField.setText(String.format("%d", (int) (stats.meanIterations * 8)));
            }
        }

        m.clearSelectionRectangle();
        setCurRenRegion(selMinX, selMaxX, selMinY, selMaxY);
        p = new MandelbrotEngine.Parameters(planeMinX, planeMaxX, planeMinY, planeMaxY,
                outputSize.width, outputSize.height, Integer.parseInt(maxIterTextField.getText()),
                Integer.parseInt(sampleSizeTextField.getText()), getMandelbrotVariant((String) mbrotVarComboBox.getSelectedItem()), getColouringMethod((String) colMethComboBox
                        .getSelectedItem()));

        MandelbrotEngine.setParameters(p);
        MandelbrotEngine.startRendering();
    }

    public void drawImage() {
        BufferedImage i;
        Graphics g;

        g = m.getImagePanelGraphics();
        i = MandelbrotEngine.getImage();
        if (i.getWidth() == outputSize.width && i.getHeight() == outputSize.height) {
            g.drawImage(i, 0, 0, null);
        } else {
            g.drawImage(i.getScaledInstance(outputSize.width, outputSize.height,
                    Image.SCALE_SMOOTH), 0, 0, null);
        }
    }
    
    public void redrawImage() {
        BufferedImage i;
        Graphics g;

        g = m.getImagePanelGraphics();
        i = MandelbrotEngine.getImage();
        if (i == null) {
            return;
        }
        m.clearSelectionRectangle();
        g.clearRect(0, 0, outputSize.width, outputSize.height);
        if (i.getWidth() == outputSize.width && i.getHeight() == outputSize.height) {
            g.drawImage(i, 0, 0, null);
        } else {
            g.drawImage(i.getScaledInstance(outputSize.width, outputSize.height,
                    Image.SCALE_SMOOTH), 0, 0, null);
        }

    }

    public void redrawImageRotated(double rotation) {
        BufferedImage i, temp;
        Graphics g;
        AffineTransform t;
        AffineTransformOp op;

        m.clearSelectionRectangle();
        i = MandelbrotEngine.getImage();
        if (i == null) {
            return;
        }

        imageRotation += rotation;
        g = m.getImagePanelGraphics();

        t = AffineTransform.getRotateInstance(imageRotation, (i.getWidth() - 1) / 2, (i.getHeight() - 1) / 2);
        op = new AffineTransformOp(t, AffineTransformOp.TYPE_BILINEAR);
        temp = op.filter(i, null);

        g.drawImage(temp, 0, 0, null);
    }

    public void zoom(Point p, double zoomFactor) {
        if (renderInProgress) {
            return;
        }
        BigDecimal x, y, sizeX, sizeY;
        double rX, rY, aspectRatio;

        rX = p.x / (double) outputSize.width;
        rY = p.y / (double) outputSize.height;
        aspectRatio = outputSize.width / (double) outputSize.height;

        sizeX = planeMaxX.subtract(planeMinX, mathCont).divide(new BigDecimal(zoomFactor, mathCont),
                mathCont);
        sizeY = planeMaxY.subtract(planeMinY, mathCont).divide(new BigDecimal(zoomFactor, mathCont),
                mathCont);
        if (sizeX.divide(sizeY, mathCont).compareTo(new BigDecimal(aspectRatio, mathCont)) > 0) {
            sizeY = sizeX.divide(new BigDecimal(aspectRatio, mathCont), mathCont);
        } else {
            sizeX = sizeY.multiply(new BigDecimal(aspectRatio, mathCont), mathCont);
        }

        x = planeMinX.add(planeUnitX.multiply(new BigDecimal(p.x, mathCont), mathCont), mathCont).subtract(sizeX.multiply(new BigDecimal(rX, mathCont), mathCont), mathCont);
        y = planeMaxY.subtract(planeUnitY.multiply(new BigDecimal(p.y, mathCont), mathCont), mathCont).subtract(sizeY.multiply(new BigDecimal(1 - rY, mathCont), mathCont), mathCont);

        setSelRenRegion(x, x.add(sizeX, mathCont), y, y.add(sizeY, mathCont));
        startRendering();
    }

    public void resetRenderingRegion() {
        setSelRenRegion(new BigDecimal("-2.0", mathCont), new BigDecimal("1.0", mathCont), new BigDecimal("-1.5", mathCont), new BigDecimal("1.5", mathCont));
    }

    public void writeImageToFile(File f) {
        BufferedImage i;

        i = MandelbrotEngine.getImage();
        if (i == null) {
            return;
        }
        try {
            ImageIO.write(i, "png", f);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void renderingBegun() {
        m.getProgressBar().setIndeterminate(true);
        m.getNotificationAreaLabel().setText("Rendering begun");
    }

    @Override
    public void regionRendered(Rectangle region) {
        m.getNotificationAreaLabel().setText("Region rendered: " + region);
    }

    @Override
    public void renderingEnded() {
        drawImage();
        m.getProgressBar().setIndeterminate(false);
        m.getNotificationAreaLabel().setText("Rendered image");
        renderInProgress = false;
    }

    @Override
    public void errorOccurred() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void statsGenerated() {

        stats = MandelbrotEngine.getStatistics();

        sScaleLabel.setText(new BigDecimal(3, mathCont).divide(planeMaxX.subtract(planeMinX, mathCont),
                mathContDisp).toString() + "x");
        m.getNotificationAreaLabel().setText(String.format("Rendered in %.3f ms", stats.renderingTime));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content om this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        curRenRegXLabel = new javax.swing.JLabel();
        curRenRegYLabel = new javax.swing.JLabel();
        selRenRegYLabel = new javax.swing.JLabel();
        selRenRegXLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        maxIterTextField = new javax.swing.JTextField();
        aaCheckBox = new javax.swing.JCheckBox();
        sampleSizeTextField = new javax.swing.JTextField();
        sampleSizeLabel = new javax.swing.JLabel();
        autoAdjustIterLimitCheckBox = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        mbrotVarComboBox = new javax.swing.JComboBox();
        colMethComboBox = new javax.swing.JComboBox();
        jPanel4 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        sSizeLabel = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        sScaleLabel = new javax.swing.JLabel();
        drawButton = new javax.swing.JButton();

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Rendering Region"));

        jLabel1.setText("Current");

        jLabel2.setText("Selection");

        curRenRegXLabel.setText("0 + 0");

        curRenRegYLabel.setText("0 + 0");

        selRenRegYLabel.setText("0 + 0");

        selRenRegXLabel.setText("0 + 0");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(selRenRegYLabel))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(curRenRegXLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(curRenRegYLabel, javax.swing.GroupLayout.Alignment.TRAILING)))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(selRenRegXLabel)))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(curRenRegXLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(curRenRegYLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(selRenRegXLabel)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selRenRegYLabel))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Renderer Settings"));

        jLabel5.setText("Max Iterations");

        maxIterTextField.setText("50");

        aaCheckBox.setText("Anti-aliasing");
        aaCheckBox.setEnabled(false);
        aaCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                aaCheckBoxItemStateChanged(evt);
            }
        });

        sampleSizeTextField.setText("100000");
        sampleSizeTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleSizeTextFieldActionPerformed(evt);
            }
        });

        sampleSizeLabel.setText("Sample Size");

        autoAdjustIterLimitCheckBox.setText("Auto");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(aaCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(sampleSizeLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sampleSizeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(autoAdjustIterLimitCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(maxIterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(maxIterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(autoAdjustIterLimitCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(aaCheckBox)
                    .addComponent(sampleSizeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleSizeLabel)))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Algorithms"));

        jLabel11.setText("Variant");

        jLabel12.setText("Colouring Method");

        mbrotVarComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Regular", "Buddhabrot" }));

        colMethComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Regular", "Red", "Green", "Blue" }));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11)
                    .addComponent(jLabel12))
                .addGap(15, 15, 15)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(colMethComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(mbrotVarComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(mbrotVarComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(colMethComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Statistics"));

        jLabel3.setText("Size");

        sSizeLabel.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        sSizeLabel.setText("0x0");
        sSizeLabel.setPreferredSize(new java.awt.Dimension(22, 14));

        jLabel4.setText("Scale");

        sScaleLabel.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        sScaleLabel.setText("0x");
        sScaleLabel.setPreferredSize(new java.awt.Dimension(33, 14));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sSizeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sScaleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(sSizeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sScaleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)))
        );

        drawButton.setMnemonic('D');
        drawButton.setText("Render");
        drawButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(drawButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 188, Short.MAX_VALUE)
                .addComponent(drawButton)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

	private void aaCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_aaCheckBoxItemStateChanged
            if (aaCheckBox.isSelected()) {
                sampleSizeLabel.setEnabled(true);
                sampleSizeTextField.setEnabled(true);
            } else {
                sampleSizeLabel.setEnabled(false);
                sampleSizeTextField.setEnabled(false);
            }
	}//GEN-LAST:event_aaCheckBoxItemStateChanged

    private void drawButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawButtonActionPerformed
        if (!renderInProgress) {
            startRendering();
        }
    }//GEN-LAST:event_drawButtonActionPerformed

    private void sampleSizeTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sampleSizeTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sampleSizeTextFieldActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox aaCheckBox;
    private javax.swing.JCheckBox autoAdjustIterLimitCheckBox;
    private javax.swing.JComboBox colMethComboBox;
    private javax.swing.JLabel curRenRegXLabel;
    private javax.swing.JLabel curRenRegYLabel;
    private javax.swing.JButton drawButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JTextField maxIterTextField;
    private javax.swing.JComboBox mbrotVarComboBox;
    private javax.swing.JLabel sScaleLabel;
    private javax.swing.JLabel sSizeLabel;
    private javax.swing.JLabel sampleSizeLabel;
    private javax.swing.JTextField sampleSizeTextField;
    private javax.swing.JLabel selRenRegXLabel;
    private javax.swing.JLabel selRenRegYLabel;
    // End of variables declaration//GEN-END:variables
}
