package sim6800.cpu;

import static sim6800.cpu.Mode.*;

// All 197 valid 6800 opcodes, from the datasheet instruction tables.
// Index = opcode byte. Null = unassigned opcode.
public final class OpcodeTable {
    private static final Opcode[] TABLE = new Opcode[256];

    public static Opcode get(int opcode) {
        return TABLE[opcode & 0xFF];
    }

    private static void op(int code, String mnemonic, Mode mode, int bytes) {
        TABLE[code] = new Opcode(mnemonic, mode, bytes);
    }

    // inherent, accumulator and stack operations (0x00-0x5F)
    static {
        op(0x01, "NOP", INH, 1);
        op(0x06, "TAP", INH, 1);
        op(0x07, "TPA", INH, 1);
        op(0x08, "INX", INH, 1);
        op(0x09, "DEX", INH, 1);
        op(0x0A, "CLV", INH, 1);
        op(0x0B, "SEV", INH, 1);
        op(0x0C, "CLC", INH, 1);
        op(0x0D, "SEC", INH, 1);
        op(0x0E, "CLI", INH, 1);
        op(0x0F, "SEI", INH, 1);
        op(0x10, "SBA", INH, 1);
        op(0x11, "CBA", INH, 1);
        op(0x16, "TAB", INH, 1);
        op(0x17, "TBA", INH, 1);
        op(0x19, "DAA", INH, 1);
        op(0x1B, "ABA", INH, 1);

        op(0x20, "BRA", REL, 2);
        op(0x22, "BHI", REL, 2);
        op(0x23, "BLS", REL, 2);
        op(0x24, "BCC", REL, 2);
        op(0x25, "BCS", REL, 2);
        op(0x26, "BNE", REL, 2);
        op(0x27, "BEQ", REL, 2);
        op(0x28, "BVC", REL, 2);
        op(0x29, "BVS", REL, 2);
        op(0x2A, "BPL", REL, 2);
        op(0x2B, "BMI", REL, 2);
        op(0x2C, "BGE", REL, 2);
        op(0x2D, "BLT", REL, 2);
        op(0x2E, "BGT", REL, 2);
        op(0x2F, "BLE", REL, 2);

        op(0x30, "TSX", INH, 1);
        op(0x31, "INS", INH, 1);
        op(0x32, "PULA", INH, 1);
        op(0x33, "PULB", INH, 1);
        op(0x34, "DES", INH, 1);
        op(0x35, "TXS", INH, 1);
        op(0x36, "PSHA", INH, 1);
        op(0x37, "PSHB", INH, 1);
        op(0x39, "RTS", INH, 1);
        op(0x3B, "RTI", INH, 1);
        op(0x3E, "WAI", INH, 1);
        op(0x3F, "SWI", INH, 1);

        // accumulator A forms 0x4x, accumulator B forms 0x5x
        String[] acc = {"NEG", null, null, "COM", "LSR", null, "ROR", "ASR",
                        "ASL", "ROL", "DEC", null, "INC", "TST", null, "CLR"};
        for (int i = 0; i < 16; i++) {
            if (acc[i] == null) continue;
            op(0x40 + i, acc[i] + "A", INH, 1);
            op(0x50 + i, acc[i] + "B", INH, 1);
        }
    }

    // memory read-modify-write and JMP (0x60-0x7F)
    static {
        String[] rmw = {"NEG", null, null, "COM", "LSR", null, "ROR", "ASR",
                        "ASL", "ROL", "DEC", null, "INC", "TST", "JMP", "CLR"};
        for (int i = 0; i < 16; i++) {
            if (rmw[i] == null) continue;
            op(0x60 + i, rmw[i], IDX, 2);
            op(0x70 + i, rmw[i], EXT, 3);
        }
    }

    // accumulator/memory operations (0x80-0xFF)
    // Low nibble selects the operation, high nibble selects accumulator and mode:
    //   8x A imm   9x A dir   Ax A idx   Bx A ext
    //   Cx B imm   Dx B dir   Ex B idx   Fx B ext
    static {
        String[] alu = {"SUB", "CMP", "SBC", null, "AND", "BIT", "LDA", "STA",
                        "EOR", "ADC", "ORA", "ADD", null, null, null, null};
        for (int i = 0; i < 12; i++) {
            if (alu[i] == null) continue;
            boolean store = alu[i].equals("STA");
            for (int accIdx = 0; accIdx < 2; accIdx++) {
                String name = alu[i] + (accIdx == 0 ? "A" : "B");
                int base = accIdx == 0 ? 0x80 : 0xC0;
                if (!store) op(base + 0x00 + i, name, IMM, 2);   // no STA immediate
                op(base + 0x10 + i, name, DIR, 2);
                op(base + 0x20 + i, name, IDX, 2);
                op(base + 0x30 + i, name, EXT, 3);
            }
        }
        // 16-bit and subroutine columns (0xC-0xF) differ between the A and B halves
        op(0x8C, "CPX", IMM, 3); op(0x9C, "CPX", DIR, 2); op(0xAC, "CPX", IDX, 2); op(0xBC, "CPX", EXT, 3);
        op(0x8D, "BSR", REL, 2); op(0xAD, "JSR", IDX, 2); op(0xBD, "JSR", EXT, 3);
        op(0x8E, "LDS", IMM, 3); op(0x9E, "LDS", DIR, 2); op(0xAE, "LDS", IDX, 2); op(0xBE, "LDS", EXT, 3);
        op(0x9F, "STS", DIR, 2); op(0xAF, "STS", IDX, 2); op(0xBF, "STS", EXT, 3);
        op(0xCE, "LDX", IMM, 3); op(0xDE, "LDX", DIR, 2); op(0xEE, "LDX", IDX, 2); op(0xFE, "LDX", EXT, 3);
        op(0xDF, "STX", DIR, 2); op(0xEF, "STX", IDX, 2); op(0xFF, "STX", EXT, 3);
    }

    private OpcodeTable() { }
}
