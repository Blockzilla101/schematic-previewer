import zip from 'zlib'
import { InputStream, OutputStream } from './util/stream.js'
import { Tile } from './util/tile.js'
import { Pos } from './util/pos.js'

class Config {
    /**
     * @typedef {"int"|"long"|"float"|"string"|"map"|"int[]"|"pos"|"pos[]"|"node"|"boolean"|"double"|"building"|"lAccess"|"byte[]"|"unitCommand"} ConfigType
     */
    /**
     * @type ConfigType
     */
    type


    /**
     * @type {number|number[]|Pos|Pos[]|string}
     */
    value

    /**
     * @param {ConfigType} type
     * @param value
     */
    constructor(type, value) {
        this.type = type
        this.value = value
    }

    toString() {
        return `{${this.type}} || ${this.value}`
    }
}

export class Schematic {
    /** @type {number} */
    width
    /** @type {number} */
    height

    _tags = new Map()

    /** @type {Tile[]} */
    tiles = []

    toString() {
        let str = []
        str.push(`name: ${this.name} | description: ${this.description}`)
        str.push(`width: ${this.width} | height: ${this.height}`)
        str.push(`tiles: ${this.tiles.length}`)

        let xLen = 0
        let yLen = 0
        let blockLen = 0

        this.tiles.forEach(tile => {
            if (tile.block.length > blockLen) blockLen = tile.block.length
            if (tile.pos.x.toString().length > xLen) xLen = tile.pos.x.toString().length
            if (tile.pos.y.toString().length > yLen) yLen = tile.pos.y.toString().length
        })

        this.tiles.forEach(t => {
            str.push(`${t.block.padEnd(blockLen)} | pos(${t.pos.x.toString().padStart(xLen, '0')}, ${t.pos.y.toString().padStart(yLen, '0')}) | rot ${t.rot} | config ${t.config}`)
        })

        return str.join('\n')
    }

    /**
     * @param {Buffer} buffer
     * @constructs
     */
    static read(buffer) {
        let stream = new InputStream(buffer)
        for (let i = 0; i < this.header.length; i++) {
            if (stream.readByte() !== this.header.codePointAt(i)) throw new Error('Invalid Header')
        }

        const ver = stream.readByte()

        stream = new InputStream(zip.inflateSync(stream.remaining()))
        const schematic = new this()

        schematic.width = stream.readShort()
        schematic.height = stream.readShort()

        let len = stream.readByte()
        for (let i = 0; i < len; i++) {
            schematic._tags.set(stream.readStr(), stream.readStr())
        }

        let blocks = []
        len = stream.readByte()
        for (let i = 0; i < len; i++) {
            blocks.push(stream.readStr())
        }

        len = stream.readInt()
        for (let i = 0; i < len; i++) {
            let block = blocks[stream.readByte()]
            let pos = new Pos(stream.readShort(), stream.readShort())
            let config = ver === 0 ? readConfig0(block, stream.readInt(), pos) : readConfig1(stream)
            let rot = stream.readByte()
            if (block !== 'air') schematic.tiles.push(new Tile(block, pos, rot, config))
        }

        return schematic
    }

    /**
     * @return {Buffer}
     */
    write() {
        let schem = new OutputStream()

        schem.writeShort(this.width)
        schem.writeShort(this.height)

        schem.writeByte(this._tags.size)
        for (const [key, val] of this._tags) {
            schem.writeString(key)
            schem.writeString(val)
        }

        let blocks = []
        this.tiles.forEach(tile => blocks.includes(tile.block) ? null : blocks.push(tile.block))
        schem.writeByte(blocks.length)
        for (const block of blocks) {
            schem.writeString(block)
        }

        schem.writeInt(this.tiles.length)
        for (const tile of this.tiles) {
            schem.writeByte(blocks.findIndex(b => b === tile.block))
            schem.writeInt(tile.pos.pack())
            writeConfig(schem, tile.config)
            schem.writeByte(tile.rot)
        }

        let out = new OutputStream()
        for (let i = 0; i < this.constructor.header.length; i++) {
            out.writeByte(this.constructor.header.codePointAt(i))
        }
        out.writeByte(1)
        out.write(zip.deflateSync(schem.buffer))

        return out.buffer
    }

