package com.hbiede.gui;

import com.hbiede.ContactPhotos;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

public class ContactGUI {
    private static final int LARGE_SIZE_FILE_LENGTH = 0x6400000;
    public JPanel mainPanel;
    public JButton fileButton;
    public JButton directoryButton;
    public JButton runButton;
    private ContactPhotos outputTask = new ContactPhotos();
    private JFrame frame;
    private JProgressBar progressBar;
    @SuppressWarnings("unused")
    private JPanel radioPanel;
    private JRadioButton firstLastRadioButton;
    private JRadioButton lastFirstRadioButton;
    @SuppressWarnings("unused")
    private JLabel radioTitleLabel;
    private JLabel contactCountLabel;
    @SuppressWarnings("unused")
    private ButtonGroup radioButtonGroup;
    private boolean outputNameLastFirst;
    @Nullable
    private File inputFile;
    private File outputDirectory;


    public ContactGUI() {
        super();
        outputNameLastFirst = true;
        fileButton.addActionListener(new ButtonListener());
        directoryButton.addActionListener(new ButtonListener());
        runButton.addActionListener(new ButtonListener());
        firstLastRadioButton.addActionListener(new RadioListener());
        lastFirstRadioButton.addActionListener(new RadioListener());
        contactCountLabel.setVisible(false);
    }

    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    private class RadioListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            outputNameLastFirst = "LastFirst".equals(e.getActionCommand());
            outputTask.setOutputNameLastFirst(outputNameLastFirst);
        }
    }

    private class ButtonListener implements ActionListener, PropertyChangeListener {

        // Called when a button is pressed
        @Override
        public void actionPerformed(ActionEvent e) {
            progressBar.setValue(0);
            contactCountLabel.setVisible(false);
            if (e.getSource().equals(fileButton)) {
                // Open File selection window
                final JFileChooser fc = new JFileChooser();
                // Only allow vCards
                fc.setFileFilter(new FileNameExtensionFilter("vCard", "vcf"));
                // Only set the file location if it is an affirmative selection
                int returnVal = fc.showOpenDialog(mainPanel);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    inputFile = fc.getSelectedFile();

                    // Check if there is enough free space on the disk to store the photos
                    // Working under the assumption that inputFile's file size serves as a
                    if (inputFile.length() >= inputFile.getFreeSpace()) {
                        JOptionPane.showMessageDialog(new JFrame("Not Enough Space"), "Not Enough Free Space!\nPlease clear out some hard drive space.", "Dialog",
                                JOptionPane.ERROR_MESSAGE);
                        inputFile = null;
                    } else {
                        // Give a warning if the file is rather large (100MB+)
                        if (inputFile.length() > LARGE_SIZE_FILE_LENGTH) {
                            contactCountLabel.setText("Loading...");
                            contactCountLabel.setVisible(true);
                            System.out.println("Big file");
                        }

                        outputTask.setInputFile(inputFile);
                    }
                }
            } else if (e.getSource().equals(directoryButton)) {
                // Open Directory selection window
                final JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                // Only set the file location if it is an affirmative selection
                int returnVal = fc.showOpenDialog(mainPanel);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    outputDirectory = fc.getSelectedFile();
                    outputTask.setOutputDirectory(outputDirectory);
                }
            } else if ("run".equals(e.getActionCommand())) {
                // Run Photo Output
                progressBar.setValue(0);
                contactCountLabel.setText("0 contact photos exported");

                // allow the action to be stopable
                runButton.setActionCommand("cancel");
                runButton.setText("Cancel");

                // setup the threaded task
                outputTask.addPropertyChangeListener(this);
                if (outputTask.isRunnable()) outputTask.execute();
                else {
                    //restore properties to non-running state and disable run button
                    contactCountLabel.setText("Problem Occurred with Property Setting");
                    runButton.setActionCommand("run");
                    runButton.setText("Run");
                    runButton.setEnabled(false);
                }

                directoryButton.setEnabled(false);
                runButton.setEnabled(false);
                contactCountLabel.setVisible(true);

            } else {
                // cancel button pressed
                outputTask.cancel(true);
                contactCountLabel.setVisible(true);
                contactCountLabel.setText(String.format("%d contact photos exported before cancelling", outputTask.getContactsOutputSoFar()));
                frame.pack();

                //reenable buttons
                directoryButton.setEnabled(true);
                runButton.setEnabled(true);

                //reset the task
                outputTask = new ContactPhotos();
                outputTask.setOutputNameLastFirst(outputNameLastFirst);
                outputTask.setOutputDirectory(outputDirectory);
                outputTask.setInputFile(inputFile);
            }

            // Enable the run button once there are valid selections
            runButton.setEnabled(inputFile != null && outputDirectory != null);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("progress".equals(evt.getPropertyName())) {
                int progress = (Integer) evt.getNewValue();
                progressBar.setValue(progress);
                contactCountLabel.setText(String.format("%d contact photos exported", outputTask.getContactsOutputSoFar()));
            }
        }
    }
}
