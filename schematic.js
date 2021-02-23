const zip = require('zlib')

class Stream {
    /** @type {Number} */
    #offset = 0;
    /** @type {Buffer} */
    #buffer;

    constructor(buffer, offset = 0) {
        this.#buffer = buffer;
        this.#offset = offset;
    }

    skip(num) {
        this.#offset += num;
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
        return parseFloat(this.readInt().toString(2));
    }

    readDouble() {
        return parseFloat(this.readFloat().toString(2));
    }

    readBool() {
        return this.readByte() === 0
    }

    readUTF() {
        const len = this.readShort();
        let str = this.#buffer.toString('utf-8', this.#offset, this.#offset + len)
        this.#offset += str.length;
        return str;
    }

    remaining() {
        return this.#buffer.slice(this.#offset)
    }
}

function unpackPos(pos) {
    return { x: pos >>> 16, y: pos & 0xFFF }
}

class Schematic {
    /** @type {number} */
    #width;
    /** @type {number} */
    #height;

    /** @type {string} */
    #name;
    /** @type {string} */
    #description;

    /** @type {string[]} */
    #blocks = [];
    /** @type {{blockIndex: number, position: {x: number, y: number}, rotation: number, config: any}[]} */
    #tiles = [];

    constructor(width, height, name, description, tiles, blocks) {
        this.#width = width;
        this.#height = height;
        this.#name = name;
        this.#description = description;
        this.#tiles = tiles;
        this.#blocks = blocks;
    }

    static read(buffer) {
        let stream = new Stream(buffer);
        for (let i = 0; i < Schematic.Header.length; i++) {
            if (stream.readByte() !== Schematic.Header.codePointAt(i)) throw new Error("Invalid Header");
        }
        const version = stream.readByte();

        stream = new Stream(zip.inflateSync(stream.remaining()))

        const width = stream.readShort();
        const height = stream.readShort();

        let numTags = stream.readByte();
        let tags = new Map();
        for (let i = 0; i < numTags; i++) {
            tags.set(stream.readUTF(), stream.readUTF())
        }

        let numBlocks = stream.readByte();
        let blocks = []
        for (let i = 0; i < numBlocks; i++) {
            blocks.push(stream.readUTF())
        }

        let numTiles = stream.readInt();
        let tiles = [];
        for (let i = 0; i < numTiles; i++) {
            try {
                let blockIndex = stream.readByte();
                let position = stream.readInt();

                let config = version === 0 ? this.#readConfig0(blocks[blockIndex], stream.readInt(), position) : this.#readConfig1(stream)

                let rotation = stream.readByte();
                tiles.push({ blockIndex, position: unpackPos(position), rotation, config })
            } catch (e) {
                console.log('error while reading')
            }
        }

        return new Schematic(width, height, tags.get('name'), tags.get('description'), tiles, blocks);
    }

    /**
     * @param {Stream} stream
     */
    static #readConfig1(stream) {
        let type = stream.readByte();
        switch(type){
            case 0: {
                return null
            }
            case 1: {
                return stream.readInt()
            }
            case 2: {
                return stream.readLong()
            }
            case 3: {
                return stream.readFloat()
            }
            case 4: {
                return stream.readUTF()
            }
            case 5: {
                stream.readByte();
                stream.readShort();
                return;
            }
            case 6: {
                let length = stream.readShort();
                let arr = []
                for (let i = 0; i < length; i++) arr.push(stream.readInt());
                return arr;
            }
            case 7: {
                return { x: stream.readInt(), y: stream.readInt() }
            }
            case 8: {
                let len = stream.readByte();
                let out = [];
                for (let i = 0; i < len; i++) out[i] = unpackPos(stream.readInt());
                return out;
            }
            case 9: {
                stream.readByte();
                stream.readShort();
                return;
            }
            case 10: {
                return stream.readBool();
            }
            case 11: {
                return stream.readDouble();
            }
            case 12: {
                return unpackPos(stream.readInt())
            }
            case 13: {
                return stream.readShort();
            }
            case 14: {
                let len = stream.readInt();
                let out = [];
                for (let i = 0; i < len; i++) out[i] = stream.readByte();
                return out;
            }
            case 15: {
                return stream.readByte();
            }
        }
    }

    /**
     * @param {string} block
     * @param {number} config
     * @param {number} position
     */
    static #readConfig0(block, config, position) {
        if (['sorter', 'inverted-sorter', 'item-source', 'unloader'].includes(block)) return 'item';
        if (['liquid-source'].includes(block)) return 'liquid';
        if (['mass-driver', 'phase-conveyor', 'bridge-conveyor'].includes(block)) return 'other bridge pos';
        if (['illuminator'].includes(block)) return 'color';
        return null;
    }

    static get Header() {
        return 'msch'
    }

    get width() {
        return this.#width;
    }

    get height() {
        return this.#height;
    }

    get name() {
        return this.#name;
    }

    get description() {
        return this.#description;
    }

    toString() {
        let blocks = this.#blocks;
        let largestX = 0;
        let largestY = 0;
        let largestBlockName = 0;
        let str = []

        this.#tiles.forEach(t => {
            if (blocks[t.blockIndex].length > largestBlockName) largestBlockName = blocks[t.blockIndex].length
            if (t.position.x.toString().length > largestX) largestX = t.position.x.toString().length
            if (t.position.y.toString().length > largestY) largestY = t.position.y.toString().length
        })

        this.#tiles.forEach(t => {
            str.push(`${blocks[t.blockIndex].padEnd(largestBlockName)} | pos (${(t.position.x).toString().padStart(largestX, '0')}, ${(t.position.y).toString().padStart(largestY, '0')}) | rot ${t.rotation} | config ${JSON.stringify(t.config)}`)
        })

        return `width ${this.width} | height ${this.height}\nname ${this.name} | description ${this.description}\nblocks ${JSON.stringify(this.#blocks)}\n${str.join('\n')}`
    }
}

module.exports = {
    Schematic, Stream
}
