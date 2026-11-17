package sim6800.cpu;

import sim6800.memory.Memory;

// The 6800 itself: registers, condition codes, fetch/decode/execute.
// Talks to the outside only through Memory. Instructions go in step().
// Everything is int and masked: 8-bit regs & 0xFF, 16-bit regs & 0xFFFF.
public class Cpu {
    int a, b;          // accumulators (8-bit)
    int x, sp, pc;     // index, stack pointer, program counter (16-bit)
    int ccr;           // condition codes (8-bit)
    boolean waiting;   // set by WAI; run() stops here
    private final Memory memory;

    // CCR bit masks:  bit 7 6 5 4 3 2 1 0  =  1 1 H I N Z V C
    public static final int C = 0x01;
    public static final int V = 0x02;
    public static final int Z = 0x04;
    public static final int N = 0x08;
    public static final int I = 0x10;
    public static final int H = 0x20;
    public static final int CCR_FIXED = 0xC0;   // bits 6,7 always 1

    public Cpu(Memory memory) {
        this.memory = memory;
        reset();
    }

    // RESET: mask interrupts, load PC from the vector at FFFE/FFFF
    public void reset() {
        a = 0; b = 0; x = 0; sp = 0;
        ccr = CCR_FIXED | I;
        waiting = false;
        pc = read16(0xFFFE);
    }

    // ---- flags: the only place CCR bits are read or changed ----
    public boolean flag(int mask) {
        return (ccr & mask) != 0;
    }

    public void setFlag(int mask, boolean on) {
        ccr = on ? ccr | mask : ccr & ~mask;
    }

    public void setNZ8(int value) {
        setFlag(N, (value & 0x80) != 0);
        setFlag(Z, (value & 0xFF) == 0);
    }

    public void setNZ16(int value) {
        setFlag(N, (value & 0x8000) != 0);
        setFlag(Z, (value & 0xFFFF) == 0);
    }

    // ---- bus: big-endian, high byte at the lower address ----
    public int read8(int addr) {
        return memory.read(addr & 0xFFFF);
    }

    public int read16(int addr) {
        return (read8(addr) << 8) | read8(addr + 1);
    }

    public void write8(int addr, int v) {
        memory.write(addr & 0xFFFF, v & 0xFF);
    }

    public void write16(int addr, int v) {
        write8(addr, v >> 8);
        write8(addr + 1, v);
    }

    // read at PC and advance; used for opcodes and operands
    public int fetch8() {
        int v = read8(pc);
        pc = (pc + 1) & 0xFFFF;
        return v;
    }

    public int fetch16() {
        int high = fetch8();
        int low = fetch8();
        return (high << 8) | low;
    }

    // ---- stack: SP points at the next free byte, grows downward ----
    public void push8(int v) {
        write8(sp, v);
        sp = (sp - 1) & 0xFFFF;
    }

    public int pull8() {
        sp = (sp + 1) & 0xFFFF;
        return read8(sp);
    }

    // low byte first so the high byte lands at the lower address
    public void push16(int v) {
        push8(v);
        push8(v >> 8);
    }

    public int pull16() {
        int high = pull8();
        int low = pull8();
        return (high << 8) | low;
    }

    // ---- execution ----
    public void step() {
        // int opcode = fetch8(); -> opcode table -> execute
    }

    public void run() {
        while (!waiting) {
            step();
        }
    }

    // ---- getters for the UI ----
    public int getA()   { return a; }
    public int getB()   { return b; }
    public int getX()   { return x; }
    public int getSp()  { return sp; }
    public int getPc()  { return pc; }
    public int getCcr() { return ccr; }
    public boolean isWaiting() { return waiting; }
}
