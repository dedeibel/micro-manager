/**
 * MainForm.java
 *
 * Form showing the UI controlling tracking of single molecules using
 * Gaussian Fitting
 *
 * The real work is done in class GaussianTrackThread
 *
 * Created on Sep 15, 2010, 9:29:05 PM
 */

package edu.valelab.gaussianfit;

import edu.valelab.gaussianfit.algorithm.FindLocalMaxima;
import edu.valelab.gaussianfit.data.GaussianInfo;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import java.awt.Color;
import java.awt.Polygon;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.json.JSONException;
import org.json.JSONObject;
import edu.valelab.gaussianfit.utils.NumberUtils;
import edu.valelab.gaussianfit.utils.ReportingUtils;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import net.miginfocom.swing.MigLayout;
import org.micromanager.Studio;
import org.micromanager.data.Coords;
import org.micromanager.display.DisplayWindow;



/**
 *
 * @author nico
 */
public class MainForm extends JFrame implements ij.ImageListener{
   private static final String NOISETOLERANCE = "NoiseTolerance";
   private static final String PCF = "PhotonConversionFactor";
   private static final String GAIN = "Gain";
   private static final String PIXELSIZE = "PixelSize";
   private static final String TIMEINTERVALMS = "TimeIntervalMs";
   private static final String ZSTEPSIZE = "ZStepSize";
   private static final String BACKGROUNDLEVEL = "BackgroundLevel";
   private static final String SIGMAMAX = "SigmaMax";
   private static final String SIGMAMIN = "SigmaMin";
   private static final String USEFILTER = "UseFilter";
   private static final String NRPHOTONSMIN = "NrPhotonsMin";
   private static final String NRPHOTONSMAX = "NrPhotonsMax";
   private static final String USENRPHOTONSFILTER = "UseNrPhotonsFilter";
   private static final String MAXITERATIONS = "MaxIterations";
   private static final String BOXSIZE = "BoxSize";
   private static final String FRAMEXPOS = "XPos";
   private static final String FRAMEYPOS = "YPos";
   private static final String FITMODE = "FitMode";
   private static final String FITSHAPE = "FitShape";
   private static final String ENDTRACKBOOL = "EndTrackBoolean";
   private static final String ENDTRACKINT = "EndTrackAfterN";
   private static final String PREFILTER = "PreFilterType";
   private static final String SKIPCHANNELS = "SkipChannels";
   private static final String CHANNELSKIPSTRING = "ChannelsToSkip";

   // we are a singleton with only one window
   public static boolean WINDOWOPEN = false;

   private Preferences prefs_;
   private final Studio studio_;
   
   // Store values of dropdown menus:
   private int shape_ = 1;
   private int fitMode_ = 2;
   private FindLocalMaxima.FilterType preFilterType_ = FindLocalMaxima.FilterType.NONE;

   private FitAllThread ft_;
   
   public AtomicBoolean aStop_ = new AtomicBoolean(false);

   private int lastFrame_ = -1;
   
   // to keep track of front most window
   ImagePlus ip_ = null;
   
   // GUI elements
   private javax.swing.JLabel labelNPoints_;
   private javax.swing.JButton mTrackButton_;
   private javax.swing.JTextField maxIterationsTextField_;
   private javax.swing.JTextField maxNrPhotonsTextField_;
   private javax.swing.JTextField maxSigmaTextField_;
   private javax.swing.JTextField minNrPhotonsTextField_;
   private javax.swing.JTextField minSigmaTextField_;
   private javax.swing.JTextField noiseToleranceTextField_;
   private javax.swing.JTextField photonConversionTextField_;
   private javax.swing.JTextField pixelSizeTextField_;
   private javax.swing.JTextField posTextField_;
   private javax.swing.JComboBox preFilterComboBox_;
   private javax.swing.JComboBox fitDimensionsComboBox1_;
   private javax.swing.JComboBox fitMethodComboBox1_;
   private javax.swing.JToggleButton readParmsButton_;
   private javax.swing.JTextField baseLevelTextField;
   private javax.swing.JTextField boxSizeTextField;
   private javax.swing.JTextField emGainTextField_;
   private javax.swing.JCheckBox endTrackCheckBox_;
   private javax.swing.JCheckBox filterDataCheckBoxNrPhotons_;
   private javax.swing.JCheckBox filterDataCheckBoxWidth_;
   private JCheckBox skipChannelsCheckBox_;
   private JTextField channelsToSkip_;
   private JLabel widthLabel_;
   private javax.swing.JSpinner endTrackSpinner_;
   private javax.swing.JButton fitAllButton_;
   private javax.swing.JToggleButton showOverlay_;
   private javax.swing.JTextField timeIntervalTextField_;
   private javax.swing.JTextField zStepTextField_;


