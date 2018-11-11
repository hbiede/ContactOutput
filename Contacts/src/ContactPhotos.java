import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Scanner;

public class ContactPhotos {

	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);

		System.out.print("vCard Location:");
		File inputFile = new File(in.nextLine());
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
			new File(inputFile.getParentFile().getAbsolutePath() + "/Output-Photos").mkdir();
			while (newLine != null) {
				if (newLine.contains("FN:")) {
					contactName = newLine.substring(3);
				} else if (newLine.contains("PHOTO;")) {
					photoString = newLine.substring(newLine.indexOf(":") + 1);
					photoInProgress = true;
				} else if (photoInProgress && newLine.charAt(0) == ' ') {
					photoString += newLine.substring(1);
				} else if (photoInProgress) {
					photoInProgress = false;
					byte[] imageBytes;
					try {
						Decoder decoder = Base64.getDecoder();
						imageBytes = decoder.decode(photoString);
					} catch (Exception e) {
						System.out.println(photoString);
						continue;
					}
					File outputFile = new File(inputFile.getParentFile().getAbsolutePath()
							+ "/Output-Photos/" + contactName + ".jpg");
					OutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));
					os.write(imageBytes);
					System.out.println("\n\n\n" + outputFile.getAbsolutePath());
					os.close();
				}
				newLine = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Well, that's a problem.");
		}
	}
}
