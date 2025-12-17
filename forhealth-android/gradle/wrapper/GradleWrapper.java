import java.io.*;
import java.net.*;

public class GradleWrapper {
    public static void main(String[] args) {
        System.out.println("Simple Gradle Wrapper");
        System.out.println("This is a minimal wrapper to download and run Gradle");
        
        String gradleVersion = "8.2";
        String gradleHome = System.getProperty("user.home") + File.separator + ".gradle";
        File gradleDir = new File(gradleHome, "wrapper/dists/gradle-" + gradleVersion + "-bin");
        
        if (!gradleDir.exists()) {
            System.out.println("Gradle " + gradleVersion + " not found, would download...");
            System.out.println("Please install Gradle manually or use Android Studio");
            System.exit(1);
        }
        
        System.out.println("Gradle found at: " + gradleDir.getAbsolutePath());
    }
}