    /**
     * Creates new form MainForm
     * 
     * @param studio Instance of the Micro-Manager 2.0 api
     */
    public MainForm(Studio studio) {
       initComponents();

       studio_ = studio;
       
       // TODO: convert to using MM profile
       if (prefs_ == null)
            prefs_ = Preferences.userNodeForPackage(this.getClass());
       noiseToleranceTextField_.setText(Integer.toString(prefs_.getInt(NOISETOLERANCE,100)));
       photonConversionTextField_.setText(Double.toString(prefs_.getDouble(PCF, 10.41)));
       emGainTextField_.setText(Double.toString(prefs_.getDouble(GAIN, 50)));
       pixelSizeTextField_.setText(Double.toString(prefs_.getDouble(PIXELSIZE, 107.0)));
       baseLevelTextField.setText(Double.toString(prefs_.getDouble(BACKGROUNDLEVEL, 100)));
       timeIntervalTextField_.setText(Double.toString(prefs_.getDouble(TIMEINTERVALMS, 1)));
       zStepTextField_.setText(Double.toString(prefs_.getDouble(ZSTEPSIZE, 50)));                   
       pixelSizeTextField_.getDocument().addDocumentListener(new BackgroundCleaner(pixelSizeTextField_));
       emGainTextField_.getDocument().addDocumentListener(new BackgroundCleaner(emGainTextField_));      
       timeIntervalTextField_.getDocument().addDocumentListener(new BackgroundCleaner(timeIntervalTextField_));
       
       minSigmaTextField_.setText(Double.toString(prefs_.getDouble(SIGMAMIN, 100)));
       maxSigmaTextField_.setText(Double.toString(prefs_.getDouble(SIGMAMAX, 200)));
       minNrPhotonsTextField_.setText(Double.toString(prefs_.getDouble(NRPHOTONSMIN, 500)));
       maxNrPhotonsTextField_.setText(Double.toString(prefs_.getDouble(NRPHOTONSMAX, 50000)));
       filterDataCheckBoxNrPhotons_.setSelected(prefs_.getBoolean(USENRPHOTONSFILTER, false));
       fitDimensionsComboBox1_.setSelectedIndex(prefs_.getInt(FITSHAPE, 1) - 1);
       fitMethodComboBox1_.setSelectedIndex(prefs_.getInt(FITMODE, 0));
       maxIterationsTextField_.setText(Integer.toString(prefs_.getInt(MAXITERATIONS, 250)));
       boxSizeTextField.setText(Integer.toString(prefs_.getInt(BOXSIZE, 16)));
       filterDataCheckBoxWidth_.setSelected(prefs_.getBoolean(USEFILTER, false));
       preFilterComboBox_.setSelectedIndex(prefs_.getInt(PREFILTER, 0));
       endTrackCheckBox_.setSelected(prefs_.getBoolean(ENDTRACKBOOL, false));
       endTrackSpinner_.setValue(prefs_.getInt(ENDTRACKINT, 0));
       skipChannelsCheckBox_.setSelected(prefs_.getBoolean(SKIPCHANNELS, false));
       channelsToSkip_.setText(prefs_.get(CHANNELSKIPSTRING, ""));
             
       DocumentListener updateNoiseOverlay = new DocumentListener() {

          @Override
          public void changedUpdate(DocumentEvent documentEvent) {
             updateDisplay();
          }

          @Override
          public void insertUpdate(DocumentEvent documentEvent) {
             updateDisplay();
          }

          @Override
          public void removeUpdate(DocumentEvent documentEvent) {
             updateDisplay();
          }

          private void updateDisplay() {
             if (WINDOWOPEN && showOverlay_.isSelected()) {
                showNoiseTolerance();
             }
          }
       };

       updateWidthDisplay();
       noiseToleranceTextField_.getDocument().addDocumentListener(updateNoiseOverlay);
       boxSizeTextField.getDocument().addDocumentListener(updateNoiseOverlay);
       
       super.getRootPane().setDefaultButton(fitAllButton_);
          
       super.setTitle("Localization Microscopy");
       
       super.setLocation(prefs_.getInt(FRAMEXPOS, 100), prefs_.getInt(FRAMEYPOS, 100));
       
       ImagePlus.addImageListener(this);
       super.setVisible(true);
    }
    
    
   private class BackgroundCleaner implements DocumentListener {

      JTextField field_;

      public BackgroundCleaner(JTextField field) {
         field_ = field;
      }

      private void updateBackground() {
         field_.setBackground(Color.white);
      }

      @Override
      public void changedUpdate(DocumentEvent documentEvent) {
         updateBackground();
      }

      @Override
      public void insertUpdate(DocumentEvent documentEvent) {
         updateBackground();
      }

      @Override
      public void removeUpdate(DocumentEvent documentEvent) {
         updateBackground();
      }
   };
    

