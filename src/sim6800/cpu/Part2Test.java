package sim6800.cpu;

import sim6800.memory.Ram;

// hand-assembled programs using only Part 2 instructions
public class Part2Test {
    static int fails = 0;

    static void check(String what, int got, int want) {
        boolean ok = got == want;
        if (!ok) fails++;
        System.out.printf("%-34s got %04X want %04X %s%n", what, got, want, ok ? "ok" : "FAIL");
    }
    static void check(String what, boolean got, boolean want) {
        boolean ok = got == want;
        if (!ok) fails++;
        System.out.printf("%-34s got %-5s want %-5s %s%n", what, got, want, ok ? "ok" : "FAIL");
    }

    // FC00-FFFF is ROM in Ram, so bytes up there go in the way the loader does it
    static void load(Ram ram, int addr, int... bytes) {
        for (int b : bytes) {
            ram.loadByte(addr, b, addr >= 0xFC00);
            addr++;
        }
    }

    public static void main(String[] args) {
        // program 1: SEC, BCS taken, BSR/RTS, SEI/CLI, SWI, RTI, WAI
        Ram ram = new Ram();
        load(ram, 0xFFFE, 0x00, 0x00);          // reset -> 0000
        load(ram, 0xFFFA, 0x00, 0x50);          // SWI   -> 0050
        load(ram, 0x0000,
            0x0D,             // 0000 SEC
            0x25, 0x02,       // 0001 BCS +2 -> 0005
            0x3F,             // 0003 SWI (must be skipped)
            0x01,             // 0004 NOP
            0x8D, 0x03,       // 0005 BSR +3 -> 000A, return 0007
            0x0E,             // 0007 CLI
            0x3F,             // 0008 SWI -> 0050, return 0009
            0x3E,             // 0009 WAI
            0x0F,             // 000A SEI
            0x39);            // 000B RTS
        load(ram, 0x0050, 0x3B);                // SWI handler: RTI

        Cpu cpu = new Cpu(ram);
        cpu.sp = 0x00FF;
        for (int i = 0; i < 7; i++) cpu.step();  // SEC BCS BSR SEI RTS CLI SWI
        System.out.println("-- program 1 after SWI");
        check("pc = SWI vector target", cpu.pc, 0x0050);
        check("I set on entry", cpu.flag(Cpu.I), true);
        check("sp after 7-byte push", cpu.sp, 0x00F8);
        check("stack: PC low at 00FF", ram.read(0x00FF), 0x09);
        check("stack: PC high at 00FE", ram.read(0x00FE), 0x00);
        check("stack: CCR at 00F9 (C0|C)", ram.read(0x00F9), 0xC1);

        cpu.step();                              // RTI
        System.out.println("-- after RTI");
        check("pc back to 0009", cpu.pc, 0x0009);
        check("sp restored", cpu.sp, 0x00FF);
        check("I restored to 0", cpu.flag(Cpu.I), false);
        check("ccr restored", cpu.ccr, 0xC1);

        cpu.step();                              // WAI
        System.out.println("-- after WAI");
        check("pc after WAI", cpu.pc, 0x000A);
        check("waiting", cpu.isWaiting, true);
        check("sp after 7-byte push", cpu.sp, 0x00F8);
        check("stack: PC low at 00FF", ram.read(0x00FF), 0x0A);

        cpu.run();                               // must do nothing now
        check("run() stops at WAI", cpu.pc, 0x000A);

        // program 2: JMP, JSR ext, PSHA/PULA, TSX, TAP/TPA, BCS not taken
        ram = new Ram();
        load(ram, 0xFFFE, 0x00, 0x10);
        load(ram, 0x0010, 0x7E, 0x00, 0x20);    // JMP $0020
        load(ram, 0x0020, 0xBD, 0x00, 0x30,     // JSR $0030 (return 0023)
                          0x3E);                // WAI
        load(ram, 0x0030,
            0x36,             // PSHA
            0x32,             // PULA
            0x30,             // TSX
            0x06,             // TAP   ccr = a | C0
            0x07,             // TPA   a = ccr
            0x25, 0xFE,       // BCS -2 (C=0, not taken)
            0x39);            // RTS
        cpu = new Cpu(ram);
        cpu.sp = 0x00FF;
        cpu.a = 0x5A;
        cpu.run();
        System.out.println("-- program 2 after run");
        check("pc after WAI", cpu.pc, 0x0024);
        check("x = sp+1 from TSX", cpu.x, 0x00FE);
        check("ccr from TAP (5A|C0)", cpu.ccr, 0xDA);
        check("a from TPA", cpu.a, 0xDA);
        check("sp after WAI", cpu.sp, 0x00F8);   // JSR +2, PSHA +1, PULA -1, RTS -2, WAI +7

        // program 3: backward branch taken
        ram = new Ram();
        load(ram, 0xFFFE, 0x00, 0x00);
        load(ram, 0x0000,
            0x20, 0x02,       // 0000 BRA +2 -> 0004
            0x3E,             // 0002 WAI
            0x01,             // 0003 NOP
            0x20, 0xFC);      // 0004 BRA -4 -> 0002
        cpu = new Cpu(ram);
        cpu.sp = 0x00FF;
        cpu.run();
        System.out.println("-- program 3 after run");
        check("backward branch landed on WAI", cpu.pc, 0x0003);

        System.out.println(fails == 0 ? "ALL OK" : fails + " FAILURES");
    }
}
