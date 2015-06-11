/*
 *!------------------------------------------------------------------------------------------------!
 *  SettingsPanel.java
 *
 *  Provides an interface to Engine. Can be used as a standalone panel.
 *
 *  Creation date: 04/12/2012
 *  Author: Arindam Biswas <arindam dot b at eml dot cc>
 *!------------------------------------------------------------------------------------------------!
 */

package info.arindam.mandarin;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

/**
 *
 * @author Arindam Biswas <arindam dot b at eml dot cc>
 */
public class SettingsPanel extends javax.swing.JPanel implements Engine.Listener {
    public static interface Listener {
        public void clearSelectionRectangle();
        public Graphics getImagePanelGraphics();
        public JProgressBar getProgressBar();
        public JLabel getNotificationAreaLabel();
    }

    private double imageRotation;
    private Rectangle outputSize;
    private double planeMinX, planeMinY, planeMaxX, planeMaxY, planeUnitX, planeUnitY, selMinX,
            selMinY, selMaxX, selMaxY;
    private Listener l;
    private boolean renderInProgress;
    private Engine.Statistics stats;

    /**
     * Creates new form MandelbrotSettingsPanel
     */
    public SettingsPanel() {
        initComponents();
        outputSize = new Rectangle(100, 100);
        imageRotation = 0;
        planeMinX = -2;
        planeMaxX = 1;
        planeMinY = -1.5;
        planeMaxY = 1.5;
        setSelRenRegion(planeMinX, planeMaxX, planeMinY, planeMaxY);
        setCurRenRegion(planeMinX, planeMaxX, planeMinY, planeMaxY);
        stats = new Engine.Statistics(0, 0, 0, 0, 0);
        Engine.initialize(this);
    }

    public void setListener(Listener l) {
        this.l = l;
    }

    private Engine.Parameters.ColouringMethod getColouringMethod(String s) {
        switch (s) {
            case "Regular":
                return Engine.Parameters.ColouringMethod.REGULAR;
            case "Red":
                return Engine.Parameters.ColouringMethod.RED;
            case "Green":
                return Engine.Parameters.ColouringMethod.GREEN;
            case "Blue":
                return Engine.Parameters.ColouringMethod.BLUE;
            default:
                return Engine.Parameters.ColouringMethod.REGULAR;
        }
    }

    public void setOutputSize(Rectangle r) {
        outputSize = r;
        planeUnitX = (planeMaxX - planeMinX) / outputSize.width;
        planeUnitY = (planeMaxY - planeMinY) / outputSize.height;
        sSizeLabel.setText(r.width + "x" + r.height);
    }

    public void setSelectionRegion(Rectangle r) {
        double selMinX, selMinY, selMaxX, selMaxY, temp1, temp2, temp3;
        double aspectRatio;

        selMinX = planeMinX + planeUnitX * r.x;
        selMaxY = planeMaxY - planeUnitY * r.y;
        selMaxX = selMinX + planeUnitX * r.width;
        selMinY = selMaxY - planeUnitY * (r.height + 1);

        aspectRatio = outputSize.width / (double) outputSize.height;
        temp3 = (selMaxX - selMinX) / (selMaxY - selMinY);
        if (temp3 > aspectRatio) {
            temp1 = selMaxX - selMinX;
            temp2 = selMaxY - selMinY;
            temp1 = temp1 / aspectRatio - temp2;
            temp1 = temp1 / 2;
            selMinY = selMinY - temp1;
            selMaxY = selMaxY + temp1;
        } else if (temp3 < aspectRatio) {
            temp1 = selMaxX - selMinX;
            temp2 = selMaxY - selMinY;
            temp1 = temp2 * aspectRatio - temp1;
            temp1 = temp1 / 2;
            selMinX = selMinX - temp1;
            selMaxX = selMaxX + temp1;
        }
        setSelRenRegion(selMinX, selMaxX, selMinY, selMaxY);
    }

    private void setCurRenRegion(double planeMinX, double planeMaxX, double planeMinY, double planeMaxY) {
        this.planeMinX = planeMinX;
        this.planeMaxX = planeMaxX;
        this.planeMinY = planeMinY;
        this.planeMaxY = planeMaxY;
        planeUnitX = (planeMaxX - planeMinX) / (outputSize.width + 1); // Zero output size means the output has just one pixel.
        planeUnitY = (planeMaxY - planeMinY) / (outputSize.height + 1); // Zero output size means the output has just one pixel.
        curRenRegXLabel.setText(String.format("X: %.3G + %.3G", planeMinX, (planeMaxX - planeMinX)));
        curRenRegYLabel.setText(String.format("Y: %.3G + %.3G", planeMinY, (planeMaxY - planeMinY)));
    }

    private void setSelRenRegion(double selMinX, double selMaxX, double selMinY, double selMaxY) {
        this.selMinX = selMinX;
        this.selMaxX = selMaxX;
        this.selMinY = selMinY;
        this.selMaxY = selMaxY;
        selRenRegXLabel.setText(String.format("X: %.3G + %.3G", planeMinX, (planeMaxX - planeMinX)));
        selRenRegYLabel.setText(String.format("Y: %.3G + %.3G", planeMinY, (planeMaxY - planeMinY)));
    }

