package sim6800.memory;

// What the CPU sees on its bus. RAM, ROM and later the keypad/display all
// implement this, so the CPU never knows what is behind an address.
// address: 0..0xFFFF, value: 0..0xFF, both as int (Java byte is signed).
public interface Memory {
    int read(int address);
    void write(int address, int value);
}
