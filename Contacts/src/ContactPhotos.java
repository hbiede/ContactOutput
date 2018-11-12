import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Scanner;

public class ContactPhotos {

	public static final String OUTPUT_FOLDER_NAME = "Output-Photos";
	public static final String DIR_SEP_CHAR = "/";
	public static final String OUTPUT_DIR = DIR_SEP_CHAR + OUTPUT_FOLDER_NAME + DIR_SEP_CHAR;

	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);

		// Get and import a vCard
		System.out.print("vCard Location:");
		File inputFile = new File(in.nextLine());
		in.close();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(inputFile));
		} catch (FileNotFoundException e) {
			System.out.println("Please input a valid vCard file.");
			return;
		}

		String newLine;
		try {
			newLine = br.readLine();
			String contactName = "";
			String photoString = "";
			boolean photoInProgress = false;
			int contactCount = 0;

			// Create the output folder if it does not exist
			new File(inputFile.getParentFile().getAbsolutePath() + OUTPUT_DIR).mkdir();
			while (newLine != null) {
				if (newLine.contains("FN:")) {
					// Get the name for a new Contact
					contactName = newLine.substring(3);
				} else if (newLine.contains("PHOTO;")) {
					// Start a new photo string
					photoString = newLine.substring(newLine.indexOf(":") + 1);
					photoInProgress = true;
				} else if (photoInProgress && newLine.charAt(0) == ' ') {
					// Add to the existing photo string if the line starts with a space (an indent
					// to signify the photo is continuing)
					photoString += newLine.substring(1);
				} else if (photoInProgress) {
					// Finish a photo string if the line is not indented while a photo string is
					// being built
					photoInProgress = false;
					byte[] imageBytes;
					try {
						// Decodes the photo string according to Base 64 to a byte array
						imageBytes = Base64.getDecoder().decode(photoString);
					} catch (Exception e) {
						System.out.println("Broken photo string: " + photoString);
						continue;
					}
					// Store the output to the output folder created earlier as "First Last.jpg"
					File outputFile = new File(inputFile.getParentFile().getAbsolutePath()
							+ OUTPUT_DIR + contactName + ".jpg");
					OutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));
					os.write(imageBytes);
					os.close();
					contactCount++;
				}
				newLine = br.readLine();
			}
			br.close();
			System.out.println(contactCount + " contact photos exported.");
		} catch (IOException e) {
			System.out.println("Well, that's a problem...");
			e.printStackTrace();
		}
	}
}
