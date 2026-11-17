package sim6800.memory;

// Plain read/write memory for the full 16-bit address space.
// Masks: & 0xFFFF = 16 address pins (wrap instead of throw), & 0xFF = 8 data pins.
public class Ram implements Memory {
    private final int[] cells = new int[65536];

    public int read(int address) {
        return cells[address & 0xFFFF];
    }

    public void write(int address, int value) {
        cells[address & 0xFFFF] = value & 0xFF;
    }

    // used by the UI's "clear memory" command
    public void clear() {
        java.util.Arrays.fill(cells, 0);
    }
}
