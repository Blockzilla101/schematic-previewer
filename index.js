import { Schematic } from './schematic.js'
import { Pos } from './util/pos.js'
import fs from 'fs'

const data = fs.readFileSync(process.argv[2])
console.log(Schematic.read(data).toString())
