import { Schematic } from './schematic.js'
import { Tile } from './util/tile.js'
import { Pos } from './util/pos.js'
import fs from 'fs'

function buildTiles(blocks, layout) {
    let tiles = [], start = [], end = []
    for(let x = 0; x < layout.length; x++) {
        for(let y = 0; y < layout[x].length; y++) {
            let temp = layout[x][y]
            if (typeof temp === 'number') {
                tiles.push(new Tile(blocks[temp], new Pos(x, y)))
            } else {
                if (temp.loc === 'start') {
                    start.push(new Tile(blocks[temp.index], new Pos(x, y)))
                } else {
                    end.push(new Tile(blocks[temp.index], new Pos(x, y)))
                }
            }
        }
    }
    return [...start, ...tiles, ...end]
}

let schem = new Schematic()
schem.width = 5
schem.height = 5

let blocks = ['phase-wall', 'surge-wall', 'core-nucleus', 'duo', 'item-source']
let tiles = [
    [1, 1, 1, 1, 1],
    [1, 4, 3, 4, 1],
    [1, 3, { index: 2, loc: 'start' }, 3, 1],
    [1, 4, 3, 4, 1],
    [1, 1, 1, 1, 1]
]

schem.tiles = buildTiles(blocks, tiles)

// console.log(schem.toString())
fs.writeFileSync('temp.msch', schem.write())
console.log(schem.toString())
// console.log(Schematic.read(fs.readFileSync(process.argv[2])).toString())
