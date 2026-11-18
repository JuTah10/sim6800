package sim6800.cpu;

import sim6800.memory.Memory;

// the 6800 itself: registers, condition codes, fetch/decode/execute.
// talks to the outside only through Memory
// everything is int and masked: 8-bit regs & 0xFF, 16-bit regs & 0xFFFF
public class Cpu {
    // accumulators (8-bit)
    int a, b;
    // index register, stack pointer, program counter (16-bit)
    int x, sp, pc;
    // condition code register (8-bit), the flags live in here as bits
    int ccr;
    // set by WAI, run() stops when this is true
    boolean isWaiting;
    // the bus, every read and write goes through this
    private final Memory memory;
    // arithmetic / logic operations and their condition codes
    final ALU alu = new ALU(this);

    // where the CPU looks for the start address of reset and SWI (our Phase 1 memory map)
    public static final int VEC_SWI   = 0xFFFA;
    public static final int VEC_RESET = 0xFFFE;

    // CCR bit masks, bit 7 6 5 4 3 2 1 0 = 1 1 H I N Z V C
    public static final int C = 0x01;
    public static final int V = 0x02;
    public static final int Z = 0x04;
    public static final int N = 0x08;
    public static final int I = 0x10;
    public static final int H = 0x20;
    // bits 6 and 7 are always 1
    public static final int CCR_FIXED = 0xC0;

    public Cpu(Memory memory) {
        // remember the bus, then come up the same way the real chip does
        this.memory = memory;
        reset();
    }

    // put the CPU in the exact state the datasheet describes after the RESET pin is pulled
    public void reset() {
        // set back to 0 after reset
        a = 0; b = 0;
        x = 0; sp = 0;

        // bits 6 and 7 always 1, I set so IRQ is masked until the program runs CLI
        ccr = CCR_FIXED | I;
        // ends WAI
        isWaiting = false;
        // start address comes from the vector at FFFE/FFFF
        pc = read16(VEC_RESET);
    }

    // ---- flags: the only place CCR bits are read or changed ----

    // is this one flag on
    public boolean flag(int mask) {
        // keep only the masked bit, nonzero means it was set
        return (ccr & mask) != 0;
    }

    // turn one flag on or off without touching the other bits
    public void setFlag(int mask, boolean on) {
        // OR sets the bit, AND with the inverted mask clears it
        ccr = on ? ccr | mask : ccr & ~mask;
    }

    // set N and Z from an 8-bit result, almost every instruction does this
    public void setNZ8(int value) {
        // N follows bit 7 (the sign bit)
        setFlag(N, (value & 0x80) != 0);
        // mask to 8 bits first so 0x100 counts as zero
        setFlag(Z, (value & 0xFF) == 0);
    }

    // same for a 16-bit result (X, SP)
    public void setNZ16(int value) {
        setFlag(N, (value & 0x8000) != 0);
        setFlag(Z, (value & 0xFFFF) == 0);
    }

    // ---- bus: big-endian, high byte at the lower address ----

    // read one byte, address wrapped to 16 bits
    public int read8(int addr) {
        return memory.read(addr & 0xFFFF);
    }

    // read two bytes, first one is the high byte
    public int read16(int addr) {
        return (read8(addr) << 8) | read8(addr + 1);
    }

    // write one byte, value cut down to 8 bits
    public void write8(int addr, int v) {
        memory.write(addr & 0xFFFF, v & 0xFF);
    }

    // write two bytes, high byte goes first
    public void write16(int addr, int v) {
        write8(addr, v >> 8);
        write8(addr + 1, v);
    }

    // read the byte at PC and move PC forward, used for opcodes and operands
    public int fetch8() {
        int v = read8(pc);
        pc = (pc + 1) & 0xFFFF;
        return v;
    }

    // same for a 2-byte operand (addresses, 16-bit immediates)
    public int fetch16() {
        int high = fetch8();
        int low = fetch8();
        return (high << 8) | low;
    }

