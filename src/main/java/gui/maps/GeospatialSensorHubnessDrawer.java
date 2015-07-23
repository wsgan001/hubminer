/**
* Hub Miner: a hubness-aware machine learning experimentation library.
* Copyright (C) 2014  Nenad Tomasev. Email: nenad.tomasev at gmail.com
* 
* This program is free software: you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation, either version 3 of the License, or (at your option) any later
* version.
* 
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along with
* this program. If not, see <http://www.gnu.org/licenses/>.
*/
package gui.maps;

import data.representation.DataSet;
import ioformat.IOARFF;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import org.jdesktop.swingx.JXMapKit.DefaultProviders;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.Waypoint;
import org.jdesktop.swingx.mapviewer.WaypointPainter;
import org.jdesktop.swingx.mapviewer.WaypointRenderer;

/**
 * This GUI displays a set of SensorWayPoint-s on the world map and scales them
 * and colors them according to the total hubness and the bad hubness of the
 * corresponding sensor stream measurements, respectively. It is a lightweight
 * visualization tool that can be used for semi-supervised initial outlier
 * detection, as high bad hubness in terms of the discrepancies between the
 * proximity and the measurement similarity can indicate erroneous measurements
 * or erroneous data processing. It is something that should be taken into
 * account.
 *
 * @author Nenad Tomasev <nenad.tomasev at gmail.com>
 */
public class GeospatialSensorHubnessDrawer extends javax.swing.JFrame {

    private DataSet latitudeLongitudeData;
    private float[] hubnessArray;
    private float[] badHubnessArray;
    private float minHubness;
    private float maxHubness;
    private float minBadHubness;
    private float maxBadHubness;
    // Waypoint visualization radii.
    private float radiusMax = 20;
    private float radiusMin = 3;
    private ArrayList<String> hubnessFileParse;
    private File currentInFile = null;
    private File currentDirectory = new File(".");

