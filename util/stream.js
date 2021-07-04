export class InputStream {
    /** @type {Number} */
    #offset = 0
    /** @type {Buffer} */
    #buffer

    constructor(buffer, offset = 0) {
        this.#buffer = buffer
        this.#offset = offset
    }

    skip(num) {
        this.#offset += num
    }

    reset() {
        this.#offset = 0
    }

    readByte() {
        return this.#buffer.readInt8((this.#offset += 1) - 1)
    }

    readShort() {
        return this.#buffer.readInt16BE((this.#offset += 2) - 2)
    }

    readInt() {
        return this.#buffer.readInt32BE((this.#offset += 4) - 4)
    }

    readLong() {
        return this.#buffer.readBigInt64BE((this.#offset += 8) - 8)
    }

    readFloat() {
        return parseFloat(this.readInt().toString(2))
    }

    readDouble() {
        return parseFloat(this.readFloat().toString(2))
    }

    readBool() {
        return this.readByte() === 0
    }

    readStr() {
        const len = this.readShort()
        let str = this.#buffer.toString('utf-8', this.#offset, this.#offset + len)
        this.#offset += str.length
        return str
    }

    readArr(len, readFunc) {
        let arr = []
        for (let i = 0; i < len; i++) {
            arr.push(readFunc())
        }
        return arr
    }

    remaining() {
        return this.#buffer.slice(this.#offset)
    }
}

/**
 * @param {Buffer} buffer
 * @param {number} newSize
 * @return {Buffer}
 */
function expandBuffer(buffer, newSize) {
    let newBuffer = Buffer.alloc(newSize)
    buffer.copy(newBuffer)
    return newBuffer
}


export class OutputStream {
    /** @type {Number} */
    #offset = 0
    /** @type {Buffer} */
    #buffer

    constructor(buffer = Buffer.alloc(0)) {
        this.#buffer = buffer
    }

    writeByte(byte) {
        this.#buffer = expandBuffer(this.#buffer, this.#buffer.length + 1)
        this.#buffer.writeInt8(byte, this.#buffer.length - 1)
    }

    writeShort(short) {
        this.#buffer = expandBuffer(this.#buffer, this.#buffer.length + 2)
        this.#buffer.writeInt16BE(short, this.#buffer.length - 2)
    }

    writeInt(int) {
        this.#buffer = expandBuffer(this.#buffer, this.#buffer.length + 4)
        this.#buffer.writeInt32BE(int, this.#buffer.length - 4)
    }

    writeLong(long) {
        this.#buffer = expandBuffer(this.#buffer, this.#buffer.length + 8)
        this.#buffer.writeBigInt64BE(long, this.#buffer.length - 8)
    }

    writeFloat(float) {
        this.#buffer = expandBuffer(this.#buffer, this.#buffer.length + 4)
        this.#buffer.writeInt32BE(parseInt(float.toString(2), 2), this.#buffer.length - 4)
    }

    writeDouble(double) {
        // todo: figure what a double actually is
        this.#buffer = expandBuffer(this.#buffer, this.#buffer.length + 4)
        this.#buffer.writeInt32BE(parseInt(double.toString(2), 2), this.#buffer.length - 4)
    }

    writeBool(bool) {
        this.#buffer = expandBuffer(this.#buffer, this.#buffer.length + 1)
        this.#buffer.writeInt8(bool ? 1 : 0, this.#buffer.length - 1)
    }

    writeString(str) {
        this.writeShort(str.length)
        for (let i = 0; i < str.length; i++) {
            this.writeByte(str.codePointAt(i))
        }
    }

    write(buffer) {
        let oldLen = this.#buffer.length
        this.#buffer = expandBuffer(this.#buffer, this.#buffer.length + buffer.length)
        buffer.copy(this.#buffer, oldLen)
    }

    get buffer() {
        return this.#buffer
    }
}
