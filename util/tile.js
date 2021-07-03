export class Tile {
    /** @type {Pos} */
    pos
    /** @type {string} */
    block
    /** @type {Object|null} */
    config = null
    /** @type {number} */
    rot

    /**
     * @param {string} block
     * @param {Pos} pos
     * @param {number} [rot=0]
     * @param {Object} [config=null]
     */
    constructor(block, pos, rot = 0, config = null) {
        this.block = block
        this.pos = pos
        this.rot = 0
        this.config = config
    }
}
