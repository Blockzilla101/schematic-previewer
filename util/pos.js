export class Pos {
    /** @type {number} */
    x
    /** @type {number} */
    y

    constructor(x, y) {
        this.x = x
        this.y = y
    }

    pack() {
        let buffer = Buffer.alloc(4)
        buffer.writeInt16BE(this.x, 0)
        buffer.writeInt16BE(this.y, 2)
        return buffer.readInt32BE(0)
    }

    sub(pos) {
        this.x = pos.x
        this.y = pos.y
    }

    static unpack(pos) {
        let buffer = Buffer.alloc(4)
        buffer.writeInt32BE(pos, 0)
        return new this(buffer.readInt16BE(0), buffer.readInt16BE(2))
    }

    toString(xPad = 0, yPad = 0) {
        return `pos(${this.x.toString().padStart(xPad, '0')}, ${this.y.toString().padStart(yPad, '0')})`
    }
}
