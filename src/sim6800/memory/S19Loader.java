package sim6800.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class S19Loader {

    public static int load(String fileName, Ram memory,
                           boolean asRom) throws IOException {

        List<String> lines = Files.readAllLines(Path.of(fileName));
        int totalBytes = 0;

        // Go through each line in the file
        for (String line : lines) {

            line = line.trim().toUpperCase();

            if (line.isEmpty()) {
                continue;
            }

            if (!line.startsWith("S") || line.length() < 4) {
                throw new IOException("Invalid S19 record");
            }

            char type = line.charAt(1);

            // We only need S0, S1 and S9 records
            if (type != '0' && type != '1' && type != '9') {
                throw new IOException("Unsupported record: S" + type);
            }

            try {
                // Read the byte count
                int count = Integer.parseInt(
                        line.substring(2, 4), 16);

                if (count < 3 || line.length() != 4 + count * 2) {
                    throw new IOException("Invalid record length");
                }

                // S9 means the end of the file
                if (type == '9') {
                    break;
                }

                // Skip S0 header records
                if (type == '0') {
                    continue;
                }

                // Get the starting memory address
                int address = Integer.parseInt(
                        line.substring(4, 8), 16);

                int dataBytes = count - 3;

                if (address + dataBytes > 65536) {
                    throw new IOException("Address outside memory");
                }

                // Read the data bytes and put them in memory
                for (int i = 0; i < dataBytes; i++) {

                    int start = 8 + i * 2;

                    int value = Integer.parseInt(
                            line.substring(start, start + 2), 16);

                    memory.loadByte(address + i, value, asRom);
                    totalBytes++;
                }

            } catch (NumberFormatException |
                     IndexOutOfBoundsException ex) {

                throw new IOException("Invalid hex data", ex);

            } catch (IllegalArgumentException ex) {

                throw new IOException(ex.getMessage(), ex);
            }
        }

        return totalBytes;
    }
}