    /** This method is called from within the constructor to
     * initialize the form.

     */
    @SuppressWarnings("unchecked")
   private void initComponents() {


      filterDataCheckBoxWidth_ = new javax.swing.JCheckBox();
      photonConversionTextField_ = new javax.swing.JTextField();
      emGainTextField_ = new javax.swing.JTextField();
      baseLevelTextField = new javax.swing.JTextField();
      minSigmaTextField_ = new javax.swing.JTextField();
      noiseToleranceTextField_ = new javax.swing.JTextField();
      pixelSizeTextField_ = new javax.swing.JTextField();
      fitAllButton_ = new javax.swing.JButton();
      preFilterComboBox_ = new javax.swing.JComboBox();
      fitDimensionsComboBox1_ = new javax.swing.JComboBox();
      timeIntervalTextField_ = new javax.swing.JTextField();
      maxIterationsTextField_ = new javax.swing.JTextField();
      maxSigmaTextField_ = new javax.swing.JTextField();
      boxSizeTextField = new javax.swing.JTextField();
      filterDataCheckBoxNrPhotons_ = new javax.swing.JCheckBox();
      minNrPhotonsTextField_ = new javax.swing.JTextField();
      maxNrPhotonsTextField_ = new javax.swing.JTextField();
      endTrackCheckBox_ = new javax.swing.JCheckBox();
      endTrackSpinner_ = new javax.swing.JSpinner();
      readParmsButton_ = new javax.swing.JToggleButton();
      fitMethodComboBox1_ = new javax.swing.JComboBox();
      showOverlay_ = new javax.swing.JToggleButton();
      mTrackButton_ = new javax.swing.JButton();
      zStepTextField_ = new javax.swing.JTextField();
      labelNPoints_ = new javax.swing.JLabel();
      posTextField_ = new javax.swing.JTextField();
      channelsToSkip_ = new JTextField();

      
      
      Font gFont = new Font("Lucida Grande", 0, 10);
      Dimension textFieldDim = new Dimension(57,20);
      Dimension dropDownSize = new Dimension(90, 20);
      Dimension dropDownSizeMax = new Dimension(120, 20);
      String indent = "gapleft 20px";


      addWindowListener(new java.awt.event.WindowAdapter() {
         @Override
         public void windowClosing(java.awt.event.WindowEvent evt) {
            formWindowClosing(evt);
         }
         @Override
         public void windowClosed(java.awt.event.WindowEvent evt) {
            formWindowClosed(evt);
         }
      });
      
      getContentPane().setLayout(new MigLayout("insets 8, fillx", "", "[13]0[13]"));
      
      
/*-----------  Imaging Parameters  -----------*/
      getContentPane().add(new JLabel("Imaging parameters..."));
      
      readParmsButton_.setText("read");
      readParmsButton_.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            readParmsButton_ActionPerformed(evt);
         }
      });
      getContentPane().add(readParmsButton_, "wrap");

      JLabel jLabel = new JLabel("Photon Conversion factor");
      jLabel.setFont(gFont); 
      getContentPane().add(jLabel, indent);

      photonConversionTextField_.setFont(gFont); 
      photonConversionTextField_.setText("10.41");
      photonConversionTextField_.setMinimumSize(textFieldDim);
      getContentPane().add(photonConversionTextField_, "wrap");

      jLabel = new JLabel("Linear (EM) Gain");
      jLabel.setFont(gFont); 
      getContentPane().add(jLabel, indent);
      
      emGainTextField_.setFont(gFont); 
      emGainTextField_.setText("50");
      emGainTextField_.setMinimumSize(textFieldDim);
      getContentPane().add(emGainTextField_, "wrap");
  
      jLabel = new JLabel("PixelSize (nm)");
      jLabel.setFont(gFont); 
      getContentPane().add(jLabel, indent);
      
      pixelSizeTextField_.setFont(gFont); 
      pixelSizeTextField_.setText("0.8");
      pixelSizeTextField_.setMinimumSize(textFieldDim);
      getContentPane().add(pixelSizeTextField_, "wrap");

      jLabel = new JLabel("Time Interval (ms)");
      jLabel.setFont(gFont); 
      getContentPane().add(jLabel, indent);
      
      timeIntervalTextField_.setFont(gFont); 
      timeIntervalTextField_.setText("0.8");
      timeIntervalTextField_.setMinimumSize(textFieldDim);
      getContentPane().add(timeIntervalTextField_, "wrap");
 
      jLabel = new JLabel("Z-step (nm)");
      jLabel.setFont(gFont); 
      getContentPane().add(jLabel, indent);

      zStepTextField_.setFont(gFont); 
      zStepTextField_.setText("50");
      zStepTextField_.setMinimumSize(textFieldDim);
      getContentPane().add(zStepTextField_, "wrap");
      
      jLabel = new JLabel("Camera Offset (counts)");
      jLabel.setFont(gFont); 
      getContentPane().add(jLabel, indent);
            
      baseLevelTextField.setFont(gFont); 
      baseLevelTextField.setText("100");
      baseLevelTextField.setMinimumSize(textFieldDim);
      getContentPane().add(baseLevelTextField, "wrap");
      
      getContentPane().add(new JSeparator(), "span, grow, wrap");
      
/*-----------  Find Maxima  -----------*/      
      jLabel = new JLabel("Find Maxima...");
      getContentPane().add(jLabel, "grow");

      showOverlay_.setText("show");
      showOverlay_.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            showOverlay_ActionPerformed(evt);
         }
      });
      getContentPane().add(showOverlay_, "wrap");
      
      jLabel = new JLabel("Pre-Filter");
      jLabel.setFont(gFont); 
      getContentPane().add(jLabel, indent + ", span, split2, grow");

      preFilterComboBox_.setFont(gFont); 
      preFilterComboBox_.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None", "Gaussian1-5" }));
      preFilterComboBox_.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            preFilterComboBox_ActionPerformed(evt);
         }
      });
      preFilterComboBox_.setMinimumSize(dropDownSize);
      preFilterComboBox_.setMaximumSize(dropDownSizeMax);
      getContentPane().add(preFilterComboBox_, "wrap");

      labelNPoints_.setFont(gFont); 
      labelNPoints_.setText("n:       ");
      getContentPane().add(labelNPoints_, indent + ", split 2");

      jLabel = new JLabel("Noise tolerance");   
      jLabel.setFont(gFont); 
      getContentPane().add(jLabel, "right");
           
      noiseToleranceTextField_.setFont(gFont); 
      noiseToleranceTextField_.setText("2000");
      noiseToleranceTextField_.setMinimumSize(textFieldDim);
      getContentPane().add(noiseToleranceTextField_, "wrap");
    
      getContentPane().add(new JSeparator(), "span, grow, wrap");
      
