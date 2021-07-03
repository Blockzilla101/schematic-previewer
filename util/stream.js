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

export class OutputStream {
    /** @type {Number} */
    #offset = 0
    /** @type {Buffer} */
    #buffer

    constructor(buffer = new Buffer(0)) {
        this.#buffer = buffer
    }

    writeByte(byte) {
        this.#buffer.writeInt8(byte)
    }

    writeShort(short) {
        this.#buffer.writeInt16BE(short)
    }

    readInt(int) {
        this.#buffer.writeInt32BE(int)
    }

    writeLong(long) {
        this.#buffer.writeBigInt64BE(long)
    }

    writeFloat(float) {
        this.#buffer.writeInt32BE(parseInt(float.toString(2), 2))
    }

    readDouble(double) {
        // todo: figure what a double actually is
        this.#buffer.writeInt32BE(parseInt(double.toString(2), 2))
    }

    writeBool(bool) {
        this.#buffer.writeInt8(bool ? 1 : 0)
    }

    writeString(str) {
        this.#buffer.write(str, 'utf-8')
    }
}
