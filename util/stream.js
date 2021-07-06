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

const chunkSize = 100000

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

    constructor(buffer = Buffer.alloc(chunkSize)) {
        this.#buffer = buffer
    }

    writeByte(byte) {
        this.#tryResize()
        this.#buffer.writeInt8(byte, this.#offset)
        this.#offset += 1
    }

    writeShort(short) {
        this.#tryResize()
        this.#buffer.writeInt16BE(short, this.#offset)
        this.#offset += 2
    }

    writeInt(int) {
        this.#tryResize()
        this.#buffer.writeInt32BE(int, this.#offset)
        this.#offset += 4
    }

    writeLong(long) {
        this.#tryResize()
        this.#buffer.writeBigInt64BE(long, this.#offset)
        this.#offset += 8
    }

    writeFloat(float) {
        this.#tryResize()
        this.#buffer.writeInt32BE(parseInt(float.toString(2), 2), this.#offset)
        this.#offset += 4
    }

    writeDouble(double) {
        this.#tryResize()
        // todo: figure what a double actually is
        this.#buffer.writeInt32BE(parseInt(double.toString(2), 2), this.#offset)
        this.#offset += 4
    }

    writeBool(bool) {
        this.#tryResize()
        this.#buffer.writeInt8(bool ? 1 : 0, this.#offset)
        this.#offset += 1
    }

    writeString(str) {
        this.writeShort(str.length)
        for (let i = 0; i < str.length; i++) {
            this.writeByte(str.codePointAt(i))
        }
    }

    /**
     * @param {Buffer} buffer
     */
    write(buffer) {
        this.#tryResize()
        if (buffer.length + this.#offset > this.#buffer.length) {
            this.#buffer = expandBuffer(this.#buffer, buffer.length + chunkSize)
        }
        buffer.copy(this.#buffer, this.#offset)
        this.#offset += buffer.length
    }

    #tryResize() {
        if (this.#offset + chunkSize < this.#buffer.length) return
        this.#buffer = expandBuffer(this.#buffer, this.#buffer.length + chunkSize)
    }

    get buffer() {
        return this.#buffer.slice(0, this.#offset)
    }
}
