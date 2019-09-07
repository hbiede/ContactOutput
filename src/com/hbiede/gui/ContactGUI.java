package com.hbiede.gui;

import com.hbiede.ContactPhotos;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;

public class ContactGUI {
    public JPanel mainPanel;
    private JButton fileButton;
    private JButton directoryButton;
    private JButton runButton;
    private JProgressBar progressBar;
    private JPanel radioPanel;
    private JRadioButton firstLastRadioButton;
    private JRadioButton lastFirstRadioButton;
    private JLabel radioTitleLabel;
    private JLabel contactCountLabel;
    private ButtonGroup radioButtonGroup;
    private boolean outputNameLastFirst;
    private File inputFile;
    private File outputDirectory;
    ContactPhotos outputTask = new ContactPhotos();


    public ContactGUI() {
        outputNameLastFirst = true;
        inputFile = outputDirectory = null;
        fileButton.addActionListener(new ButtonListener());
        directoryButton.addActionListener(new ButtonListener());
        runButton.addActionListener(new ButtonListener());
        firstLastRadioButton.addActionListener(new RadioListener());
        lastFirstRadioButton.addActionListener(new RadioListener());
    }

    private static int countContacts(File contactsFile) {
        BufferedReader br;
        int contactsCount = 0;
        try {
            br = new BufferedReader(new FileReader(contactsFile));
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            return 0;
        }

        String newLine;
        try {
            newLine = br.readLine();
            while (newLine != null) {
                if (newLine.contains("PHOTO;")) {
                    contactsCount++;
                }
                newLine = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            // Shouldn't ever come up. Just pleasing the compiler
            System.out.println("Well, that's a problem...");
            e.printStackTrace();
        }
        return contactsCount;
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
                int returnVal = fc.showOpenDialog(ContactGUI.this.mainPanel);
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
                        if (inputFile.length() > 0x6400000) {
                            contactCountLabel.setText("Loading...");
                            contactCountLabel.setVisible(true);
                            System.out.println("Big file");
                        }
                        // Count the number of contacts in the file
                        progressBar.setMaximum(100);
                        outputTask.setTotalContacts(countContacts(inputFile));
                        outputTask.setInputFile(inputFile);
                    }
                }
            } else if (e.getSource().equals(directoryButton)) {
                // Open Directory selection window
                final JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                // Only set the file location if it is an affirmative selection
                int returnVal = fc.showOpenDialog(ContactGUI.this.mainPanel);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    outputDirectory = fc.getSelectedFile();
                    outputTask.setOutputDirectory(outputDirectory);
                }
            } else if ("run".equals(e.getActionCommand())) {
                // Run Photo Output
                progressBar.setValue(0);

                // allow the action to be stopable
                runButton.setActionCommand("cancel");
                runButton.setText("Cancel");

                outputTask.addPropertyChangeListener(this);
                outputTask.setRunButton(runButton);
                outputTask.execute();

                contactCountLabel.setVisible(true);

            } else {
                outputTask.cancel(true);
                contactCountLabel.setVisible(true);
                contactCountLabel.setText(String.format("%d contact photos exported before cancelling", outputTask.getContactsOutputSoFar()));
            }

            // Enable the run button once there are valid selections
            runButton.setEnabled(inputFile != null && outputDirectory != null);
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if ("progress".equals(evt.getPropertyName())) {
                int progress = (Integer) evt.getNewValue();
                progressBar.setValue(progress);
                contactCountLabel.setText(String.format("%d contact photos exported", outputTask.getContactsOutputSoFar()));
            }
        }
    }
}
