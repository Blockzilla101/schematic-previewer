const fs = require('fs');
const { Schematic } = require('./schematic');

const data = fs.readFileSync('./test.msch');
console.log(Schematic.read(data).toString());