    // ---- stack: SP points at the next free byte, grows downward ----

    // store at SP, then move SP down
    public void push8(int v) {
        write8(sp, v);
        sp = (sp - 1) & 0xFFFF;
    }

    // move SP up, then read what is there
    public int pull8() {
        sp = (sp + 1) & 0xFFFF;
        return read8(sp);
    }

    // low byte first so the high byte ends up at the lower address
    public void push16(int v) {
        push8(v);
        push8(v >> 8);
    }

    // reverse of push16
    public int pull16() {
        int high = pull8();
        int low = pull8();
        return (high << 8) | low;
    }

    // addressing modes

    // work out where the operand is, called right after the opcode is fetched
    // so PC is sitting on the first operand byte
    int operandAddress(Opcode op) {
        switch (op.mode()) {
            case IMM: {
                // operand is the next byte(s) itself, so its address is PC
                int addr = pc;
                // skip past it (1 byte normally, 2 for CPX/LDX/LDS)
                pc = (pc + op.bytes() - 1) & 0xFFFF;
                return addr;
            }
            case DIR:
                // one byte address, page zero 0000-00FF
                return fetch8();
            case EXT:
                // full two byte address
                return fetch16();
            case IDX:
                // X plus an unsigned one byte offset
                return (x + fetch8()) & 0xFFFF;
            case REL: {
                // one byte signed offset, the (byte) cast makes it signed
                int offset = (byte) fetch8();
                // added to PC which is already past the instruction
                return (pc + offset) & 0xFFFF;
            }
            default:
                // inherent, no operand
                return -1;
        }
    }

    // execution

    // run one instruction
    public void step() {
        // after WAI do nothing, the test programs all end this way
        if (isWaiting) return;

        // fetch
        int opcodeByte = fetch8();
        // decode
        Opcode op = OpcodeTable.get(opcodeByte);
        if (op == null) {
            throw new IllegalStateException(
                String.format("illegal opcode %02X at %04X", opcodeByte, (pc - 1) & 0xFFFF));
        }
        // execute
        int ea = operandAddress(op);
        execute(op.mnemonic(), ea);
    }

    // keep stepping until the program hits WAI
    public void run() {
        while (!isWaiting) {
            step();
        }
    }

    // interrupt stack frame, used by SWI, WAI and RTI (Phase 1 Table 3.3)

    // push the 7 bytes in datasheet order: PC, X, A, B, CCR
    void pushAll() {
        push16(pc);
        push16(x);
        push8(a);
        push8(b);
        push8(ccr);
    }

    // reverse of pushAll, used by RTI
    void pullAll() {
        // OR in the fixed bits in case the stacked value was changed
        ccr = pull8() | CCR_FIXED;
        b = pull8();
        a = pull8();
        x = pull16();
        pc = pull16();
    }

    // ---- instructions: control flow, stack, flags, interrupts ----

    // anything not handled here is a data/arithmetic/logic instruction and goes to DataInstructions
    void execute(String m, int ea) {
        switch (m) {
            // branches, conditions from the datasheet branch table
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

            // jumps and subroutines, the return address is PC after the instruction
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
            // datasheet: TSX gives X = SP + 1, TXS gives SP = X - 1
            case "TSX": x = (sp + 1) & 0xFFFF; break;
            case "TXS": sp = (x - 1) & 0xFFFF; break;

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
            // SWI saves everything, masks IRQ and jumps through its vector
            case "SWI": pushAll(); setFlag(I, true); pc = read16(VEC_SWI); break;
            // WAI saves everything then stops, a real chip would wait for an interrupt here
            case "WAI": pushAll(); isWaiting = true; break;
            case "RTI": pullAll(); break;

            default: DataInstructions.execute(this, m, ea);
        }
    }

    // only change PC if the condition was true
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
    public boolean isWaiting() { return isWaiting; }
}
