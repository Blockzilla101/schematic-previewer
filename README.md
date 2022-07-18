Generates a preview for a mindustry schematic, along with printing other stuff about the schematic.

#### Usage
- `previewer.jar path/to/options.json`<br>
`options.json` is an array of schematic options which are:
```json
{
  "schematicPath": "path to .msch or base64",
  "previewPath": "path to preview.png" 
}
```
Remove `previewPath` if you dont want a preview.

- Schematic data is printed to stdout in json. It is an array of:
```json
{
  "name": "name of schematic",
  "description": "description",
  "labels": [],
  "previewPath": "path to preview.png, this property is missing if no preview path was provided in options",
  "quality": 4,
  "requirements": {},
  "powerProduced": 0,
  "powerConsumed": 0,
  "powerStored": 0,
  "height": 0,
  "width": 0,
  "blockCount": "number of blocks in the schematic",
  "schematic": "path or base64 of schematic",    
}
```
`requirements` is a dictionary of mindustry item and the amount required.


#### Compiling
Requires java 16
- `./gradlew dist` to compile, built jar should be at `build/libs/previewer.jar`

----
Previewing code ~~stolen~~ borrowed from [CoreBot](https://github.com/Anuken/CoreBot)
