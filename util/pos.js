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
        return null
    }

    sub(pos) {
        this.x = pos.x
        this.y = pos.y
    }

    static unpack(pos) {
        return new this(pos >>> 16, pos & 0xffff)
    }

    toString(xPad = 0, yPad = 0) {
        return `pos(${this.x.toString().padStart(xPad, '0')}, ${this.y.toString().padStart(yPad, '0')})`
    }
}