/*-----------  Fit Parameters  -----------*/
      jLabel = new JLabel("Fit Parameters...");
      getContentPane().add(jLabel, "left, wrap");
 
      jLabel = new JLabel("Dimensions");            
      jLabel.setFont(gFont);
      getContentPane().add(jLabel, indent + ", split 2, span 2, grow");
         
      fitDimensionsComboBox1_.setFont(gFont); 
      fitDimensionsComboBox1_.setModel(new javax.swing.DefaultComboBoxModel(
              new String[] { "1", "2", "3" }));
      fitDimensionsComboBox1_.setMinimumSize(dropDownSize);
      fitDimensionsComboBox1_.setMaximumSize(dropDownSizeMax);
      getContentPane().add(fitDimensionsComboBox1_, "wrap");
 
      jLabel = new JLabel("Fitter");      
      jLabel.setFont(gFont);
      getContentPane().add(jLabel, indent + ", span 2, split 2, grow");
      
      fitMethodComboBox1_.setFont(gFont); 
      fitMethodComboBox1_.setModel(new javax.swing.DefaultComboBoxModel(
              new String[] { "Simplex", "Levenberg-Marq", "Simplex-MLE", "Levenberg-Marq-Weighted" }));
      fitMethodComboBox1_.setMinimumSize(dropDownSize);    
      fitMethodComboBox1_.setMaximumSize(dropDownSizeMax);
      getContentPane().add(fitMethodComboBox1_, "gapright push, wrap");

      jLabel = new JLabel("Max Iterations");      
      jLabel.setFont(gFont); 
      getContentPane().add(jLabel, indent);

      maxIterationsTextField_.setFont(gFont); 
      maxIterationsTextField_.setText("250");
      maxIterationsTextField_.setMinimumSize(textFieldDim);
      getContentPane().add(maxIterationsTextField_, "wrap");

      jLabel = new JLabel("Box Size (pixels)");
      jLabel.setFont(gFont); 
      getContentPane().add(jLabel, indent);
      
      
      // HACK: not re-assigning jLabel as done below causes the last 
      // jLabel to not show. No idea why, but this works around the problem
      jLabel = new JLabel("Hack");

      boxSizeTextField.setFont(gFont); 
      boxSizeTextField.setText("16");
      boxSizeTextField.setMinimumSize(textFieldDim);
      getContentPane().add(boxSizeTextField, "wrap");
      
      getContentPane().add(new JSeparator(), "span 3, grow, wrap");
      
/*-----------  Filter Data  -----------*/
      getContentPane().add(new JLabel("Filter Data..."), "wrap");
      
      widthLabel_ = new JLabel(" nm < Width < "); 
      
      filterDataCheckBoxWidth_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            updateWidthDisplay();
         }
      });
      getContentPane().add(filterDataCheckBoxWidth_, indent + ",span 3, split 5");  

      minSigmaTextField_.setFont(gFont); 
      minSigmaTextField_.setText("100");
      minSigmaTextField_.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            minSigmaTextFieldActionPerformed(evt);
         }
      });
      minSigmaTextField_.setMinimumSize(textFieldDim);
      getContentPane().add(minSigmaTextField_);
          
      widthLabel_.setFont(gFont);
      getContentPane().add(widthLabel_);
      
      maxSigmaTextField_.setFont(gFont); 
      maxSigmaTextField_.setText("200");
      maxSigmaTextField_.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            maxSigmaTextFieldActionPerformed(evt);
         }
      });
      maxSigmaTextField_.setMinimumSize(textFieldDim);
      getContentPane().add(maxSigmaTextField_);

      JLabel jLabel3 = new JLabel("nm");
      jLabel3.setFont(gFont); 
      getContentPane().add(jLabel3, "wrap");

      getContentPane().add(filterDataCheckBoxNrPhotons_, indent + ", span, split 4");

      minNrPhotonsTextField_.setFont(gFont); 
      minNrPhotonsTextField_.setText("100");
      minNrPhotonsTextField_.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            minNrPhotonsTextFieldActionPerformed(evt);
         }
      });
      getContentPane().add(minNrPhotonsTextField_);

      JLabel jLabel2 = new JLabel(" < # photons < ");
      jLabel2.setFont(gFont); 
      getContentPane().add(jLabel2);

      maxNrPhotonsTextField_.setFont(gFont); 
      maxNrPhotonsTextField_.setText("200");
      maxNrPhotonsTextField_.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            maxNrPhotonsTextFieldActionPerformed(evt);
         }
      });
      getContentPane().add(maxNrPhotonsTextField_, "wrap");
 
      endTrackCheckBox_.setFont(gFont); 
      endTrackCheckBox_.setText("End track when missing");
      getContentPane().add(endTrackCheckBox_, indent + ", span 3, split 3");

      endTrackSpinner_.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));
      getContentPane().add(endTrackSpinner_);

      jLabel.setText(" frames");
      jLabel.setFont(gFont); 
      getContentPane().add(jLabel, "wrap");

      getContentPane().add(new JSeparator(), "span, grow, wrap");

      
/*-----------  Positions  -----------*/
      getContentPane().add(new JLabel("Positions..."), "wrap");

      JButton allPosButton = new JButton ("All");
      allPosButton.setFont(gFont); 
      allPosButton.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            allPosButton_ActionPerformed(evt);
         }
      });
      getContentPane().add(allPosButton, indent + ",span, split 3");

      
      final JButton currentPosButton = new JButton();
      currentPosButton.setFont(gFont); 
      currentPosButton.setText("Current");
      currentPosButton.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            currentPosButton_ActionPerformed(evt);
         }
      });
      getContentPane().add(currentPosButton);

      posTextField_.setFont(gFont); 
      posTextField_.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
      posTextField_.setMinimumSize(textFieldDim);
      posTextField_.setText("1");
      getContentPane().add(posTextField_, "wrap");

      getContentPane().add(new JSeparator(), "span, grow, wrap");