    public void startRendering() {
        renderInProgress = true;
        Engine.Parameters p;

        if (autoAdjustIterLimitCheckBox.isSelected()) {
            int limit;

            limit = Integer.parseInt(maxIterTextField.getText());
            if (stats.minIterations > 0.125 * limit) {
                maxIterTextField.setText(String.format("%d", (int) (stats.minIterations / 0.125 + 1)));
            } else if (stats.meanIterations > 0 && stats.meanIterations < 0.125 * limit) {
                maxIterTextField.setText(String.format("%d", (int) (stats.meanIterations * 8)));
            }
        }

        l.clearSelectionRectangle();
        setCurRenRegion(selMinX, selMaxX, selMinY, selMaxY);
        p = new Engine.Parameters(planeMinX, planeMaxX, planeMinY, planeMaxY,
                outputSize.width, outputSize.height, Integer.parseInt(maxIterTextField.getText()),
                getColouringMethod((String) colMethComboBox
                        .getSelectedItem()));

        Engine.setParameters(p);
        Engine.startRendering();
    }

    public void drawImage() {
        BufferedImage i;
        Graphics g;

        g = l.getImagePanelGraphics();
        i = Engine.getImage();
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

        g = l.getImagePanelGraphics();
        i = Engine.getImage();
        if (i == null) {
            return;
        }
        l.clearSelectionRectangle();
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

        l.clearSelectionRectangle();
        i = Engine.getImage();
        if (i == null) {
            return;
        }

        imageRotation += rotation;
        g = l.getImagePanelGraphics();

        t = AffineTransform.getRotateInstance(imageRotation, (i.getWidth() - 1) / 2, (i.getHeight() - 1) / 2);
        op = new AffineTransformOp(t, AffineTransformOp.TYPE_BILINEAR);
        temp = op.filter(i, null);

        g.drawImage(temp, 0, 0, null);
    }

    public void zoom(Point p, double zoomFactor) {
        if (renderInProgress) {
            return;
        }
        double x, y, sizeX, sizeY;
        double rX, rY, aspectRatio;

        rX = p.x / (double) outputSize.width;
        rY = p.y / (double) outputSize.height;
        aspectRatio = outputSize.width / (double) outputSize.height;

        sizeX = (planeMaxX - planeMinX) / zoomFactor;
        sizeY = (planeMaxY - planeMinY) / zoomFactor;
        if (sizeX / sizeY > aspectRatio) {
            sizeY = sizeX / aspectRatio;
        } else {
            sizeX = sizeY * aspectRatio;
        }

        x = planeMinX + planeUnitX * p.x - sizeX * rX;
        y = planeMaxY - planeUnitY * p.y - sizeY * (1 - rY);

        setSelRenRegion(x, x + sizeX, y, y + sizeY);
        startRendering();
    }

    public void resetRenderingRegion() {
        setSelRenRegion(-2.0, 1.0, -1.5, 1.5);
    }

    public void writeImageToFile(File f) {
        BufferedImage i;

        i = Engine.getImage();
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
        l.getProgressBar().setIndeterminate(true);
        l.getNotificationAreaLabel().setText("Rendering begun");
    }

    @Override
    public void regionRendered(Rectangle region) {
        l.getNotificationAreaLabel().setText("Region rendered: " + region);
    }

    @Override
    public void renderingEnded() {
        drawImage();
        l.getProgressBar().setIndeterminate(false);
        l.getNotificationAreaLabel().setText("Rendered image");
        renderInProgress = false;
    }

    @Override
    public void errorOccurred() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void statsGenerated() {

        stats = Engine.getStatistics();

        sScaleLabel.setText(String.format("%.3Gx", 3 / (planeMaxX - planeMinX)));
        l.getNotificationAreaLabel().setText(String.format("Rendered in %.3f ms", stats.renderingTime));
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
        autoAdjustIterLimitCheckBox = new javax.swing.JCheckBox();
        jLabel12 = new javax.swing.JLabel();
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

        autoAdjustIterLimitCheckBox.setText("Auto");

        jLabel12.setText("Colouring Method");

        colMethComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Regular", "Red", "Green", "Blue" }));
        colMethComboBox.setEnabled(false);
        colMethComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colMethComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addGap(15, 15, 15)
                        .addComponent(colMethComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(maxIterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(autoAdjustIterLimitCheckBox)))
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
                    .addComponent(jLabel12)
                    .addComponent(colMethComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
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
                .addContainerGap(20, Short.MAX_VALUE)
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
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 194, Short.MAX_VALUE)
                .addComponent(drawButton)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void drawButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawButtonActionPerformed
        if (!renderInProgress) {
            startRendering();
        }
    }//GEN-LAST:event_drawButtonActionPerformed

    private void colMethComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colMethComboBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_colMethComboBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox autoAdjustIterLimitCheckBox;
    private javax.swing.JComboBox colMethComboBox;
    private javax.swing.JLabel curRenRegXLabel;
    private javax.swing.JLabel curRenRegYLabel;
    private javax.swing.JButton drawButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JTextField maxIterTextField;
    private javax.swing.JLabel sScaleLabel;
    private javax.swing.JLabel sSizeLabel;
    private javax.swing.JLabel selRenRegXLabel;
    private javax.swing.JLabel selRenRegYLabel;
    // End of variables declaration//GEN-END:variables
}
