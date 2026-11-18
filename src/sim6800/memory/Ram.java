package sim6800.memory;

public class Ram implements Memory {

    // 64 KB of memory
    private final int[] cells = new int[65536];

    // Keeps track of which addresses are ROM
    private final boolean[] rom = new boolean[65536];

    // For connecting devices later
    private final IoPort[] ports = new IoPort[65536];

    public Ram() {

        // ROM area from our Phase 1 memory map
        for (int i = 0xFC00; i <= 0xFFFF; i++) {
            rom[i] = true;
        }
    }

    // Read a value from memory
    @Override
    public int read(int address) {

        address = address & 0xFFFF;

        if (ports[address] != null) {
            return ports[address].read(address) & 0xFF;
        }

        return cells[address];
    }

    // Write a value to memory
    @Override
    public void write(int address, int value) {

        address = address & 0xFFFF;
        value = value & 0xFF;

        if (ports[address] != null) {
            ports[address].write(address, value);
            return;
        }

        // Normal CPU writes cannot change ROM
        if (rom[address]) {
            return;
        }

        cells[address] = value;
    }

    // Used when loading an S19 file
    public void loadByte(int address, int value, boolean asRom) {

        address = address & 0xFFFF;
        value = value & 0xFF;

        if (ports[address] != null) {
            throw new IllegalArgumentException(
                    "Cannot load into an I/O address");
        }

        if (!asRom && rom[address]) {
            throw new IllegalArgumentException(
                    "Cannot load RAM into ROM");
        }

        cells[address] = value;

        if (asRom) {
            rom[address] = true;
        }
    }

    // Connect a device to a range of memory addresses
    public void connectPort(int start, int end, IoPort port) {

        if (start < 0 || end > 0xFFFF ||
                start > end || port == null) {
            throw new IllegalArgumentException(
                    "Invalid I/O range");
        }

        for (int i = start; i <= end; i++) {
            if (rom[i]) {
                throw new IllegalArgumentException(
                        "I/O address overlaps ROM");
            }
        }

        for (int i = start; i <= end; i++) {
            ports[i] = port;
        }
    }

    // Clear RAM without deleting ROM
    public void clear() {

        for (int i = 0; i < cells.length; i++) {

            if (!rom[i] && ports[i] == null) {
                cells[i] = 0;
            }
        }
    }
}