/*-----------  Channels  -----------*/
      getContentPane().add(new JLabel("Skip Channels..."), "wrap");
      
      skipChannelsCheckBox_ = new JCheckBox();
      skipChannelsCheckBox_.setFont(gFont); 
      skipChannelsCheckBox_.setText("Skip channel 1");
      getContentPane().add(skipChannelsCheckBox_, indent + ", wrap");
      
      //channelsToSkip_.setMinimumSize(textFieldDim);
      //getContentPane().add(channelsToSkip_, "wrap");
      
      
      getContentPane().add(new JSeparator(), "span, grow, wrap");
/*-----------  Buttons  -----------*/
 

      fitAllButton_.setText("Fit");
      fitAllButton_.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            fitAllButton_ActionPerformed(evt);
         }
      });
      getContentPane().add(fitAllButton_, "span, split 3");
      fitAllButton_.setBounds(10, 530, 80, 30);


      JButton trackButton = new JButton("Track");
      trackButton.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            trackButtonActionPerformed(evt);
         }
      });
      getContentPane().add(trackButton);
      
      mTrackButton_.setText("MTrack");
      mTrackButton_.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            mTrackButton_ActionPerformed(evt);
         }
      });
      getContentPane().add(mTrackButton_, "wrap");

          
      JButton showButton = new JButton("Data");
      showButton.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            showButtonActionPerformed(evt);
         }
      });
      getContentPane().add(showButton, "span, split 2, align center");

      JButton stopButton = new JButton("Stop");
      stopButton.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            stopButtonActionPerformed(evt);
         }
      });
      getContentPane().add(stopButton, "wrap");
    

      pack();
      
      setResizable(false);
   }

  
    private void trackButtonActionPerformed(java.awt.event.ActionEvent evt) {
       GaussianTrackThread tT = new GaussianTrackThread(IJ.getImage(), 
               FindLocalMaxima.FilterType.NONE);
       updateValues(tT);
       
       // Execute on another thread,
       // use tT.trackGaussians to run it on the same thread
       tT.init();
       System.out.println("started thread");
    }

    private void formWindowClosed(java.awt.event.WindowEvent evt) {
       WINDOWOPEN = false;
    }

    private void fitAllButton_ActionPerformed(java.awt.event.ActionEvent evt) {
       if (ft_ == null || !ft_.isRunning()) {
          try {
             shape_ = NumberUtils.displayStringToInt(fitDimensionsComboBox1_.getSelectedItem());
          } catch (ParseException ex) {
             studio_.logs().showError(ex, "Input error that should never happen");
             return;
          }
          fitMode_ = fitMethodComboBox1_.getSelectedIndex();
          ft_ = new FitAllThread(studio_, shape_, fitMode_, preFilterType_, 
                  posTextField_.getText());
          updateValues(ft_);
          ft_.init();
       } else {
          JOptionPane.showMessageDialog(null, "Already running fitting analysis");
       }
    }

   private void updateWidthDisplay() {
      boolean selected = filterDataCheckBoxWidth_.isSelected();
      minSigmaTextField_.setEnabled(selected);
      widthLabel_.setEnabled(selected);
      maxSigmaTextField_.setEditable(selected);
    }
    
    private void formWindowClosing(java.awt.event.WindowEvent evt) {
       try {
       prefs_.put(NOISETOLERANCE, noiseToleranceTextField_.getText());
       prefs_.putDouble(PCF, NumberUtils.displayStringToDouble(photonConversionTextField_.getText()));
       prefs_.putDouble(GAIN, NumberUtils.displayStringToDouble(emGainTextField_.getText()));
       prefs_.putDouble(PIXELSIZE, NumberUtils.displayStringToDouble(pixelSizeTextField_.getText()));      
       prefs_.putDouble(TIMEINTERVALMS, NumberUtils.displayStringToDouble(timeIntervalTextField_.getText()));
       prefs_.putDouble(ZSTEPSIZE, NumberUtils.displayStringToDouble(zStepTextField_.getText()));
       prefs_.putDouble(BACKGROUNDLEVEL, NumberUtils.displayStringToDouble(baseLevelTextField.getText()));
       prefs_.putBoolean(USEFILTER, filterDataCheckBoxWidth_.isSelected());
       prefs_.putDouble(SIGMAMIN, NumberUtils.displayStringToDouble(minSigmaTextField_.getText()));
       prefs_.putDouble(SIGMAMAX, NumberUtils.displayStringToDouble(maxSigmaTextField_.getText()));
       prefs_.putBoolean(USENRPHOTONSFILTER, filterDataCheckBoxNrPhotons_.isSelected());
       prefs_.putDouble(NRPHOTONSMIN, NumberUtils.displayStringToDouble(minNrPhotonsTextField_.getText()));
       prefs_.putDouble(NRPHOTONSMAX, NumberUtils.displayStringToDouble(maxNrPhotonsTextField_.getText()));
       prefs_.putInt(MAXITERATIONS, NumberUtils.displayStringToInt(maxIterationsTextField_.getText()));
       prefs_.putInt(BOXSIZE, NumberUtils.displayStringToInt(boxSizeTextField.getText()));
       prefs_.putInt(PREFILTER, preFilterComboBox_.getSelectedIndex());
       prefs_.putInt(FRAMEXPOS, getX());
       prefs_.putInt(FRAMEYPOS, getY());
       prefs_.putBoolean(ENDTRACKBOOL, endTrackCheckBox_.isSelected() );
       prefs_.putInt(ENDTRACKINT, (Integer) endTrackSpinner_.getValue() );
       prefs_.putInt(FITMODE, fitMethodComboBox1_.getSelectedIndex());
       prefs_.putInt(FITSHAPE, fitDimensionsComboBox1_.getSelectedIndex() + 1);
       prefs_.putBoolean(SKIPCHANNELS, skipChannelsCheckBox_.isSelected());
       } catch (ParseException ex) {
          ReportingUtils.logError(ex, "Error while closing Localization Microscopy plugin");
       }
       
       WINDOWOPEN = false;
       
       this.setVisible(false);
    }

    public void formWindowOpened() {
       WINDOWOPEN = true;
    }
    
   @Override
    public void dispose() {
       formWindowClosing(null);
    }

    private void preFilterComboBox_ActionPerformed(java.awt.event.ActionEvent evt) {
       String item = (String) preFilterComboBox_.getSelectedItem();
       if (item.equals("None"))
          preFilterType_ = FindLocalMaxima.FilterType.NONE;
       if (item.equals("Gaussian1-5"))
          preFilterType_ = FindLocalMaxima.FilterType.GAUSSIAN1_5;
       if (showOverlay_.isSelected())
         showNoiseTolerance();
    }

    private void maxSigmaTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
       if (Double.parseDouble(maxSigmaTextField_.getText()) <=
               Double.parseDouble(minSigmaTextField_.getText() ))
          minSigmaTextField_.setText( Double.toString
                  (Double.parseDouble(maxSigmaTextField_.getText()) - 1));
    }

    private void minSigmaTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
       if (Double.parseDouble(minSigmaTextField_.getText()) >=
               Double.parseDouble(maxSigmaTextField_.getText() ))
          maxSigmaTextField_.setText( Double.toString
                  (Double.parseDouble(minSigmaTextField_.getText()) + 1));
    }

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {
       if (ft_ != null && ft_.isRunning())
          ft_.stop();
       aStop_.set(true);   
    }

    private void minNrPhotonsTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
        if (Double.parseDouble(minNrPhotonsTextField_.getText()) >=
               Double.parseDouble(maxNrPhotonsTextField_.getText() ))
          minNrPhotonsTextField_.setText( Double.toString
                  (Double.parseDouble(maxNrPhotonsTextField_.getText()) - 1));
    }

    private void maxNrPhotonsTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
        if (Double.parseDouble(maxNrPhotonsTextField_.getText()) <=
           Double.parseDouble(minNrPhotonsTextField_.getText() ))
        maxNrPhotonsTextField_.setText( Double.toString
           (Double.parseDouble(minNrPhotonsTextField_.getText()) + 1));
    }

    private void showButtonActionPerformed(java.awt.event.ActionEvent evt) {
       DataCollectionForm dcForm = DataCollectionForm.getInstance();
       dcForm.setVisible(true);
    }


   private boolean showNoiseTolerance() {
       ImagePlus siPlus;
       try {
          siPlus = IJ.getImage();
       } catch (Exception e) {
          return false;
       }
       if (ip_ != siPlus)
          ip_ = siPlus;

       // Roi originalRoi = siPlus.getRoi();
       // Find maximum in Roi, might not be needed....
      try {
         int val = Integer.parseInt(noiseToleranceTextField_.getText());
         int halfSize = Integer.parseInt(boxSizeTextField.getText()) / 2;
         Polygon pol = FindLocalMaxima.FindMax(siPlus, 2* halfSize, val, preFilterType_);
         // pol = FindLocalMaxima.noiseFilter(siPlus.getProcessor(), pol, val);
         Overlay ov = new Overlay();
         for (int i = 0; i < pol.npoints; i++) {
            int x = pol.xpoints[i];
            int y = pol.ypoints[i];
            ov.add(new Roi(x - halfSize, y - halfSize, 2 * halfSize, 2 * halfSize));
         }
         labelNPoints_.setText("n: " + pol.npoints);
         siPlus.setOverlay(ov);
         siPlus.setHideOverlay(false);
      } catch (NumberFormatException nfEx) {
         // nothing to do
      }
      return true;
   }


   private void readParmsButton_ActionPerformed(java.awt.event.ActionEvent evt) {
      // should not have made this a push button...
      readParmsButton_.setSelected(false);
      // take the active ImageJ image
      ImagePlus siPlus;
      try {
         siPlus = IJ.getImage();
      } catch (Exception ex) {
         return;
      }
      if (ip_ != siPlus) {
         ip_ = siPlus;
      }
      try {
         Class<?> mmWin = Class.forName("org.micromanager.MMWindow");
         Constructor[] aCTors = mmWin.getDeclaredConstructors();
         aCTors[0].setAccessible(true);
         Object mw = aCTors[0].newInstance(siPlus);
         Method[] allMethods = mmWin.getDeclaredMethods();
         
         // assemble all methods we need
         Method mIsMMWindow = null;
         Method mGetSummaryMetaData = null;
         Method mGetImageMetaData = null;
         for (Method m : allMethods) {
            String mname = m.getName();
            if (mname.startsWith("isMMWindow")
                    && m.getGenericReturnType() == boolean.class) {
               mIsMMWindow = m;
               mIsMMWindow.setAccessible(true);
            }
            if (mname.startsWith("getSummaryMetaData")
                    && m.getGenericReturnType() == JSONObject.class) {
               mGetSummaryMetaData = m;
               mGetSummaryMetaData.setAccessible(true);
            }
            if (mname.startsWith("getImageMetadata")
                    && m.getGenericReturnType() == JSONObject.class) {
               mGetImageMetaData = m;
               mGetImageMetaData.setAccessible(true);
            }

         }

         if (mIsMMWindow != null && (Boolean) mIsMMWindow.invoke(mw)) {
            JSONObject summary = null;
            int lastFrame = 0;
            if (mGetSummaryMetaData != null) {
               summary = (JSONObject) mGetSummaryMetaData.invoke(mw);
               try {
                  lastFrame = summary.getInt("Frames");
               } catch (JSONException ex) {
               }               
            }
            JSONObject im = null;
            JSONObject imLast = null;
            if (mGetImageMetaData != null) {
               im = (JSONObject) mGetImageMetaData.invoke(mw, 0, 0, 0, 0);
               if (lastFrame > 0)
                  imLast = (JSONObject) 
                          mGetImageMetaData.invoke(mw, 0, 0, lastFrame - 1, 0);              
            }
                                  
            if (summary != null && im != null) {

               // it may be better to read the timestamp of the first and last frame and deduce interval from there
               if (summary.has("Interval_ms")) {
                  try {
                     timeIntervalTextField_.setText(NumberUtils.doubleToDisplayString(summary.getDouble("Interval_ms")));
                     timeIntervalTextField_.setBackground(Color.lightGray);
                  } catch (JSONException jex) {
                     // nothing to do
                  }
               }
               if (summary.has("PixelSize_um")) {
                  try {
                     pixelSizeTextField_.setText(NumberUtils.doubleToDisplayString(summary.getDouble("PixelSize_um") * 1000.0));
                     pixelSizeTextField_.setBackground(Color.lightGray);
                  } catch (JSONException jex) {
                     System.out.println("Error");
                  }

               }
               double emGain = -1.0;
               boolean conventionalGain = false;


               // find amplifier for Andor camera
               try {
                  String camera = im.getString("Core-Camera");
                  if (im.getString(camera + "-Output_Amplifier").equals("Conventional")) {
                     conventionalGain = true;
                  }
                  // TODO: find amplifier for other cameras

                  // find gain for Andor:
                  try {
                     emGain = im.getDouble(camera + "-Gain");
                  } catch (JSONException jex) {
                     try {
                        emGain = im.getDouble(camera + "-EMGain");
                     } catch (JSONException jex2) {
                        // key not found, nothing to do
                     }
                  }

               } catch (JSONException ex) {
                  // tag not found...
               }


               if (conventionalGain) {
                  emGain = 1;
               }
               if (emGain > 0) {
                  emGainTextField_.setText(NumberUtils.doubleToDisplayString(emGain));
                  emGainTextField_.setBackground(Color.lightGray);
               }

               // Get time stamp from first and last frame
               try {
                  double firstTimeMs = im.getDouble("ElapsedTime-ms");
                  double lastTimeMs = imLast.getDouble("ElapsedTime-ms");
                  double intervalMs = (lastTimeMs - firstTimeMs) / lastFrame;
                  timeIntervalTextField_.setText(
                          NumberUtils.doubleToDisplayString(intervalMs));
                  timeIntervalTextField_.setBackground(Color.lightGray);
               } catch (JSONException jex) {
               }

            }
         }

      } catch (ClassNotFoundException ex) {
      } catch (InstantiationException ex) {
         //Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IllegalAccessException ex) {
         //Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IllegalArgumentException ex) {
         //Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
      } catch (InvocationTargetException ex) {
         //Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
      }

   }

   private void showOverlay_ActionPerformed(java.awt.event.ActionEvent evt) {
      if (showOverlay_.isSelected()) {
         if (showNoiseTolerance()) {
            showOverlay_.setText("hide");
         }
      } else {
         ImagePlus siPlus;
         try {
            siPlus = IJ.getImage();
         } catch (Exception e) {
            return;
         }
         siPlus.setHideOverlay(true);
         showOverlay_.setText("show");
      }
   }
   
   private void mTrackButton_ActionPerformed(java.awt.event.ActionEvent evt) {

      // Poor way of tracking multiple spots by running sequential tracks
      // TODO: optimize
      final ImagePlus siPlus;
      try {
         siPlus = IJ.getImage();
      } catch (Exception e) {
         return;
      }
      if (ip_ != siPlus) {
         ip_ = siPlus;
      }

      Runnable mTracker = new Runnable() {
         @Override
         public void run() {
            aStop_ .set(false);
            int val = Integer.parseInt(noiseToleranceTextField_.getText());
            int halfSize = Integer.parseInt(boxSizeTextField.getText()) / 2;

            // If ROI manager is used, use RoiManager Rois
            //  may be dangerous if the user is not aware
            RoiManager roiM = RoiManager.getInstance();
            Roi[] rois = null;
            if (roiM != null) {
               rois = roiM.getSelectedRoisAsArray();
            }
            if (rois != null && rois.length > 0) {
               for (Roi roi : rois) {
                  siPlus.setRoi(roi, false);
                  Polygon pol = FindLocalMaxima.FindMax(siPlus, halfSize, val, preFilterType_);
                  for (int i = 0; i < pol.npoints && !aStop_.get(); i++) {
                     int x = pol.xpoints[i];
                     int y = pol.ypoints[i];
                     siPlus.setRoi(x - 2 * halfSize, y - 2 * halfSize, 4 * halfSize, 4 * halfSize);
                     GaussianTrackThread tT = new GaussianTrackThread(siPlus, 
                             FindLocalMaxima.FilterType.NONE);
                     updateValues(tT);
                     tT.trackGaussians(true);
                  }
               }
            } else {  // no Rois in RoiManager
               Polygon pol = FindLocalMaxima.FindMax(siPlus, halfSize, val, preFilterType_);
               for (int i = 0; i < pol.npoints && !aStop_.get(); i++) {
                  int x = pol.xpoints[i];
                  int y = pol.ypoints[i];
                  siPlus.setRoi(x - 2 * halfSize, y - 2 * halfSize, 4 * halfSize, 4 * halfSize);
                  GaussianTrackThread tT = new GaussianTrackThread(siPlus,
                          FindLocalMaxima.FilterType.NONE);
                  updateValues(tT);
                  tT.trackGaussians(true);
               }
            }
         }
      };

      (new Thread(mTracker)).start();

   }

   private void allPosButton_ActionPerformed(java.awt.event.ActionEvent evt) {
      ImagePlus ip;
      try {
          ip = IJ.getImage();
      } catch (Exception e) {
          return;
      }
      DisplayWindow dw = studio_.displays().getCurrentWindow();

      int nrPos = 1;
      if ( ! (dw == null || ip != dw.getImagePlus())) {
         nrPos = dw.getDatastore().getAxisLength(Coords.STAGE_POSITION);
      }
      if (nrPos > 1) {
         posTextField_.setText("1-" + nrPos);
      }
      
   }

   private void currentPosButton_ActionPerformed(java.awt.event.ActionEvent evt) {
      ImagePlus ip;
      try {
          ip = IJ.getImage();
      } catch (Exception e) {
          return;
      }
      DisplayWindow dw = studio_.displays().getCurrentWindow();

      int pos = 1;
      if ( ! (dw == null || ip != dw.getImagePlus())) {
         pos = dw.getDisplayedImages().get(0).getCoords().getStagePosition() + 1;
      }
      posTextField_.setText("" + pos);
   }

   public void updateValues(GaussianInfo tT) {
      try {
         tT.setNoiseTolerance(Integer.parseInt(noiseToleranceTextField_.getText()));
         tT.setPhotonConversionFactor(NumberUtils.displayStringToDouble(photonConversionTextField_.getText()));
         tT.setGain(NumberUtils.displayStringToDouble(emGainTextField_.getText()));
         tT.setPixelSize((float) NumberUtils.displayStringToDouble(pixelSizeTextField_.getText()));
         tT.setZStackStepSize((float) NumberUtils.displayStringToDouble(zStepTextField_.getText()));
         tT.setTimeIntervalMs(NumberUtils.displayStringToDouble(timeIntervalTextField_.getText()));
         tT.setBaseLevel(NumberUtils.displayStringToDouble(baseLevelTextField.getText()));
         tT.setUseWidthFilter(filterDataCheckBoxWidth_.isSelected());
         tT.setSigmaMin(NumberUtils.displayStringToDouble(minSigmaTextField_.getText()));
         tT.setSigmaMax(NumberUtils.displayStringToDouble(maxSigmaTextField_.getText()));
         tT.setUseNrPhotonsFilter(filterDataCheckBoxNrPhotons_.isSelected());
         tT.setNrPhotonsMin(NumberUtils.displayStringToDouble(minNrPhotonsTextField_.getText()));
         tT.setNrPhotonsMax(NumberUtils.displayStringToDouble(maxNrPhotonsTextField_.getText()));
         tT.setMaxIterations(Integer.parseInt(maxIterationsTextField_.getText()));
         tT.setBoxSize(Integer.parseInt(boxSizeTextField.getText()));
         tT.setShape(fitDimensionsComboBox1_.getSelectedIndex() + 1);
         tT.setFitMode(fitMethodComboBox1_.getSelectedIndex() + 1);
         tT.setEndTrackBool(endTrackCheckBox_.isSelected());
         tT.setEndTrackAfterNFrames((Integer) endTrackSpinner_.getValue());
         tT.setSkipChannels(skipChannelsCheckBox_.isSelected());
         if (skipChannelsCheckBox_.isSelected()) {
            /*
            String[] parts = channelsToSkip_.getText().split(",");
            int[] result = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
               result[i] =  NumberUtils.displayStringToInt(parts[i]);
            }
            tT.setChannelsToSkip(result);
            */
         }
      } catch (NumberFormatException ex) {
         JOptionPane.showMessageDialog(null, "Error interpreting input: " + ex.getMessage());
      } catch (ParseException ex) {
         JOptionPane.showMessageDialog(null, "Error interpreting input: " + ex.getMessage());
      }
   }

   @Override
   public void imageOpened(ImagePlus ip) {
      imageUpdated(ip);
   }

   @Override
   public void imageClosed(ImagePlus ip) {
         //   System.out.println("Closed");
   }

   @Override
   public void imageUpdated(ImagePlus ip) {
      if (!WINDOWOPEN) {
         return;
      }
      if (ip != ip_) {
         pixelSizeTextField_.setBackground(Color.white);
         emGainTextField_.setBackground(Color.white);      
         timeIntervalTextField_.setBackground(Color.white);
    
         if (ip_ != null) {
            ip_.setOverlay(null);
            ip_.setHideOverlay(true);
         }
         ip_ = ip;
      }
         
      if (showOverlay_.isSelected()) {
         
         // note that there is confusion about frames versus slices
         int frame = 1;
         if (ip.getNFrames() > 1)
            frame = ip.getFrame();
         else if (ip.getNSlices() > 1)
            frame = ip.getSlice();
         
         if (lastFrame_ != frame) {
            lastFrame_ = frame;
            showNoiseTolerance();
         }
      }
   }

}