    get name() {
        return this._tags.get('name') ?? 'unknown'
    }

    get description() {
        return this._tags.get('description') ?? null
    }

    set name(val) {
        this._tags.set('name', val)
    }

    set description(val) {
        this._tags.set('description', val)
    }

    static get header() {
        return 'msch'
    }
}

function readConfig0(block, value, pos) {
    if (['sorter', 'inverted-sorter', 'item-source'].includes(block)) return '[item]'
    if (['liquid-source'].includes(block)) return '[liquid]'
    if (['mass-driver', 'item-bridge'].includes(block)) return Pos.unpack(value).sub(pos)
    if (['illuminator'].includes(block)) return value
    return null
}

/**
 * @param {InputStream} stream
 */
function readConfig1(stream) {
    const type = stream.readByte()
    switch (type) {
        case 0: return null
        case 1: return new Config('int', stream.readInt())
        case 2: return new Config('long', stream.readLong())
        case 3: return new Config('float', stream.readInt())
        case 4: return new Config('string', stream.readStr())
        case 5: {
            stream.readByte()
            stream.readShort()
            console.warn('[ type 5 is not done ]')
            return new Config('map', null)
        }
        case 6: return new Config('int[]', stream.readArr(stream.readShort(), () => stream.readInt()))
        case 7: return new Config('pos', new Pos(stream.readInt(), stream.readInt()))
        case 8: return new Config('pos[]', stream.readArr(stream.readByte(), () => Pos.unpack(stream.readInt())))
        case 9: {
            stream.readByte()
            stream.readShort()
            console.warn('[ type 9 is not done ]')
            return new Config('node', null)
        }
        case 10: return new Config('boolean', stream.readBool())
        case 11: return new Config('double', stream.readDouble())
        case 12: return new Config('building', Pos.unpack(stream.readInt()))
        case 13: {
            stream.readShort()
            console.warn('[ type 13 is not done ]')
            return new Config('lAccess', null)
        }
        case 14: return new Config('byte[]', stream.readArr(stream.readShort(), () => stream.readByte()))
        case 15: {
            stream.readByte()
            console.warn('[ type 15 is not done ]')
            return new Config('unitCommand', null)
        }
        default: throw new Error(`Invalid type ${type}`)
    }
}

/**
 * @param {OutputStream} out
 * @param {Config|null} config
 */
function writeConfig(out, config) {
    if (config === null) {
        out.writeByte(0)
        return
    }
    switch (config.type) {
        case 'int': {
            out.writeByte(1)
            out.writeInt(config.value)
            return
        }
        case 'long': {
            out.writeByte(2)
            out.writeLong(config.value)
            return
        }
        case 'float': {
            out.writeByte(3)
            out.writeFloat(config.value)
            return
        }
        case 'map': {
            out.writeByte(0)
            console.warn('type 5 not implemented')
            return
        }
        case 'int[]': {
            out.writeByte(6)
            out.writeShort(config.value.length)
            config.value.forEach(i => out.writeInt(i))
            return
        }
        case 'pos': {
            out.writeByte(7)
            out.writeInt(config.value.x)
            out.writeInt(config.value.y)
            return
        }
        case 'pos[]': {
            out.writeByte(8)
            out.writeByte(config.value.length)
            config.value.forEach(pos => out.writeInt(pos.pack()))
            return
        }
        case 'node': {
            out.writeByte(0)
            console.warn('type 9 not implemented')
            return
        }
        case 'boolean': {
            out.writeByte(10)
            out.writeBool(config.value)
            return
        }
        case 'double': {
            out.writeByte(11)
            out.writeDouble(config.value)
            return
        }
        case 'building': {
            out.writeByte(12)
            out.writeInt(config.value.pack())
            return
        }
        case 'lAccess': {
            out.writeByte(0)
            console.warn('type 13 not implemented')
            return
        }
        case 'byte[]': {
            out.writeByte(14)
            out.writeInt(config.value.length)
            config.value.forEach(byte => out.writeByte(byte))
            return
        }
        case 'unitCommand': {
            out.writeByte(0)
            console.warn('type 15 not implemented')
            return
        }

        default: throw new Error(`Invalid type ${config.type}`)
    }
}

