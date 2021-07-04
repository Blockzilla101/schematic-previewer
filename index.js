import { Schematic } from './schematic.js'
import fs from 'fs'

const data = fs.readFileSync(process.argv[2])
// console.log(Schematic.read(data).toString())

// const expensiveWall = 'bXNjaAF4nGNgZGBkZGDJS8xNZRBxrShIzcssS1Vwzi8oSC1SCE/MyWFi4EnOL0rVzStNzkktLWbgTgbL6ZYD5RgYGPgYaAcYIRQAYPISnQ=='

let schematic = Schematic.read(data)
console.log(schematic.toString())
const rewritten = schematic.write()

// console.assert(expensiveWall === rewritten.toString('base64'))

// console.log(rewritten.toString('base64'))
// console.log(expensiveWall)

schematic = Schematic.read(rewritten)
console.log(schematic.toString())