Generates a preview for a mindustry schematic, along with printing other stuff about the schematic.

#### Usage
- `previewer.jar [options] schematic.msch`
- `previewer.jar --bulk path/to/file -d path/to/file`
##### Options
| Option | Description |
| ------ | ----------- |
| `-p <true\|false>` | Whether to generate a preivew instead of just outputing information about the schematic. |
| `-o <path/to/file>` | Where to put the generated preview. Cannot be used when `-p` option is set to `false` or when using `--bulk`. |
| `-offset <num>` | Number of pixels between edge of preview and schematic. Doesnt really gets used with `-art`. |
| `-d <path/to/file>` | Where to output information about the schematic. If omitted data is outputted to `stdout` prefixed by `>` otherwise data is outputed to the file provided in JSON format. Required by `--bulk`. |
| `-bg <true\|false>` | Whether to create the prevew with a background or not. Required with using `-art`. |
| `-bc <#hexcolor>` | Hex color for border in background. |
| `-art <true\|false>` | Whether to render sorter/pixel art for schematics. Requires `-bg`. Its position cant be changed and is always it top left if the schematic is pixel art. |
| `-bulk <path/to/file>` | Preview schematics in bulk. |
##### Bulk Mode
Format for bulk mode is same as arguments to the jar. eg `previewer.jar [options] schematic.msch` -> `[options] schematic.msch`. `-d` option is required. `-o` option cannot be used in bulk mode. Preview image (if preview is enabled) is outputted to `schem-preview-<time>.png` and `previewPath` is set to the image in the data file.

#### Compiling
Requires java 14
- `./gradlew dist` to compile, built jar should be at `build/libs/previewer.jar`

----
Previewing code ~~stolen~~ borrowed from [CoreBot](https://github.com/Anuken/CoreBot)