    /**
     * Creates new form GeospatialHubnessDrawer
     */
    public GeospatialSensorHubnessDrawer() {
        initComponents();
        jXMapKit1.setDefaultProvider(DefaultProviders.OpenStreetMaps);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jXMapKit1 = new org.jdesktop.swingx.JXMapKit();
        hDrawButton = new javax.swing.JButton();
        statusLabel = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        loadXYmenuItem = new javax.swing.JMenuItem();
        loadHubnessMenuItem = new javax.swing.JMenuItem();
        closeItem = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        hDrawButton.setText("Draw Hubness Points");
        hDrawButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hDrawButtonActionPerformed(evt);
            }
        });

        jMenu1.setText("File");

        loadXYmenuItem.setText("Load Coordinate File");
        loadXYmenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadXYmenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(loadXYmenuItem);

        loadHubnessMenuItem.setText("Load Hubness Array File");
        loadHubnessMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadHubnessMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(loadHubnessMenuItem);

        closeItem.setText("Close");
        closeItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeItemActionPerformed(evt);
            }
        });
        jMenu1.add(closeItem);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
                    .addComponent(hDrawButton, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(968, 968, 968))
            .addGroup(layout.createSequentialGroup()
                .addGap(95, 95, 95)
                .addComponent(jXMapKit1, javax.swing.GroupLayout.PREFERRED_SIZE, 790, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(282, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(jXMapKit1, javax.swing.GroupLayout.PREFERRED_SIZE, 602, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(71, 71, 71)
                .addComponent(statusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(hDrawButton, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Exit.
     *
     * @param evt ActionEvent object.
     */
    private void closeItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeItemActionPerformed
        System.exit(0);
    }//GEN-LAST:event_closeItemActionPerformed

    /**
     * Load the latitude and longitude data.
     *
     * @param evt ActionEvent object.
     */
    private void loadXYmenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadXYmenuItemActionPerformed
        JFileChooser jfc = new JFileChooser(currentDirectory);
        int rVal = jfc.showOpenDialog(GeospatialSensorHubnessDrawer.this);
        if (rVal == JFileChooser.APPROVE_OPTION) {
            currentInFile = jfc.getSelectedFile();
            currentDirectory = currentInFile.getParentFile();
            try {
                statusLabel.setText("Loading lat/long from: "
                        + currentInFile.getPath());
                System.out.println("Loading data.");
                IOARFF persister = new IOARFF();
                latitudeLongitudeData = persister.load(currentInFile.getPath());
                System.out.println("Data loaded, "
                        + latitudeLongitudeData.size() + " instances");
                statusLabel.setText("Data succesfully loaded from: "
                        + currentInFile.getName());
            } catch (Exception e) {
                System.err.println(e.getMessage());
                statusLabel.setText(e.getMessage());
            }
        }
    }//GEN-LAST:event_loadXYmenuItemActionPerformed

    /**
     * Load the sensor measurement hubness information. This information is
     * provided in a file with a header. Each comma-separated data line contains
     * total hubness and bad hubness as its first and second member. This is the
     * expected file format.
     *
     * @param evt ActionEvent object.
     */
    private void loadHubnessMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadHubnessMenuItemActionPerformed
        JFileChooser jfc = new JFileChooser(currentDirectory);
        int rVal = jfc.showOpenDialog(GeospatialSensorHubnessDrawer.this);
        if (rVal == JFileChooser.APPROVE_OPTION) {
            currentInFile = jfc.getSelectedFile();
            currentDirectory = currentInFile.getParentFile();
            BufferedReader br = null;
            try {
                statusLabel.setText("Loading hubness data from: "
                        + currentInFile.getPath());
                br = new BufferedReader(new InputStreamReader(
                        new FileInputStream(currentInFile)));
                // The first line is just the description.
                br.readLine();
                hubnessFileParse = new ArrayList<>(500);
                String line;
                line = br.readLine();
                while (line != null) {
                    hubnessFileParse.add(line);
                    line = br.readLine();
                }
                hubnessArray = new float[hubnessFileParse.size()];
                badHubnessArray = new float[hubnessFileParse.size()];
                String[] lineItems;
                minHubness = Float.MAX_VALUE;
                maxHubness = 0;
                minBadHubness = Float.MAX_VALUE;
                maxBadHubness = 0;
                for (int i = 0; i < hubnessFileParse.size(); i++) {
                    lineItems = hubnessFileParse.get(i).split(",");
                    // Each element is in its own neighbor set, to avoid zero
                    // occurrence counts, so we increment by 1.
                    hubnessArray[i] = Integer.parseInt(lineItems[0]) + 1;
                    badHubnessArray[i] = Integer.parseInt(lineItems[1]);
                    if (hubnessArray[i] > maxHubness) {
                        maxHubness = hubnessArray[i];
                    }
                    if (hubnessArray[i] < minHubness) {
                        minHubness = hubnessArray[i];
                    }
                    if (badHubnessArray[i] > maxBadHubness) {
                        maxBadHubness = badHubnessArray[i];
                    }
                    if (badHubnessArray[i] < minBadHubness) {
                        minBadHubness = badHubnessArray[i];
                    }
                }
                statusLabel.setText("Data succesfully loaded from: "
                        + currentInFile.getName());
            } catch (Exception e) {
                statusLabel.setText(e.getMessage());
            } finally {
                try {
                    if (br != null) {
                        br.close();
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }//GEN-LAST:event_loadHubnessMenuItemActionPerformed

    /**
     * Draws the Waypoints on the map that are sized and colored according to
     * the provided hubness information. Larger circles correspond to larger
     * hubs in the data and the color corresponds to whether their influence is
     * beneficial or detrimental on the analysis. Red coloring corresponds to
     * high bad hubness.
     *
     * @param evt ActionEvent object.
     */
    private void hDrawButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hDrawButtonActionPerformed
        // Initialize the waypoints.
        Set<Waypoint> waypoints = new HashSet<>(latitudeLongitudeData.size());
        for (int i = 0; i < latitudeLongitudeData.size(); i++) {
            waypoints.add(new SensorWayPoint(
                    latitudeLongitudeData.data.get(i).fAttr[0],
                    -latitudeLongitudeData.data.get(i).fAttr[1],
                    hubnessArray[i],
                    badHubnessArray[i]));
        }
        // Create a WaypointPainter to draw the points.
        WaypointPainter painter = new WaypointPainter();
        painter.setWaypoints(waypoints);
        painter.setRenderer(new WaypointRenderer() {
            @Override
            public boolean paintWaypoint(Graphics2D g, JXMapViewer map,
                    Waypoint wp) {
                SensorWayPoint waypoint = (SensorWayPoint) wp;
                float darkRange = 0.25f;
                float redRange = 0.5f;
                float greenRange = 0.5f;
                // Calculate the badness coefficient that will be used to
                // determine the Waypoint color in the visualization.
                float badnessCoefficient;
                if (maxBadHubness != minBadHubness) {
                    badnessCoefficient =
                            (waypoint.getBadHubness() - minBadHubness)
                            / (maxBadHubness - minBadHubness);
                } else {
                    badnessCoefficient = 0;
                }
                float r = badnessCoefficient * darkRange
                        + badnessCoefficient * redRange;
                float gr = badnessCoefficient * darkRange
                        + (1 - badnessCoefficient) * greenRange;
                float b = badnessCoefficient * darkRange;
                Color currColor = new Color(r, gr, b);
                // Get the zoom level.
                int zLevel = jXMapKit1.getMainMap().getZoom();
                // Determine the radius according the the relative hubness
                // score.
                int radius = (int) ((((waypoint.getHubness() - minHubness)
                        / (maxHubness - minHubness)) * (radiusMax - radiusMin))
                        + radiusMin);
                if (zLevel < 14 && zLevel >= 12) {
                    radius += 4;
                } else if (zLevel < 12 && zLevel >= 10) {
                    radius += 8;
                } else if (zLevel < 10) {
                    radius += 12;
                }
                g.setColor(currColor);
                g.fillOval(-radius, -radius, 2 * radius, 2 * radius);
                return true;
            }
        });
        jXMapKit1.getMainMap().setOverlayPainter(painter);
    }//GEN-LAST:event_hDrawButtonActionPerformed

    /**
     * @param args The command line parameters.
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GeospatialSensorHubnessDrawer().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem closeItem;
    private javax.swing.JButton hDrawButton;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private org.jdesktop.swingx.JXMapKit jXMapKit1;
    private javax.swing.JMenuItem loadHubnessMenuItem;
    private javax.swing.JMenuItem loadXYmenuItem;
    private javax.swing.JLabel statusLabel;
    // End of variables declaration//GEN-END:variables
}