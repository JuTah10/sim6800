package sim6800.ui;

import sim6800.cpu.Cpu;
import sim6800.memory.Ram;
import sim6800.memory.S19Loader;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        Ram ram = new Ram();
        Cpu cpu = new Cpu(ram);

        // Change this to true when the CPU instructions are ready
        boolean cpuReady = false;

        boolean running = true;

        while (running) {

            System.out.println("\n=== Motorola 6800 Simulator ===");
            System.out.println("1. Load S19 into RAM");
            System.out.println("2. Load S19 into ROM");
            System.out.println("3. Run program");
            System.out.println("4. Step one instruction");
            System.out.println("5. View CPU registers");
            System.out.println("6. View memory");
            System.out.println("7. Clear RAM");
            System.out.println("8. Reset CPU");
            System.out.println("9. Quit");

            System.out.print("Enter choice: ");
            String choice = input.nextLine();

            switch (choice) {

                case "1":
                    loadFile(input, ram, cpu, false);
                    break;

                case "2":
                    loadFile(input, ram, cpu, true);
                    break;

                case "3":
                    if (cpuReady) {
                        cpu.run();
                        System.out.println("CPU paused.");
                    } else {
                        System.out.println("CPU instructions not ready yet.");
                    }
                    break;

                case "4":
                    if (cpuReady) {
                        cpu.step();
                        System.out.println("One instruction executed.");
                    } else {
                        System.out.println("CPU instructions not ready yet.");
                    }
                    break;

                case "5":
                    showRegisters(cpu);
                    break;

                case "6":
                    showMemory(input, ram);
                    break;

                case "7":
                    ram.clear();
                    cpu.reset();
                    System.out.println("RAM cleared.");
                    break;

                case "8":
                    cpu.reset();
                    System.out.println("CPU reset.");
                    break;

                case "9":
                    running = false;
                    System.out.println("Goodbye!");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        }

        input.close();
    }

    // Loads an S19 file into memory
    public static void loadFile(Scanner input, Ram ram,
                                Cpu cpu, boolean asRom) {

        System.out.print("Enter S19 file path: ");
        String fileName = input.nextLine();

        try {

            int count = S19Loader.load(fileName, ram, asRom);

            cpu.reset();

            System.out.println("Loaded " + count + " bytes.");

        } catch (IOException e) {

            System.out.println("Error loading file: "
                    + e.getMessage());
        }
    }

    // Shows the CPU register values
    public static void showRegisters(Cpu cpu) {

        System.out.println("\nCPU Registers:");

        System.out.printf("A   = %02X%n", cpu.getA());
        System.out.printf("B   = %02X%n", cpu.getB());
        System.out.printf("X   = %04X%n", cpu.getX());
        System.out.printf("SP  = %04X%n", cpu.getSp());
        System.out.printf("PC  = %04X%n", cpu.getPc());
        System.out.printf("CCR = %02X%n", cpu.getCcr());

        System.out.println("I flag = " + cpu.flag(Cpu.I));
        System.out.println("Waiting = " + cpu.isWaiting());
    }

    // Shows values stored in memory
    public static void showMemory(Scanner input, Ram ram) {

        try {

            System.out.print("Enter address (hex): ");
            int address = Integer.parseInt(
                    input.nextLine().trim(), 16);

            System.out.print("How many bytes? ");
            int count = Integer.parseInt(
                    input.nextLine().trim());

            if (address < 0 || address > 0xFFFF
                    || count < 1 || count > 256
                    || address + count > 65536) {

                System.out.println("Invalid address or count.");
                return;
            }

            for (int i = 0; i < count; i++) {

                if (i % 8 == 0) {
                    System.out.printf(
                            "%n%04X: ", address + i);
                }

                System.out.printf(
                        "%02X ", ram.read(address + i));
            }

            System.out.println();

        } catch (NumberFormatException e) {

            System.out.println("Invalid number.");
        }
    }
}