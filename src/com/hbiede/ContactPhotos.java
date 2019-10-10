package com.hbiede;

import com.hbiede.gui.ContactGUI;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ContactPhotos extends SwingWorker<Void, Void> {

    @NonNls
    private static final String FILE_SEPARATOR = "/";
    @NonNls
    private static final String PHOTO = "PHOTO;";
    @NonNls
    private static final String NAME = "N:";
    @NonNls
    private static final String ORGANIZATION = "ORG:";
    @NonNls
    private static final String ONLINE_RESOURCE = "http";
    @NonNls
    private static final String IO_PROBLEM_RESOURCE_KEY = "IOProblem";
    private static final char SPACE_CHAR = ' ';
    @NonNls
    public static final String RUN_BUTTON_TEXT = "RunButtonText";
    private static final char VCF_FIELD_DELIMITER = ':';
    private static ContactGUI gui = new ContactGUI();
    private File inputFile = null;
    private File outputDirectory = null;
    private int totalContacts;
    private int contactsOutputSoFar = 0;
    private boolean outputNameLastFirst = true;

    public static void main(String[] args) {
        //noinspection Convert2Lambda
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame(Localizer.i18n_str("FrameTitle"));
                frame.setContentPane(gui.mainPanel);
                gui.setFrame(frame);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });
    }

    private static int countContacts(File contactsFile) {
        BufferedReader br;
        int contactsCount = 0;
        try {
            br = new BufferedReader(new FileReader(contactsFile, StandardCharsets.UTF_8));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return 0;
        }

        String newLine;
        try {
            newLine = br.readLine();
            while (newLine != null) {
                if (newLine.contains(PHOTO)) {
                    contactsCount++;
                }
                newLine = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            // Shouldn't ever come up. Just pleasing the compiler
            System.out.println(Localizer.i18n_str(IO_PROBLEM_RESOURCE_KEY));
            e.printStackTrace();
        }
        return contactsCount;
    }

    public boolean isRunnable() {
        return !(inputFile == null || outputDirectory == null || totalContacts == 0);
    }

    public void setInputFile(File inputFile) {
        if (inputFile != null && inputFile.canRead()) {
            this.inputFile = inputFile;
            setTotalContacts(countContacts(inputFile));
        }
    }

    public void setOutputDirectory(File outputDirectory) {
        if (outputDirectory != null && outputDirectory.canWrite()) this.outputDirectory = outputDirectory;
    }

    public int getContactsOutputSoFar() {
        return contactsOutputSoFar;
    }

    public void setOutputNameLastFirst(boolean outputNameLastFirst) {
        this.outputNameLastFirst = outputNameLastFirst;
    }

    private void setTotalContacts(int totalContacts) {
        if (totalContacts > 0) this.totalContacts = totalContacts;
    }

    @Override
    protected Void doInBackground() {
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(inputFile, StandardCharsets.UTF_8));
        } catch (IOException ioe) {
            System.out.println(Localizer.i18n_str("RequestValidVCF"));
            return null;
        }

        String newLine;
        try {
            newLine = br.readLine();
            @NonNls String contactName = "";
            StringBuilder photoString = new StringBuilder(0);
            boolean photoInProgress = false;

            // Create the output folder if it does not exist
            outputDirectory.mkdir();
            while (newLine != null && !isCancelled()) {
                if (newLine.startsWith(NAME)) {
                    // Get the name for a new Contact
                    if (outputNameLastFirst) {
                        String[] tokens = newLine.substring(2).split(";");
                        if (tokens.length == 1 || tokens[1] == null || tokens[1].trim().isEmpty()) {
                            contactName = tokens[0];
                        } else {
                            contactName = (tokens[0] == null || tokens[0].trim().isEmpty()) ? tokens[1] : tokens[0] + ", " + tokens[1];
                        }
                    } else {
                        contactName = newLine.substring(3);
                    }
                } else if (newLine.startsWith(ORGANIZATION) && contactName.isEmpty()) { // contact name must be blank so as to prevent a person with a company name associated from being named after the company
                    contactName = newLine.substring(4).split(";")[0];
                } else if (newLine.contains(PHOTO)) {
                    // Start a new photo string
                    photoString.delete(0, photoString.length());
                    photoString.append(newLine.substring(newLine.indexOf(VCF_FIELD_DELIMITER) + 1));
                    photoInProgress = true;
                } else if (photoInProgress && newLine.charAt(0) == SPACE_CHAR) {
                    // Add to the existing photo string if the line starts with a space (an indent
                    // to signify the photo is continuing)
                    photoString.append(newLine.substring(1));
                } else if (photoInProgress) {
                    // Finish a photo string if the line is not indented while a photo string is
                    // being built
                    photoInProgress = false;
                    try {
                        File outputFile = new File(outputDirectory.getAbsolutePath() + FILE_SEPARATOR + contactName + ".jpg");
                        OutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));
                        if (photoString.toString().contains(ONLINE_RESOURCE)) {
                            InputStream imageReader = new BufferedInputStream(new URL(photoString.toString()).openStream());
                            int imageByte;
                            //noinspection NestedAssignment - It is used though...
                            while ((imageByte = imageReader.read()) != -1) {
                                os.write(imageByte);
                            }
                            imageReader.close();
                        } else {
                            // Decode and store the output to the output folder created earlier
                            os.write(Base64.getDecoder().decode(photoString.toString()));
                        }
                        contactsOutputSoFar++;
                        setProgress(100 * contactsOutputSoFar / totalContacts);
                        os.close();
                        contactName = "";
                    } catch (Exception e) {
                        System.out.printf(Localizer.i18n_str("BrokenPhoto"), contactName);
                        System.out.println(); // new line character
                        System.err.println(e.getMessage());
                        continue;
                    }

                }
                newLine = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            System.out.println(Localizer.i18n_str(IO_PROBLEM_RESOURCE_KEY));
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void done() {
        @NonNls String terminatingActionCommand = "run";
        gui.runButton.setActionCommand(terminatingActionCommand);
        gui.runButton.setText(Localizer.i18n_str(RUN_BUTTON_TEXT));
        gui.fileButton.setEnabled(true);
        gui.directoryButton.setEnabled(true);
    }
}
