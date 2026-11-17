package sim6800.ui;

import sim6800.cpu.Cpu;
import sim6800.memory.Ram;

// Test driver. Not part of the processor; it only uses one through its
// public methods. For now just a quick check that reset works.
// Real command loop comes later (load, step, run, regs, mem, clear, quit).
public class Main {
    public static void main(String[] args) {
        Ram ram = new Ram();

        // put a reset vector at FFFE/FFFF pointing to 0x0010
        ram.write(0xFFFE, 0x00);
        ram.write(0xFFFF, 0x10);

        Cpu cpu = new Cpu(ram);

        // should print pc = 0010, ccr = D0, I = true
        System.out.printf("pc  = %04X%n", cpu.getPc());
        System.out.printf("ccr = %02X%n", cpu.getCcr());
        System.out.println("I   = " + cpu.flag(Cpu.I));
    }
}
