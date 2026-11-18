package sim6800.cpu;

public class ALU {
    private final Cpu cpu;
    public ALU(Cpu cpu) {
        this.cpu = cpu;
    }

    //loads/stores/transfers
    public int LDA(int address) { //Load accumulator from memory
        int result = cpu.read8(address);

        cpu.setNZ8(result);
        cpu.setFlag(Cpu.V, false);

        return result;
    }
    public void STAA(int address) {//Store from accumulator A to memory
        int value = cpu.getA();
        cpu.write8(address, value);
        cpu.setNZ8(value);
        cpu.setFlag(Cpu.V, false);
    }
    public void STAB(int address) {//Store from accumulator B to memory
        int value = cpu.getB();
        cpu.write8(address, value);
        cpu.setNZ8(value);
        cpu.setFlag(Cpu.V, false);
    }
    public int LDX(int address) {//Load index register
        int result = cpu.read16(address);
        cpu.setNZ16(result);
        cpu.setFlag(Cpu.V, false);
        return result;
    }
    public void STX(int address) {//Store index register
        int value = cpu.getX();

        cpu.write16(address, value);

        cpu.setNZ16(value);
        cpu.setFlag(Cpu.V, false);
    }
    public int LDS(int address) {//Load stack pointer
        int result = cpu.read16(address);
        cpu.setNZ16(result);
        cpu.setFlag(Cpu.V, false);
        return result;
    }
    public void STS(int address) {//Store stack pointer
        int value = cpu.getSp();

        cpu.write16(address, value);

        cpu.setNZ16(value);
        cpu.setFlag(Cpu.V, false);
    }
    public int TAB(int valueA) {//Transfer from accumulator A to B
        cpu.setNZ8(valueA);
        cpu.setFlag(Cpu.V, false);

        return valueA;
    }
    public int TBA(int valueB) {//Transfer from accumulator B to A
        cpu.setNZ8(valueB);
        cpu.setFlag(Cpu.V, false);

        return valueB;
    }
    //arithmetic
    public int ADD(int valueA, int valueMem) {//Add to accumulator
        int result = valueA + valueMem;

        cpu.setFlag(Cpu.H, ((valueA & 0x0F) + (valueMem & 0x0F)) > 0x0F);
        cpu.setFlag(Cpu.C, result > 0xFF);
        cpu.setFlag(Cpu.V, (~(valueA ^ valueMem) & (valueA ^ result) & 0x80) != 0);

        result &= 0xFF;

        cpu.setNZ8(result);

        return result;
    }
    public int ADC(int valueA, int valueMem) {//Add contents of memory and flag to accumulator
        int result = valueA + valueMem + (cpu.flag(Cpu.C) ? 1 : 0);

        cpu.setFlag(Cpu.H, ((valueA & 0x0F) + (valueMem & 0x0F) + (cpu.flag(Cpu.C) ? 1 : 0)) > 0x0F);
        cpu.setFlag(Cpu.V, (~(valueA ^ valueMem) & (valueA ^ result) & 0x80) != 0);
        cpu.setFlag(Cpu.C, result > 0xFF);

        result &= 0xFF;
        cpu.setNZ8(result);

        return result;
    }
    public int ABA(int valueA, int valueB) {//Add B to A
        int result = valueA + valueB;

        //set if there was a carry from bit3
        cpu.setFlag(Cpu.H, ((valueA & 0x0F) + (valueB & 0x0F)) > 0x0F);

        //set if there was a carry from vit7
        cpu.setFlag(Cpu.C, result > 0xFF);

        cpu.setFlag(Cpu.V, ((~(valueA ^ valueB) & (valueA ^ result) & 0x80) != 0));
        result &= 0xFF;

        cpu.setNZ8(result);

        return result;
    }
    public int SUB(int valueA, int valueMem) {//Subtract from accumulator A
        int result = valueA - valueMem;

        //check if accumulator and mem have different signs and if the subtraction result has changed signs which indicates overflow
        boolean overflow = ((valueA ^ valueMem) & (valueA ^ result) & 0x80) != 0;
        result &= 0xFF;

        cpu.setFlag(Cpu.C, valueMem > valueA);
        cpu.setNZ8(result);
        cpu.setFlag(Cpu.V, overflow);

        return result;

    }
    public int SBC(int value, int memory) {//Subtract value in memory and carry flag from accumulator
        int result = value - memory - (cpu.flag(Cpu.C) ? 1 : 0);

        boolean overflow = ((value ^ memory) & (value ^ result) & 0x80) != 0;
        cpu.setFlag(Cpu.C, memory + (cpu.flag(Cpu.C) ? 1 : 0) > value);

        result &= 0xFF;
        cpu.setNZ8(result);
        cpu.setFlag(Cpu.V, overflow);

        return result;
    }
    public int SBA(int valueA, int valueB) {//Subtract B from A
        int result = valueA-valueB;
        result&=0xFF;

        cpu.setFlag(Cpu.C, valueA < valueB);
        cpu.setNZ8(result);
        cpu.setFlag(Cpu.V, ((valueA ^ valueB) & (valueA ^ result) & 0x80) != 0);
        return result;

    }
    public void CMP(int AValue, int memValue) {//Compare contents in accumulator to memory
        int result = AValue - memValue;
        result &= 0xFF;

        cpu.setFlag(Cpu.C, AValue < memValue);
        cpu.setNZ8(result);
        boolean overflow = ((AValue ^ memValue) & (AValue ^ result) & 0x80) != 0;
        cpu.setFlag(Cpu.V, overflow);
    }
    public void CBA(int AValue, int BValue) {//Compare contents of A and B
        int result = AValue - BValue;

        cpu.setFlag(Cpu.C, AValue < BValue);

        result &= 0xFF;

        cpu.setNZ8(result);
        boolean overflow = ((AValue ^ BValue) & (AValue ^ result) & 0x80) != 0;
        cpu.setFlag(Cpu.V, overflow);
    }
    public void CPX(int IRXValue, int memValue) {//Compare contents of memory to Index Register X
        int result = IRXValue - memValue;
        result &= 0xFFFF;

        cpu.setNZ16(result);

        //if x&mem have different signs and the result has a different sign from X, overflow has occurred
        boolean overflow = ((IRXValue ^ memValue) & (IRXValue ^ result) & 0x8000) != 0;
        cpu.setFlag(Cpu.V, overflow);

    }
    public int INC(int value) {//Increment accumulator/memory location/stack pointer/index register X
        value++;
        value &= 0xFF;

        cpu.setNZ8(value);
        cpu.setFlag(Cpu.V, value == 0x80);

        return value;

    }
    public int DEC(int value) {//Decrement accumulator/memory location/stack pointer/index register X
        value--;
        value &= 0xFF;

        cpu.setNZ8(value);
        cpu.setFlag(Cpu.V, value == 0x7F);

        return value;
    }
    public int NEG(int value) {//Negate accumulator/memory location
        value = -value;
        value &= 0xFF;

        cpu.setNZ8(value);
        cpu.setFlag(Cpu.V, value == 0x80);
        cpu.setFlag(Cpu.C, value != 0);

        return value;


    }
    public int DAA(int value) {//Decimal adjust A (turns a hex number into a decimal number) for BCD addition

        int adjust = 0;
        boolean carryIn = cpu.flag(Cpu.C);

        //check if its a valud BCD, if not add 6 to the digit
        if ((value & 0x0F) > 9 || cpu.flag(Cpu.H)) adjust += 0x06;
        if (value > 0x99 || carryIn) adjust += 0x60;

        value += adjust;

        boolean carryOut = value > 0xFF || carryIn;
        //clamp
        value &= 0xFF;

        cpu.setFlag(Cpu.C, carryOut);
        cpu.setNZ8(value);
        //V is "not defined"
        return value;
    }
    public int INX(int value) {//increment index register X
        value ++;

        //clamp to 16 bits
        value &= 0xFFFF;
        cpu.setFlag(Cpu.Z, value == 0);

        return value;

    }
    public int DEX(int value) {//Decrement index register X
        value --;

        //clamp to 16 bits
        value &= 0xFFFF;
        cpu.setFlag(Cpu.Z, value == 0);

        return value;

    }
    //logic/test
    public int AND(int accumulatorValue, int memoryValue) {//Memory  contents AND accumulator
        //result is AND of values
        int result = accumulatorValue & memoryValue;

        cpu.setNZ8(result);
        cpu.setFlag(Cpu.V, false);

        return result;
    }
    public int ORA(int accumulatorValue, int memoryValue) {//Memory/value OR accumulator
        //result is AND of values
        int result = accumulatorValue | memoryValue;

        cpu.setNZ8(result);
        cpu.setFlag(Cpu.V, false);

        return result;
    }
    public int EOR(int accumulatorValue, int memoryValue) {//memory/value XOR accumulator
        //result is XOR of values
        int result = accumulatorValue ^ memoryValue;

        cpu.setNZ8(result);
        cpu.setFlag(Cpu.V, false);

        return result;

    }
    public void BIT(int accumulatorValue, int memoryValue) {//memory contents AND accumulator, only flags are affected

        //result is AND of values
        int result = accumulatorValue & memoryValue;

        cpu.setNZ8(result);
        cpu.setFlag(Cpu.V, false);
    }
    public int COM(int value) {//Complement/NOT of accumulator or memory
        //result is the NOT of value
        int result = ~value & 0xFF;

        cpu.setFlag(Cpu.C, true);
        cpu.setNZ8(result);
        cpu.setFlag(Cpu.V, false);

        return result;

    }
    public int CLR(int value) {//Clear accumulator/memory location
        cpu.setFlag(Cpu.C, false);
        cpu.setNZ8(0);
        cpu.setFlag(Cpu.V, false);
        return 0x00;

    }
    public void TST(int value) {//sets N/Z flags based on value
        cpu.setFlag(Cpu.C, false);
        cpu.setNZ8(value);
        cpu.setFlag(Cpu.V, false);
    }
    //shifts/rotates
    public int ASL(int value) {//Shift left (arithmetic), bit0 is 0 (mult by 2)
        //check bit7 for carry
        boolean carryOut = (value & 0x80) != 0;

        value = value << 1;

        //clamp
        value &= 0xFF;

        cpu.setFlag(Cpu.C, carryOut);
        cpu.setNZ8(value);
        cpu.setFlag(Cpu.V, cpu.flag(Cpu.N) != cpu.flag(Cpu.C));

        return value;

    }
    public int ASR(int value) {//Shift right (arithmetic), bit7 is the same
        // save bit7
        int bit7 = value & 0x80;

        //check if bit0 is set
        boolean carryOut = (value & 0x01) != 0;

        value = value >>> 1;

        //keep bit7
        if (bit7 != 0) value += 0x80;


        //flags
        cpu.setFlag(Cpu.C, carryOut);
        cpu.setNZ8(value);
        cpu.setFlag(Cpu.V, cpu.flag(Cpu.N) != cpu.flag(Cpu.C));

        return value;
    }
    public int LSR(int value) {//shift right (logical), bit7 is 0 (divide by 2)
        // if bit0 is 1 then carry it out
        boolean carryOut = (value & 0x01) != 0;

        // Shift right
        value = value >>> 1;

        //flags
        cpu.setFlag(Cpu.C, carryOut);
        cpu.setNZ8(value);
        cpu.setFlag(Cpu.V, cpu.flag(Cpu.N) != cpu.flag(Cpu.C));

        return value;
    }
    public int ROL(int value) {//rotate left, store bit in carry flag
        //get old carryIn
        int carryIn = 0;
        if (cpu.flag(Cpu.C)) {
            carryIn = 0x01;
        }

        //if there is a bit that can be carried, set carry flag to true
        boolean carryOut = (value & 0x80) != 0;

        // shift right then insert carry bit
        int result = (value << 1) + carryIn;

        //clamp results with bitmask
        result &= 0xFF;

        //flags
        cpu.setFlag(Cpu.C, carryOut);
        cpu.setNZ8(result);
        cpu.setFlag(Cpu.V, cpu.flag(Cpu.N) != cpu.flag(Cpu.C));

        return result;
    }
    public int ROR(int value) {//Rotate right, store bit in carry flag
        //get old carryIn
        int carryIn = 0;
        if (cpu.flag(Cpu.C)) {
            carryIn = 0x80;
        }

        //if there is a bit that can be carried, set carry flag to true
        boolean carryOut = (value & 0x01) == 1;

        // shift right then insert carry bit
        int result = (value >>> 1) + carryIn;


        //flags
        cpu.setFlag(Cpu.C, carryOut);
        cpu.setNZ8(result);
        cpu.setFlag(Cpu.V, cpu.flag(Cpu.N) != cpu.flag(Cpu.C));

        return result;
    }
}
