package sim6800.cpu;

import sim6800.memory.Memory;

// The 6800 itself: registers, condition codes, fetch/decode/execute.
// Talks to the outside only through Memory. step() = fetch/decode/execute.
// Everything is int and masked: 8-bit regs & 0xFF, 16-bit regs & 0xFFFF.
public class Cpu {
    int a, b;          // accumulators (8-bit)
    int x, sp, pc;     // index, stack pointer, program counter (16-bit)
    int ccr;           // condition codes (8-bit)
    boolean waiting;    // set by WAI; run() stops here
    boolean irqLine;    // IRQ pin held low by a device (level sensitive)
    boolean nmiPending; // NMI edge seen and not yet serviced (edge sensitive)
    private final Memory memory;

    // interrupt vectors (datasheet / Phase 1 Table 3.4)
    public static final int VEC_IRQ   = 0xFFF8;
    public static final int VEC_SWI   = 0xFFFA;
    public static final int VEC_NMI   = 0xFFFC;
    public static final int VEC_RESET = 0xFFFE;

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
        irqLine = false;
        nmiPending = false;
        pc = read16(VEC_RESET);
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

    // ---- addressing modes: return the operand's address ----
    // Called right after the opcode is fetched, so PC is on the first operand byte.
    int operandAddress(Opcode op) {
        switch (op.mode()) {
            case IMM: {                                   // operand is the next byte(s)
                int addr = pc;
                pc = (pc + op.bytes() - 1) & 0xFFFF;
                return addr;
            }
            case DIR: return fetch8();                    // page zero 0000-00FF
            case EXT: return fetch16();                   // full 16-bit address
            case IDX: return (x + fetch8()) & 0xFFFF;     // X + unsigned offset
            case REL: {                                   // PC + signed offset
                int offset = (byte) fetch8();             // cast makes it signed
                return (pc + offset) & 0xFFFF;            // PC already past the instruction
            }
            default:  return -1;                          // INH: no operand
        }
    }

    // ---- execution ----
    // One instruction: service a pending interrupt, else fetch/decode/execute.
    public void step() {
        if (nmiPending) {
            nmiPending = false;
            interrupt(VEC_NMI);
            return;
        }
        if (irqLine && !flag(I)) {
            interrupt(VEC_IRQ);
            return;
        }
        if (waiting) return;                              // WAI: idle until an interrupt

        int opcodeByte = fetch8();
        Opcode op = OpcodeTable.get(opcodeByte);
        if (op == null) {
            throw new IllegalStateException(
                String.format("illegal opcode %02X at %04X", opcodeByte, (pc - 1) & 0xFFFF));
        }
        int ea = operandAddress(op);
        execute(op.mnemonic(), ea);
    }

    public void run() {
        while (!waiting) {
            step();
        }
    }

    // ---- interrupt entry (Phase 1 Section 3.1.2) ----
    // Push PC, X, A, B, CCR (7 bytes, Table 3.3 order), set I, load the vector.
    // After WAI the registers are already on the stack, so skip the push.
    void interrupt(int vector) {
        if (!waiting) pushAll();
        waiting = false;
        setFlag(I, true);
        pc = read16(vector);
    }

    void pushAll() {
        push16(pc);
        push16(x);
        push8(a);
        push8(b);
        push8(ccr);
    }

    void pullAll() {
        ccr = pull8() | CCR_FIXED;
        b = pull8();
        a = pull8();
        x = pull16();
        pc = pull16();
    }

    // Devices call these (Phase 3). IRQ is a level: held until released.
    public void setIrqLine(boolean low) { irqLine = low; }
    // NMI is an edge: one call = one high-to-low transition.
    public void triggerNmi() { nmiPending = true; }

    // ---- instructions: control flow, stack, flags, interrupts ----
    // Data / arithmetic / logic instructions live in DataInstructions.
    void execute(String m, int ea) {
        switch (m) {
            // branches: conditions from the datasheet branch table
            case "BRA": branch(ea, true); break;
            case "BHI": branch(ea, !flag(C) && !flag(Z)); break;
            case "BLS": branch(ea, flag(C) || flag(Z)); break;
            case "BCC": branch(ea, !flag(C)); break;
            case "BCS": branch(ea, flag(C)); break;
            case "BNE": branch(ea, !flag(Z)); break;
            case "BEQ": branch(ea, flag(Z)); break;
            case "BVC": branch(ea, !flag(V)); break;
            case "BVS": branch(ea, flag(V)); break;
            case "BPL": branch(ea, !flag(N)); break;
            case "BMI": branch(ea, flag(N)); break;
            case "BGE": branch(ea, flag(N) == flag(V)); break;
            case "BLT": branch(ea, flag(N) != flag(V)); break;
            case "BGT": branch(ea, !flag(Z) && flag(N) == flag(V)); break;
            case "BLE": branch(ea, flag(Z) || flag(N) != flag(V)); break;

            // jumps and subroutines: return address = PC after the instruction
            case "JMP": pc = ea; break;
            case "JSR":
            case "BSR": push16(pc); pc = ea; break;
            case "RTS": pc = pull16(); break;

            // stack
            case "PSHA": push8(a); break;
            case "PSHB": push8(b); break;
            case "PULA": a = pull8(); break;
            case "PULB": b = pull8(); break;
            case "INS": sp = (sp + 1) & 0xFFFF; break;
            case "DES": sp = (sp - 1) & 0xFFFF; break;
            case "TSX": x = (sp + 1) & 0xFFFF; break;   // datasheet: X = SP + 1
            case "TXS": sp = (x - 1) & 0xFFFF; break;   // datasheet: SP = X - 1

            // condition code register
            case "TAP": ccr = a | CCR_FIXED; break;
            case "TPA": a = ccr | CCR_FIXED; break;
            case "CLC": setFlag(C, false); break;
            case "SEC": setFlag(C, true); break;
            case "CLI": setFlag(I, false); break;
            case "SEI": setFlag(I, true); break;
            case "CLV": setFlag(V, false); break;
            case "SEV": setFlag(V, true); break;
            case "NOP": break;

            // software interrupt and wait
            case "SWI": pushAll(); setFlag(I, true); pc = read16(VEC_SWI); break;
            case "WAI": pushAll(); waiting = true; break;
            case "RTI": pullAll(); break;

            default: DataInstructions.execute(this, m, ea);
        }
    }

    private void branch(int target, boolean take) {
        if (take) pc = target;
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
