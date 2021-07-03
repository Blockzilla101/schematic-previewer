import zip from 'zlib'
import { InputStream } from './util/stream.js'
import { Tile } from './util/tile.js'
import { Pos } from './util/pos.js'

export class Schematic {
    width
    height

    name
    description

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

        let map = new Map()
        let len = stream.readByte()
        for (let i = 0; i < len; i++) {
            map.set(stream.readStr(), stream.readStr())
        }

        schematic.name = map.get('name') ?? 'unknown'
        schematic.description = map.get('description') ?? null

        let labels = map.get('labels')

        let blocks = []
        len = stream.readByte()
        for (let i = 0; i < len; i++) {
            blocks.push(stream.readStr())
        }

        len = stream.readInt()
        for (let i = 0; i < len; i++) {
            let block = blocks[stream.readByte()]
            let pos = Pos.unpack(stream.readInt())
            let config = ver === 0 ? readConfig0(block, stream.readInt(), pos) : readConfig1(stream)
            let rot = stream.readByte()
            if (block !== 'air') schematic.tiles.push(new Tile(block, pos, rot, config))
        }

        return schematic
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
        case 1: return stream.readInt()
        case 2: return stream.readLong()
        case 3: return stream.readFloat()
        case 4: return stream.readStr()
        case 5: {
            stream.readByte()
            stream.readShort()
            return '[ type 5 is not done ]'
        }
        case 6: return stream.readArr(stream.readShort(), () => stream.readInt())
        case 7: return new Pos(stream.readInt(), stream.readInt())
        case 8: return stream.readArr(stream.readByte(), () => new Pos(stream.readShort(), stream.readShort()))
        case 9: {
            stream.readByte()
            stream.readShort()
            return '[ type 9 is not done ]'
        }
        case 10: return stream.readBool()
        case 11: return stream.readDouble()
        case 12: {
            stream.readInt()
            return '[ type 12 is not done ]'
        }
        case 13: {
            stream.readShort()
            return '[ type 13 is not done ]'
        }
        case 14: return stream.readArr(stream.readShort(), () => stream.readByte())
        case 15: {
            stream.readByte()
            return '[ type 15 is not done ]'
        }
        default: throw new Error(`Invalid type ${type}`)
    }
}

